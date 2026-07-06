package com.vicinity24.api.bicycle.controller;

import com.vicinity24.api.bicycle.dto.BikeShopDetailDto;
import com.vicinity24.api.bicycle.dto.BikeShopSearchRequest;
import com.vicinity24.api.bicycle.dto.BikeShopSearchResponse;
import com.vicinity24.api.bicycle.service.BikeShopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bikes/shop")
public class BikeShopController {

    private final BikeShopService bikeShopService;

    public BikeShopController(BikeShopService bikeShopService) {
        this.bikeShopService = bikeShopService;
    }

    @PostMapping("/search")
    public ResponseEntity<BikeShopSearchResponse> search(@Valid @RequestBody(required = false) BikeShopSearchRequest request) {
        BikeShopSearchRequest payload = request == null
                ? new BikeShopSearchRequest(null, null, null, null, null, null, 0, 12, null)
                : request;
        return ResponseEntity.ok(bikeShopService.search(payload));
    }

    @GetMapping("/{bikeId}")
    public ResponseEntity<BikeShopDetailDto> getDetail(@PathVariable("bikeId") Long bikeId) {
        return ResponseEntity.ok(bikeShopService.getDetail(bikeId));
    }
}
