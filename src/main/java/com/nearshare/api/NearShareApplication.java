package com.nearshare.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.nearshare.api.repository")
public class NearShareApplication {
    public static void main(String[] args) {
        SpringApplication.run(NearShareApplication.class, args);
    }
}