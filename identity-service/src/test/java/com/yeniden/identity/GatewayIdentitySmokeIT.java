package com.yeniden.identity;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Explicit smoke after packaging api-gateway; not a default Surefire *Test.
 * Inherits the direct identity regression cases as well as this real gateway HTTP flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIdentitySmokeIT extends IdentityFlowIntegrationTest {
    @LocalServerPort int identityPort;

    @Test void gatewayRelaysFullAuthFlowAndPrivateProfileSecurity() throws Exception {
        Path jar = Path.of("../api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar").toAbsolutePath();
        assertThat(jar).as("Package api-gateway before the explicit smoke test").isRegularFile();
        int gatewayPort;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            gatewayPort = socket.getLocalPort();
        }
        Process gateway = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-jar", jar.toString(), "--server.address=127.0.0.1", "--server.port=" + gatewayPort,
                "--IDENTITY_HOST=127.0.0.1", "--IDENTITY_PORT=" + identityPort)
                .redirectErrorStream(true)
                .redirectOutput(Path.of("target/gateway-smoke.log").toFile()).start();
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()) {
            String origin = "http://127.0.0.1:" + gatewayPort;
            await().atMost(Duration.ofSeconds(40)).pollInterval(Duration.ofMillis(200)).until(() -> {
                if (!gateway.isAlive()) throw new IllegalStateException("Gateway exited; inspect target/gateway-smoke.log");
                try {
                    return client.send(HttpRequest.newBuilder(URI.create(origin + "/api/v1/identity/health"))
                            .timeout(Duration.ofSeconds(2)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
                } catch (java.io.IOException ex) { return false; }
            });
            JsonNode c = send(client, origin, "/api/v1/auth/otp/request", Map.of("phone", phone), 202);
            UUID challenge = UUID.fromString(c.get("challengeId").asText());
            JsonNode tokens = send(client, origin, "/api/v1/auth/otp/verify",
                    Map.of("phone", phone, "code", delivered.get(challenge), "challengeId", challenge), 200);
            var me = client.send(HttpRequest.newBuilder(URI.create(origin + "/api/v1/users/me"))
                    .header("Authorization", "Bearer " + tokens.get("accessToken").asText()).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(me.statusCode()).isEqualTo(200);
            assertThat(json.readTree(me.body()).path("data").path("phone").asText()).isEqualTo(phone);
            String id = json.readTree(me.body()).path("data").path("id").asText();
            var publicResponse = client.send(HttpRequest.newBuilder(URI.create(origin + "/api/v1/users/" + id)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(publicResponse.statusCode()).isEqualTo(200);
            assertThat(publicResponse.body()).doesNotContain(phone, "\"email\"", "\"neighborhoodId\"");
            JsonNode rotated = send(client, origin, "/api/v1/auth/refresh",
                    Map.of("refreshToken", tokens.get("refreshToken").asText()), 200);
            send(client, origin, "/api/v1/auth/logout", Map.of("refreshToken", rotated.get("refreshToken").asText()), 204);
            var denied = client.send(HttpRequest.newBuilder(URI.create(origin + "/api/v1/users/me"))
                    .header("Authorization", "Bearer " + rotated.get("accessToken").asText()).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(denied.statusCode()).isEqualTo(401);
        } finally {
            gateway.destroy();
            if (!gateway.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) gateway.destroyForcibly();
        }
    }

    private JsonNode send(HttpClient client, String origin, String path, Object body, int status) throws Exception {
        var response = client.send(HttpRequest.newBuilder(URI.create(origin + path))
                        .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(path).isEqualTo(status);
        return response.body().isBlank() ? json.nullNode() : json.readTree(response.body()).path("data");
    }
}
