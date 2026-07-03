package com.vicinity24.api.bicycle.dto;

import java.util.List;

public record FahradFuchsStorefrontDto(
        FahradFuchsStoreDto store,
        List<FahradFuchsCatalogItemDto> bikes
) {
}
