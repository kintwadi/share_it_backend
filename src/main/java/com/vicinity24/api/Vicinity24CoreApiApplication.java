package com.vicinity24.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.vicinity24.api.repository",
        "com.vicinity24.api.config",
        "com.vicinity24.api.partner.repository",
        "com.vicinity24.api.recommendation.repository"
})
public class Vicinity24CoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(Vicinity24CoreApiApplication.class, args);
    }
}
