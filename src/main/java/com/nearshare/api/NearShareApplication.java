package com.nearshare.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.nearshare.api.repository",
        "com.nearshare.api.partner.repository",
        "com.nearshare.api.recommendation.repository"
})
public class NearShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(NearShareApplication.class, args);
    }
}
