package com.yeniden.identity.otp;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

final class OtpCrypto {
    private final SecretKeySpec key;
    private final String namespace;
    private final SecureRandom random = new SecureRandom();

    OtpCrypto(OtpProperties properties) {
        properties.validate();
        key = new SecretKeySpec(properties.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        namespace = properties.getNamespace();
    }

    String newCode() { return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000)); }
    String phoneKey(String phone) { return hmac("phone", phone); }
    String ipKey(String ip) { return hmac("ip", ip); }
    String proof(String phone, String code, UUID id) { return hmac("proof", phone, id.toString(), code); }

    private String hmac(String purpose, String... parts) {
        try {
            // Mac is mutable, so never share one between requests.
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(namespace.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            mac.update(purpose.getBytes(StandardCharsets.UTF_8));
            for (String part : parts) {
                mac.update((byte) 0);
                mac.update(part.getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(mac.doFinal());
        } catch (GeneralSecurityException e) {
            throw OtpErrors.unavailable();
        }
    }
}
