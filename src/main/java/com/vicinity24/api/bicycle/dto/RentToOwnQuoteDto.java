package com.vicinity24.api.bicycle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentToOwnQuoteDto {
    private UUID listingId;
    private UUID borrowerId;
    private BigDecimal retailPurchasePrice;
    private BigDecimal rentalCreditApplied;
    private BigDecimal settlementAmount;
    private String currency;
    private boolean rentToOwnEligible;
}
