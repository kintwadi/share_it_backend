package com.vicinity24.api.core.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarItem {
    private UUID id;
    private String title;
    private String transactionType; // "SOLD", "LENT", "GIVEN"
    private BigDecimal price;
}
