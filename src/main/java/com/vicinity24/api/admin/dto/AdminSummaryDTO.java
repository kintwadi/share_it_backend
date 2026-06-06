package com.vicinity24.api.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSummaryDTO {
    private long users;
    private long transactions;
    private long subscriptions;
    private long disputedListings;
    private long disputedReturns;
    private long releaseFailedTransactions;
}

