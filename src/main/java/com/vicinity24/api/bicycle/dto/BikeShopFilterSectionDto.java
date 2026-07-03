package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record BikeShopFilterSectionDto(
        String key,
        String label,
        String displayLabel,
        String uiControl,
        boolean isCustom,
        boolean multiSelect,
        boolean componentSpecific,
        List<BikeShopFilterOptionDto> options
) {
}
