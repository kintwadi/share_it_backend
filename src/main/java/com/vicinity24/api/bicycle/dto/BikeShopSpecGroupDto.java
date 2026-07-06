package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record BikeShopSpecGroupDto(
        String attributeName,
        boolean custom,
        List<String> values
) {
}
