package com.vicinity24.api.bicycle.controller;

import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import com.vicinity24.api.bicycle.dto.BikeAdminAttributeRequest;
import com.vicinity24.api.bicycle.dto.BikeAdminUpsertBikeRequest;
import com.vicinity24.api.bicycle.dto.BikeShopDetailDto;
import com.vicinity24.api.bicycle.service.BikeAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bikes/admin")
public class BikeAdminController {

    private final BikeAdminService bikeAdminService;

    public BikeAdminController(BikeAdminService bikeAdminService) {
        this.bikeAdminService = bikeAdminService;
    }

    @PostMapping("/attributes")
    public ResponseEntity<BikeSpecAttribute> createAttribute(@Valid @RequestBody BikeAdminAttributeRequest request) {
        return ResponseEntity.ok(bikeAdminService.createAttribute(request));
    }

    @PostMapping("/catalog")
    public ResponseEntity<BikeShopDetailDto> upsertBike(@Valid @RequestBody BikeAdminUpsertBikeRequest request) {
        return ResponseEntity.ok(bikeAdminService.upsertBike(request));
    }
}
