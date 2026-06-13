package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.dto.ExchangeLocationDTO;
import com.vicinity24.api.core.dto.LocationDTO;
import com.vicinity24.api.core.model.ExchangeLocation;
import com.vicinity24.api.core.repository.ExchangeLocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pickup-locations")
public class ExchangeLocationController {
    private final ExchangeLocationRepository exchangeLocationRepository;

    public ExchangeLocationController(ExchangeLocationRepository exchangeLocationRepository) {
        this.exchangeLocationRepository = exchangeLocationRepository;
    }

    @GetMapping("/")
    public ResponseEntity<List<ExchangeLocationDTO>> list() {
        List<ExchangeLocation> active = exchangeLocationRepository.findByActiveTrue();
        List<ExchangeLocationDTO> dtoList = active.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtoList);
    }

    private ExchangeLocationDTO toDTO(ExchangeLocation p) {
        return ExchangeLocationDTO.builder()
                .id(p.getId())
                .referenceId(p.getReferenceId())
                .name(p.getName())
                .address(p.getAddress())
                .location(LocationDTO.builder()
                        .x(p.getLocation() != null ? p.getLocation().getLat() : null)
                        .y(p.getLocation() != null ? p.getLocation().getLng() : null)
                        .build())
                .operatingTimeFrom(p.getOperatingTimeFrom())
                .operatingTimeTo(p.getOperatingTimeTo())
                .build();
    }
}
