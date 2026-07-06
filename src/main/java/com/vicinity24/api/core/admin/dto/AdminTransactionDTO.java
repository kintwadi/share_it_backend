package com.vicinity24.api.core.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminTransactionDTO {
    private UUID id;
    private UUID listingId;
    private String listingTitle;
    private UUID payerId;
    private String payerEmail;
    private UUID payeeId;
    private String payeeEmail;
    private BigDecimal amount;
    private BigDecimal rentalAmount;
    private BigDecimal serviceFeeAmount;
    private BigDecimal depositAmount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String paymentToken;
    private String stripeTransferId;
    private String stripeRefundId;
    private String releaseError;
    private LocalDateTime timestamp;
}

