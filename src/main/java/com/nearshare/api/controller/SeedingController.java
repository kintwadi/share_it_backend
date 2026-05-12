package com.nearshare.api.controller;

import com.nearshare.api.service.MockDataSeederService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seed")
public class SeedingController {

    private final MockDataSeederService mockDataSeederService;

    public SeedingController(MockDataSeederService mockDataSeederService) {
        this.mockDataSeederService = mockDataSeederService;
    }

    @GetMapping
    public ResponseEntity<String> seedData() {
        String result = mockDataSeederService.seedMockData();
        if (result.startsWith("Failed")) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
