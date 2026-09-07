package com.yeniden.ecocoin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EcoCoin Service Uygulaması Başlangıç Sınıfı (Port 8084).
 */
@SpringBootApplication
@EnableScheduling
public class EcoCoinApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoCoinApplication.class, args);
    }
}
