package com.vicinity24.api.controller;

import com.vicinity24.api.dto.LocationResponse;
import com.vicinity24.api.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/reverse")
    public ResponseEntity<LocationResponse> reverse(@RequestParam("lat") double lat, @RequestParam("lng") double lng) {
        return ResponseEntity.ok(locationService.reverseGeocode(lat, lng));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<LocationResponse>> autocomplete(
            @RequestParam("q") String q,
            @RequestParam(value = "countryCodes", required = false) String countryCodes,
            @RequestParam(value = "limit", required = false, defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(locationService.autocomplete(q, countryCodes, limit));
    }
}

