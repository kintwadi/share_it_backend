package com.vicinity24.api.bicycle.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface BicycleCatalogRow {
    UUID getId();
    String getTitle();
    String getDescription();
    String getCategory();
    String getImageUrl();
    BigDecimal getHourlyRate();
    String getCity();
    String getCountry();
    String getFrameSize();
    String getBikeType();
    Integer getAssemblyBufferMinutes();
    Boolean getRentToOwnEligible();
    BigDecimal getRetailPurchasePrice();
    String getInventoryStatus();
    LocalDateTime getCreatedAt();
}
