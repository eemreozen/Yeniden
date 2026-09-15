package com.yeniden.identity.sms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.yeniden.common.exception.BaseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SmsGateSenderTest {
    // Fictional numbers only; every network request goes to this process's loopback stub.
    private static final String PHONE = "+12025550123";
    private static final String CODE = "012345";
    private static final UUID CHALLENGE = UUID.fromString("7264f8aa-7293-4d0f-84e3-763b06cbdf4d");
    private final List<SmsGateSender> senders = new ArrayList<>();
    private final AtomicInteger requests = new AtomicInteger();
    private final AtomicInteger redirectedRequests = new AtomicInteger();
    private final AtomicReference<Captured> captured = new AtomicReference<>();
    private final CountDownLatch releaseResponse = new CountDownLatch(1);
    private volatile int status = 202;
    private volatile boolean stallHeaders;
    private volatile boolean stallBody;
    private HttpServer server;
    private ExecutorService executor;

    @BeforeEach
    void startStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/message", this::respond);
        server.createContext("/leak", exchange -> {
            redirectedRequests.incrementAndGet();
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopStub() {
        releaseResponse.countDown();
        senders.forEach(SmsGateSender::close);
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void sendsOfficialLocalServerContractAndStableId() throws Exception {
        SmsGateSender sender = sender(properties());
        sender.send(PHONE, CODE, CHALLENGE);
        Captured first = captured.get();
        sender.send(PHONE, CODE, CHALLENGE);
        assertEquals(2, requests.get());
        assertEquals("POST", first.method());
        assertEquals("/message", first.path());
        assertEquals("application/json", first.contentType());
        assertEquals("application/json", first.accept());
        assertEquals("Basic " + Base64.getEncoder().encodeToString(
                "stub-user:stub-password".getBytes(StandardCharsets.UTF_8)), first.authorization());
        JsonNode body = new ObjectMapper().readTree(first.body());
        assertEquals(3, body.size());
        assertEquals(CHALLENGE.toString(), body.get("id").asText());
        assertEquals("Yeniden dogrulama kodunuz: " + CODE, body.at("/textMessage/text").asText());
        assertEquals(1, body.get("phoneNumbers").size());
        assertEquals(PHONE, body.at("/phoneNumbers/0").asText());
        assertEquals(body, new ObjectMapper().readTree(captured.get().body()));
    }

    @Test
    void disabledDefaultsRejectEvenWithMissingConfigurationAndMakeNoRequest() {
        assertUnavailable(() -> sender(new SmsGateProperties()).send(PHONE, CODE, CHALLENGE));
        SmsGateProperties disabled = properties();
        disabled.setEnabled(false);
        disabled.setBaseUrl("not a URI");
        assertUnavailable(() -> sender(disabled).send(PHONE, CODE, CHALLENGE));
        assertEquals(0, requests.get());
    }

    @Test
    void requiresExplicitHttpOptInBeforeAnyRequest() {
        SmsGateProperties properties = properties();
        properties.setAllowPrivateHttp(false);
        assertUnavailable(() -> sender(properties));
        assertEquals(0, requests.get());
    }

    @Test
    void emptyAllowlistAndUnlistedRecipientFailClosed() {
        SmsGateProperties properties = properties();
        properties.setAllowedRecipients(Set.of());
        assertUnavailable(() -> sender(properties).send(PHONE, CODE, CHALLENGE));
        assertUnavailable(() -> sender(properties()).send("+12025550124", CODE, CHALLENGE));
        assertEquals(0, requests.get());
    }

    @Test
    void snapshotsAllowlistRatherThanAcceptingLaterMutation() {
        Set<String> recipients = new HashSet<>(Set.of(PHONE));
        SmsGateProperties properties = properties();
        properties.setAllowedRecipients(recipients);
        SmsGateSender sender = sender(properties);
        recipients.add("+12025550124");
        assertUnavailable(() -> sender.send("+12025550124", CODE, CHALLENGE));
        assertEquals(0, requests.get());
    }

    @Test
    void rejectsInvalidInputsWithoutNetworkOrSensitiveExceptionDetails() {
        SmsGateSender sender = sender(properties());
        assertUnavailable(() -> sender.send(null, CODE, CHALLENGE));
        assertUnavailable(() -> sender.send("2025550123", CODE, CHALLENGE));
        assertUnavailable(() -> sender.send(PHONE, null, CHALLENGE));
        assertUnavailable(() -> sender.send(PHONE, "12345", CHALLENGE));
        assertUnavailable(() -> sender.send(PHONE, "1234567", CHALLENGE));
        assertUnavailable(() -> sender.send(PHONE, "12\n345", CHALLENGE));
        assertUnavailable(() -> sender.send(PHONE, CODE, null));
        assertEquals(0, requests.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 302, 303, 307, 308, 400, 401, 403, 404, 409, 422, 429, 500, 503})
    void rejectionAndRedirectNeverSucceedLeakOrRetry(int responseStatus) {
        status = responseStatus;
        assertUnavailable(() -> sender(properties()).send(PHONE, CODE, CHALLENGE));
        assertEquals(1, requests.get());
        assertEquals(0, redirectedRequests.get());
    }

    @Test
    void delayedHeadersAreBoundedAndNotRetried() {
        stallHeaders = true;
        assertBoundedFailure();
    }

    @Test
    void stalledBodyIsBoundedEvenAfterSuccessfulHeaders() {
        stallBody = true;
        assertBoundedFailure();
    }

    @Test
    void connectionFailureIsSanitized() {
        SmsGateSender sender = sender(properties());
        server.stop(0);
        assertUnavailable(() -> sender.send(PHONE, CODE, CHALLENGE));
    }

    @Test
    void interruptedCallerRemainsInterrupted() {
        SmsGateSender sender = sender(properties());
        Thread.currentThread().interrupt();
        try {
            assertUnavailable(() -> sender.send(PHONE, CODE, CHALLENGE));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void invalidCredentialsTimeoutsAndAllowlistFailClosed() {
        SmsGateProperties properties = properties();
        properties.setUsername("user:ambiguous");
        assertUnavailable(() -> sender(properties));
        properties.setUsername("stub-user");
        properties.setPassword("secret\r\nheader");
        assertUnavailable(() -> sender(properties));
        properties.setPassword(" ");
        assertUnavailable(() -> sender(properties));
        properties.setPassword(null);
        assertUnavailable(() -> sender(properties));
        properties.setPassword("stub-password");
        properties.setConnectTimeout(Duration.ZERO);
        assertUnavailable(() -> sender(properties));
        properties.setConnectTimeout(Duration.ofSeconds(3));
        properties.setReadTimeout(Duration.ofSeconds(61));
        assertUnavailable(() -> sender(properties));
        properties.setReadTimeout(null);
        assertUnavailable(() -> sender(properties));
        properties.setReadTimeout(Duration.ofSeconds(5));
        properties.setAllowedRecipients(Set.of("*"));
        assertUnavailable(() -> sender(properties));
        assertEquals(0, requests.get());
    }

    private void assertBoundedFailure() {
        SmsGateProperties properties = properties();
        properties.setReadTimeout(Duration.ofMillis(500));
        SmsGateSender sender = sender(properties);
        assertTimeout(Duration.ofSeconds(3), () ->
                assertUnavailable(() -> sender.send(PHONE, CODE, CHALLENGE)));
        assertEquals(1, requests.get());
    }

    private SmsGateProperties properties() {
        SmsGateProperties properties = new SmsGateProperties();
        properties.setEnabled(true);
        properties.setAllowPrivateHttp(true);
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setUsername("stub-user");
        properties.setPassword("stub-password");
        properties.setAllowedRecipients(Set.of(PHONE));
        return properties;
    }

    private SmsGateSender sender(SmsGateProperties properties) {
        SmsGateSender sender = new SmsGateSender(properties);
        senders.add(sender);
        return sender;
    }

    static void assertUnavailable(org.junit.jupiter.api.function.Executable action) {
        BaseException exception = assertThrows(BaseException.class, action);
        assertEquals(503, exception.getHttpStatus());
        assertEquals("service_unavailable", exception.getErrorCode());
        assertEquals("SMS service is unavailable.", exception.getMessage());
        assertNull(exception.getCause());
        assertEquals(0, exception.getSuppressed().length);
    }

    private void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            captured.set(new Captured(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("Accept"), exchange.getRequestBody().readAllBytes()));
            requests.incrementAndGet();
            if (stallHeaders) {
                awaitRelease();
            }
            exchange.getResponseHeaders().set("Location", "http://localhost:" + server.getAddress().getPort() + "/leak");
            byte[] response = ("raw-provider-response " + PHONE + " " + CODE + " stub-password")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, response.length);
            if (stallBody) {
                exchange.getResponseBody().write(response, 0, 1);
                exchange.getResponseBody().flush();
                awaitRelease();
                exchange.getResponseBody().write(response, 1, response.length - 1);
            } else {
                exchange.getResponseBody().write(response);
            }
        }
    }

    private void awaitRelease() {
        try {
            releaseResponse.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record Captured(String method, String path, String authorization,
                            String contentType, String accept, byte[] body) {
        @Override
        public String toString() { return "Captured[redacted]"; }
    }
}
