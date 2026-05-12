package com.nearshare.api.controller;

import com.nearshare.api.dto.LocationDTO;
import com.nearshare.api.dto.PickupLocationDTO;
import com.nearshare.api.model.PickupLocation;
import com.nearshare.api.repository.PickupLocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pickup-locations")
public class PickupLocationController {
    private final PickupLocationRepository pickupLocationRepository;

    public PickupLocationController(PickupLocationRepository pickupLocationRepository) {
        this.pickupLocationRepository = pickupLocationRepository;
    }

    @GetMapping("/")
    public ResponseEntity<List<PickupLocationDTO>> list() {
        List<PickupLocation> active = pickupLocationRepository.findByActiveTrue();
        List<PickupLocationDTO> dtoList = active.stream().map(this::toDTO).toList();
        return ResponseEntity.ok(dtoList);
    }

    private PickupLocationDTO toDTO(PickupLocation p) {
        return PickupLocationDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .address(p.getAddress())
                .location(LocationDTO.builder()
                        .x(p.getLocation() != null ? p.getLocation().getLat() : null)
                        .y(p.getLocation() != null ? p.getLocation().getLng() : null)
                        .build())
                .build();
    }
}

