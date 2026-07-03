package com.vicinity24.api.bicycle.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BikeAdminAttributeRequest(
        @NotBlank String attributeName,
        Boolean isCustom,
        List<String> values
) {
}
