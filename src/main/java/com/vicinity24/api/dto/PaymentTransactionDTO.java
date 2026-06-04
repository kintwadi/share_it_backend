package com.vicinity24.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PaymentTransactionDTO {
    private UUID id;
    private UUID listingId;
    private String listingTitle;
    private UUID payerId;
    private UUID payeeId;
    private String direction;
    private BigDecimal amount;
    private BigDecimal rentalAmount;
    private BigDecimal serviceFeeAmount;
    private BigDecimal depositAmount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String releaseError;
    private LocalDateTime timestamp;
}
