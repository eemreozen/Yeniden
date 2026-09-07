package com.yeniden.gamification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Gamification Service Uygulaması Başlangıç Sınıfı (Port 8085).
 */
@SpringBootApplication
@EnableScheduling
public class GamificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamificationApplication.class, args);
    }
}
