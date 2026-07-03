package com.vicinity24.api.bicycle.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BikeShopFilterSelectionRequest(
        @NotBlank String key,
        List<String> values
) {
}
