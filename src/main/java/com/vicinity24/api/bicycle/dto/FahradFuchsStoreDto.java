package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record FahradFuchsStoreDto(
        String storeName,
        String addressLine1,
        String cityLine,
        String phone,
        String email,
        String mapUrl,
        List<String> openingHours
) {
}
