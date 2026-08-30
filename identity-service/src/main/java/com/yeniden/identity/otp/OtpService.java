package com.yeniden.identity.otp;

import com.yeniden.identity.sms.SmsSender;

import java.util.UUID;

/** A successful verify consumes the proof before the caller starts its auth transaction. */
public class OtpService {
    private final RedisOtpStore store;
    private final SmsSender sender;
    private final OtpCrypto crypto;

    OtpService(RedisOtpStore store, SmsSender sender, OtpCrypto crypto) {
        this.store = store;
        this.sender = sender;
        this.crypto = crypto;
    }

    public OtpChallengeResponse request(String rawPhone, String ip) {
        String phone = PhoneNormalizer.normalize(rawPhone);
        // The HTTP boundary must supply the resolved, trusted client IP, never a raw forwarded header.
        if (ip == null || ip.isBlank() || ip.length() > 64) throw OtpErrors.invalid();
        UUID id = UUID.randomUUID();
        String code = crypto.newCode();
        String phoneKey = crypto.phoneKey(phone);
        long retryMillis = store.reserve(phoneKey, crypto.ipKey(ip), id, crypto.proof(phone, code, id));
        if (retryMillis > 0) throw new RateLimitedException(seconds(retryMillis));
        try {
            // Never retry an ambiguous network outcome: the provider might already have accepted it.
            sender.send(phone, code, id);
            RedisOtpStore.Activation activation = store.activate(phoneKey, id);
            if (activation.ttlMillis() <= 0) throw OtpErrors.unavailable();
            return new OtpChallengeResponse(id, seconds(activation.ttlMillis()), seconds(activation.retryMillis()));
        } catch (RuntimeException e) {
            try {
                store.invalidate(phoneKey, id);
            } catch (RuntimeException ignored) {
                // Fail closed. Pending challenges cannot verify; cleanup is bounded by the original TTL.
            }
            throw OtpErrors.unavailable();
        }
    }

    public String verify(String rawPhone, String code, UUID challengeId) {
        String phone = PhoneNormalizer.normalize(rawPhone);
        if (challengeId == null) throw OtpErrors.invalid();
        // Malformed codes still consume an attempt on the matching active challenge.
        String proof = code != null && code.matches("[0-9]{6}")
                ? crypto.proof(phone, code, challengeId) : "invalid";
        if (!store.consume(crypto.phoneKey(phone), challengeId, proof)) throw OtpErrors.invalid();
        return phone;
    }

    private static long seconds(long millis) { return millis <= 0 ? 0 : (millis + 999) / 1000; }
}
