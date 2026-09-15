package com.yeniden.identity.otp;

/** TR mobile syntax only; ownership is established separately by OTP verification. */
public final class PhoneNormalizer {
    private PhoneNormalizer() { }

    public static String normalize(String rawPhone) {
        if (rawPhone == null || rawPhone.length() > 64
                || !rawPhone.matches("[+0-9 ()-]+")) {
            throw OtpErrors.invalid();
        }
        String phone = rawPhone.replaceAll("[ ()-]", "");
        if (phone.matches("\\+905[0-9]{9}")) return phone;
        if (phone.matches("905[0-9]{9}")) return "+" + phone;
        if (phone.matches("05[0-9]{9}")) return "+9" + phone;
        if (phone.matches("5[0-9]{9}")) return "+90" + phone;
        throw OtpErrors.invalid();
    }
}
