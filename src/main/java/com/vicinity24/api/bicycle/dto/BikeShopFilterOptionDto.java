package com.vicinity24.api.bicycle.dto;

public record BikeShopFilterOptionDto(
        String value,
        String label,
        String displayLabel,
        long count,
        boolean selected,
        boolean disabled
) {
}
