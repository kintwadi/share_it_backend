package com.vicinity24.api.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Vicinity24CoreApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(Vicinity24CoreApiApplication.class, args);
    }
}
