package com.nearshare.api.enterprise.controller;

import com.nearshare.api.enterprise.dto.EnterpriseSampleDataDTO;
import com.nearshare.api.enterprise.service.EnterpriseSampleDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise/sample-data")
@RequiredArgsConstructor
public class EnterpriseSampleDataController {
    private final EnterpriseSampleDataService service;

    @PostMapping("/load")
    public ResponseEntity<EnterpriseSampleDataDTO> load(
            @RequestParam(name = "reset", defaultValue = "false") boolean reset,
            @RequestParam(name = "limit", defaultValue = "80") int limit
    ) {
        return ResponseEntity.ok(service.load(reset, limit));
    }
}

