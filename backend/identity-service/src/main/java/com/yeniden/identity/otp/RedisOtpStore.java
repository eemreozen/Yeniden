package com.yeniden.identity.otp;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;

final class RedisOtpStore {
    private static final DefaultRedisScript<Long> RESERVE = script("otp-reserve", Long.class);
    private static final DefaultRedisScript<List> ACTIVATE = script("otp-activate", List.class);
    private static final DefaultRedisScript<Long> CONSUME = script("otp-consume", Long.class);
    private static final DefaultRedisScript<Long> INVALIDATE = script("otp-invalidate", Long.class);

    private final StringRedisTemplate redis;
    private final String prefix;
    private final long ttlMillis;
    private final long cooldownMillis;
    private final int maxAttempts;
    private final int phoneLimit;
    private final int ipLimit;

    RedisOtpStore(StringRedisTemplate redis, OtpProperties properties) {
        properties.validate();
        this.redis = redis;
        // All rate keys for this namespace share a cluster slot, including the cross-phone IP limit.
        prefix = "{" + properties.getNamespace() + "}:";
        ttlMillis = properties.getTtl().toMillis();
        cooldownMillis = properties.getCooldown().toMillis();
        maxAttempts = properties.getMaxAttempts();
        phoneLimit = properties.getPhoneLimit();
        ipLimit = properties.getIpLimit();
    }

    long reserve(String phoneKey, String ipKey, UUID id, String proof) {
        return execute(RESERVE, List.of(challengeKey(phoneKey), cooldownKey(phoneKey),
                        prefix + "phone-rate:" + phoneKey, prefix + "ip-rate:" + ipKey),
                id.toString(), proof, Long.toString(ttlMillis), Long.toString(cooldownMillis),
                Integer.toString(phoneLimit), Integer.toString(ipLimit));
    }

    Activation activate(String phoneKey, UUID id) {
        List<?> result = execute(ACTIVATE, List.of(challengeKey(phoneKey), cooldownKey(phoneKey)), id.toString());
        if (result.size() != 2 || !(result.get(0) instanceof Long ttl) || !(result.get(1) instanceof Long retry)) {
            throw OtpErrors.unavailable();
        }
        return new Activation(ttl, retry);
    }

    boolean consume(String phoneKey, UUID id, String proof) {
        return execute(CONSUME, List.of(challengeKey(phoneKey)), id.toString(), proof,
                Integer.toString(maxAttempts)) == 1;
    }

    void invalidate(String phoneKey, UUID id) {
        execute(INVALIDATE, List.of(challengeKey(phoneKey)), id.toString());
    }

    String challengeKey(String phoneKey) { return prefix + "challenge:" + phoneKey; }
    String cooldownKey(String phoneKey) { return prefix + "cooldown:" + phoneKey; }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            T result = redis.execute(script, keys, (Object[]) args);
            if (result == null) throw OtpErrors.unavailable();
            return result;
        } catch (RuntimeException e) {
            throw OtpErrors.unavailable();
        }
    }

    private static <T> DefaultRedisScript<T> script(String name, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/" + name + ".lua"));
        script.setResultType(resultType);
        return script;
    }

    record Activation(long ttlMillis, long retryMillis) { }
}
