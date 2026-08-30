package com.yeniden.identity.otp;

import com.yeniden.common.exception.BaseException;
import com.yeniden.identity.sms.SmsSender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/** Real Redis scripts and concurrency. Unit-only runs can use -DexcludedGroups=integration. */
@Tag("integration")
@Testcontainers
class OtpRedisIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private OtpProperties properties;
    private OtpCrypto crypto;
    private RedisOtpStore store;
    private OtpService service;
    private final Map<UUID, String> messages = new ConcurrentHashMap<>();
    private final AtomicInteger sends = new AtomicInteger();
    private static final String PHONE = "+905321234567";
    private static final String IP = "192.0.2.1";

    @BeforeAll
    static void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        redis.afterPropertiesSet();
    }

    @AfterAll
    static void disconnect() {
        if (connectionFactory != null) connectionFactory.destroy();
    }

    @BeforeEach
    void isolateTestKeys() {
        // No FLUSHDB: each test owns its namespace, and the disposable container owns the data.
        properties = OtpPropertiesTest.validProperties();
        properties.setNamespace("test:otp:" + UUID.randomUUID());
        crypto = new OtpCrypto(properties);
        store = new RedisOtpStore(redis, properties);
        service = serviceUsing((phone, code, id) -> {
            messages.put(id, code);
            sends.incrementAndGet();
        });
    }

    @Test
    void storesOnlyHmacProofAndPseudonymousKeysAndConsumesExactlyOnce() {
        OtpChallengeResponse response = service.request("05321234567", IP);
        Map<Object, Object> state = redis.opsForHash().entries(challengeKey());
        assertThat(state).containsEntry("status", "active").containsEntry("attempts", "0")
                .containsEntry("id", response.challengeId().toString())
                .containsEntry("hash", crypto.proof(PHONE, messages.get(response.challengeId()), response.challengeId()));
        assertThat(state.values()).doesNotContain(PHONE, IP, messages.get(response.challengeId()));
        var keys = redis.keys("{" + properties.getNamespace() + "}:*");
        assertThat(keys).hasSize(4).allSatisfy(k -> assertThat(k).doesNotContain(PHONE, IP, "05321234567"));
        assertThat(response.expiresIn()).isBetween(1L, 180L);
        assertThat(response.retryAfterSeconds()).isBetween(1L, 60L);
        assertThat(service.verify(PHONE, messages.get(response.challengeId()), response.challengeId())).isEqualTo(PHONE);
        assertInvalid(() -> service.verify(PHONE, messages.get(response.challengeId()), response.challengeId()));
        assertThat(redis.hasKey(challengeKey())).isFalse();
        assertRateLimited(() -> service.request(PHONE, IP), 1, 60);
    }

    @Test
    void concurrentDuplicateVerificationHasExactlyOneWinner() throws Exception {
        OtpChallengeResponse response = service.request(PHONE, IP);
        List<Boolean> outcomes = race(24, () -> {
            try {
                service.verify(PHONE, messages.get(response.challengeId()), response.challengeId());
                return true;
            } catch (BaseException e) {
                assertThat(e.getErrorCode()).isEqualTo("invalid_otp");
                return false;
            }
        });
        assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
    }

    @Test
    void concurrentRequestsForOnePhoneSendOnlyOnce() throws Exception {
        List<Boolean> outcomes = race(16, () -> {
            try {
                service.request(PHONE, IP);
                return true;
            } catch (RateLimitedException e) {
                assertThat(e.getRetryAfterSeconds()).isBetween(1L, 60L);
                return false;
            }
        });
        assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(1);
        assertThat(sends.get()).isEqualTo(1);
    }

    @Test
    void fifthWrongAttemptExhaustsChallengeAndRetriesNeverExtendTtl() {
        OtpChallengeResponse response = service.request(PHONE, IP);
        redis.expire(challengeKey(), Duration.ofSeconds(40));
        long ttlBefore = redis.getExpire(challengeKey(), TimeUnit.MILLISECONDS);
        String wrong = wrongCode(response.challengeId());
        for (int i = 0; i < 4; i++) assertInvalid(() -> service.verify(PHONE, wrong, response.challengeId()));
        assertThat(redis.opsForHash().get(challengeKey(), "attempts")).isEqualTo("4");
        assertThat(redis.getExpire(challengeKey(), TimeUnit.MILLISECONDS)).isBetween(1L, ttlBefore);
        assertInvalid(() -> service.verify(PHONE, wrong, response.challengeId()));
        assertThat(redis.hasKey(challengeKey())).isFalse();
        assertInvalid(() -> service.verify(PHONE, messages.get(response.challengeId()), response.challengeId()));
    }

    @Test
    void correctFifthAttemptIsAllowed() {
        OtpChallengeResponse response = service.request(PHONE, IP);
        for (int i = 0; i < 4; i++) assertInvalid(() -> service.verify(PHONE, wrongCode(response.challengeId()), response.challengeId()));
        assertThat(service.verify(PHONE, messages.get(response.challengeId()), response.challengeId())).isEqualTo(PHONE);
    }

    @Test
    void concurrentWrongAttemptsCannotExceedLimit() throws Exception {
        OtpChallengeResponse response = service.request(PHONE, IP);
        race(16, () -> {
            assertInvalid(() -> service.verify(PHONE, wrongCode(response.challengeId()), response.challengeId()));
            return true;
        });
        assertThat(redis.hasKey(challengeKey())).isFalse();
        assertInvalid(() -> service.verify(PHONE, messages.get(response.challengeId()), response.challengeId()));
    }

    @Test
    void expiredProofCannotBeVerifiedOrReactivated() {
        OtpChallengeResponse response = service.request(PHONE, IP);
        redis.expire(challengeKey(), Duration.ofMillis(1));
        await().atMost(Duration.ofSeconds(3)).until(() -> !Boolean.TRUE.equals(redis.hasKey(challengeKey())));
        assertInvalid(() -> service.verify(PHONE, messages.get(response.challengeId()), response.challengeId()));
        assertThat(store.activate(crypto.phoneKey(PHONE), response.challengeId()).ttlMillis()).isZero();
    }

    @Test
    void replacementInvalidatesOldChallengeAndStaleOperationsDoNotTouchNewOne() {
        OtpChallengeResponse old = service.request(PHONE, IP);
        endCooldown();
        OtpChallengeResponse current = service.request(PHONE, IP);
        assertInvalid(() -> service.verify(PHONE, messages.get(old.challengeId()), old.challengeId()));
        store.invalidate(crypto.phoneKey(PHONE), old.challengeId());
        assertThat(store.activate(crypto.phoneKey(PHONE), old.challengeId()).ttlMillis()).isZero();
        assertThat(redis.opsForHash().get(challengeKey(), "attempts")).isEqualTo("0");
        assertThat(service.verify(PHONE, messages.get(current.challengeId()), current.challengeId())).isEqualTo(PHONE);
    }

    @Test
    void pendingProofCannotVerifyAndActivationPreservesOriginalTtl() {
        OtpService observing = serviceUsing((phone, code, id) -> {
            assertThat(redis.opsForHash().get(challengeKey(), "status")).isEqualTo("pending");
            assertInvalid(() -> service.verify(phone, code, id));
            assertThat(redis.opsForHash().get(challengeKey(), "attempts")).isEqualTo("0");
            redis.expire(challengeKey(), Duration.ofSeconds(40));
            messages.put(id, code);
        });
        OtpChallengeResponse response = observing.request(PHONE, IP);
        assertThat(response.expiresIn()).isBetween(1L, 40L);
        assertThat(service.verify(PHONE, messages.get(response.challengeId()), response.challengeId())).isEqualTo(PHONE);
    }

    @Test
    void failedSendInvalidatesProofButRetainsCooldownAndRateCounters() {
        OtpService failing = serviceUsing((phone, code, id) -> {
            messages.put(id, code);
            sends.incrementAndGet();
            throw new IllegalStateException("ambiguous transport result");
        });
        assertUnavailable(() -> failing.request(PHONE, IP));
        assertThat(redis.hasKey(challengeKey())).isFalse();
        assertThat(sends.get()).isEqualTo(1);
        assertRateLimited(() -> failing.request(PHONE, IP), 1, 60);
        for (int i = 0; i < 2; i++) {
            endCooldown();
            assertUnavailable(() -> failing.request(PHONE, IP));
        }
        endCooldown();
        assertRateLimited(() -> failing.request(PHONE, IP), 3500, 3600);
        assertThat(sends.get()).isEqualTo(3);
    }

    @Test
    void delayedSendFailureNeverInvalidatesLaterChallenge() {
        UUID replacement = UUID.randomUUID();
        OtpService delayedFailure = serviceUsing((phone, code, id) -> {
            endCooldown();
            assertThat(store.reserve(crypto.phoneKey(PHONE), crypto.ipKey(IP), replacement,
                    crypto.proof(PHONE, "012345", replacement))).isZero();
            store.activate(crypto.phoneKey(PHONE), replacement);
            throw new IllegalStateException("old request failed after replacement");
        });
        assertUnavailable(() -> delayedFailure.request(PHONE, IP));
        assertThat(service.verify(PHONE, "012345", replacement)).isEqualTo(PHONE);
    }

    @Test
    void delayedAcceptanceCannotActivateOrDeleteTheReplacement() {
        UUID replacement = UUID.randomUUID();
        OtpService delayedAcceptance = serviceUsing((phone, code, id) -> {
            endCooldown();
            store.reserve(crypto.phoneKey(PHONE), crypto.ipKey(IP), replacement,
                    crypto.proof(PHONE, "012345", replacement));
        });
        assertUnavailable(() -> delayedAcceptance.request(PHONE, IP));
        assertThat(redis.opsForHash().get(challengeKey(), "id")).isEqualTo(replacement.toString());
        assertThat(redis.opsForHash().get(challengeKey(), "status")).isEqualTo("pending");
        assertInvalid(() -> service.verify(PHONE, "012345", replacement));
        store.activate(crypto.phoneKey(PHONE), replacement);
        assertThat(service.verify(PHONE, "012345", replacement)).isEqualTo(PHONE);
    }

    @Test
    void phoneHourlyLimitSurvivesSuccessfulConsumptionAndUsesNormalizedIdentity() {
        String[] forms = {"+905321234567", "05321234567", "5321234567"};
        for (String form : forms) {
            OtpChallengeResponse response = service.request(form, IP);
            service.verify(PHONE, messages.get(response.challengeId()), response.challengeId());
            endCooldown();
        }
        assertRateLimited(() -> service.request("905321234567", "192.0.2.2"), 3500, 3600);
        assertThat(sends.get()).isEqualTo(3);
    }

    @Test
    void ipHourlyLimitIsAtomicAcrossDifferentPhones() throws Exception {
        AtomicInteger phoneSuffix = new AtomicInteger();
        List<Boolean> outcomes = race(20, () -> {
            String phone = "+90532" + String.format(java.util.Locale.ROOT, "%07d", phoneSuffix.incrementAndGet());
            try {
                service.request(phone, IP);
                return true;
            } catch (RateLimitedException e) {
                assertThat(e.getRetryAfterSeconds()).isBetween(3500L, 3600L);
                return false;
            }
        });
        assertThat(outcomes.stream().filter(Boolean::booleanValue).count()).isEqualTo(10);
        assertThat(sends.get()).isEqualTo(10);
    }

    @Test
    void rollingWindowDropsOldRequestsWithoutResettingRecentEntries() {
        for (int i = 0; i < 3; i++) {
            service.request(PHONE, IP);
            endCooldown();
        }
        assertRateLimited(() -> service.request(PHONE, IP), 3500, 3600);
        String phoneRateKey = "{" + properties.getNamespace() + "}:phone-rate:" + crypto.phoneKey(PHONE);
        String oldest = redis.opsForZSet().range(phoneRateKey, 0, 0).iterator().next();
        // Redis TIME is the authority; score zero is outside the rolling window on any current server.
        redis.opsForZSet().add(phoneRateKey, oldest, 0);
        service.request(PHONE, IP);
        assertThat(redis.opsForZSet().zCard(phoneRateKey)).isEqualTo(3);
        endCooldown();
        assertRateLimited(() -> service.request(PHONE, IP), 3500, 3600);
    }

    @Test
    void separateNamespaceDoesNotShareProofsOrRateLimits() {
        OtpChallengeResponse original = service.request(PHONE, IP);
        OtpProperties isolated = OtpPropertiesTest.validProperties();
        isolated.setNamespace("test:otp:" + UUID.randomUUID());
        OtpService other = new OtpService(new RedisOtpStore(redis, isolated),
                (phone, code, id) -> messages.put(id, code), new OtpCrypto(isolated));
        assertInvalid(() -> other.verify(PHONE, messages.get(original.challengeId()), original.challengeId()));
        OtpChallengeResponse response = other.request(PHONE, IP);
        assertThat(other.verify(PHONE, messages.get(response.challengeId()), response.challengeId())).isEqualTo(PHONE);
    }

    private OtpService serviceUsing(SmsSender sender) { return new OtpService(store, sender, crypto); }
    private String challengeKey() { return store.challengeKey(crypto.phoneKey(PHONE)); }
    private void endCooldown() { redis.delete(store.cooldownKey(crypto.phoneKey(PHONE))); }
    private String wrongCode(UUID id) { return "000000".equals(messages.get(id)) ? "999999" : "000000"; }

    private static void assertInvalid(Runnable action) { assertError(action, 400, "invalid_otp"); }
    private static void assertUnavailable(Runnable action) { assertError(action, 503, "service_unavailable"); }
    private static void assertError(Runnable action, int status, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BaseException.class, e -> {
            assertThat(e.getHttpStatus()).isEqualTo(status);
            assertThat(e.getErrorCode()).isEqualTo(code);
        });
    }

    private static void assertRateLimited(Runnable action, long minimum, long maximum) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(RateLimitedException.class,
                e -> assertThat(e.getRetryAfterSeconds()).isBetween(minimum, maximum));
    }

    private static <T> List<T> race(int count, Callable<T> action) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) futures.add(executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("race did not start");
                return action.call();
            }));
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(15, TimeUnit.SECONDS));
            return results;
        }
    }
}
