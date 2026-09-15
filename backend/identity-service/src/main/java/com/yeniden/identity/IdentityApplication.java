package com.yeniden.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Identity Service mikroservisinin ana başlangıç sınıfı.
 */
@SpringBootApplication(scanBasePackages = "com.yeniden")
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
