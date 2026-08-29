package com.yeniden.identity.controller;

import com.yeniden.common.result.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/identity/otp")
public class OtpController {

    private final StringRedisTemplate redisTemplate;

    public OtpController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Telefon numarasına OTP doğrulama kodu gönderir (Redis 3 dakika TTL ile saklar).
     */
    @PostMapping("/send")
    public ApiResponse<String> sendOtp(@RequestParam String phone) {
        String code = String.format("%06d", new Random().nextInt(900000) + 100000);
        String redisKey = "OTP:" + phone;
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofMinutes(3));
        return ApiResponse.success("OTP kodu başarıyla üretildi ve Redis'e kaydedildi (TTL: 3dk).", "Kod: " + code);
    }

    /**
     * Kullanıcının girdiği OTP kodunu Redis'ten doğrular.
     */
    @PostMapping("/verify")
    public ApiResponse<Boolean> verifyOtp(@RequestParam String phone, @RequestParam String code) {
        String redisKey = "OTP:" + phone;
        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            return ApiResponse.error("OTP kodunun süresi dolmuş veya hiç üretilmemiş!");
        }

        if (!storedCode.equals(code)) {
            return ApiResponse.error("Hatalı OTP kodu!");
        }

        // Başarılı doğrulamadan sonra OTP tek kullanımlıktır, Redis'ten silinir
        redisTemplate.delete(redisKey);
        return ApiResponse.success("OTP başarıyla doğrulandı!", true);
    }
}
