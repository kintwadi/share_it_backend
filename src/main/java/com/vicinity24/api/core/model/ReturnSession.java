package com.vicinity24.api.core.model;

import com.vicinity24.api.core.model.enums.ReturnStatus;
import com.vicinity24.api.core.model.enums.ReturnMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "return_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnSession {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id")
    private User borrower;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_id")
    private User lender;

    private String borrowerQrCode;
    private String lenderQrCode;
    private String borrowerCode;
    private String lenderCode;

    @Enumerated(EnumType.STRING)
    private ReturnMode returnMethod;

    private LocalDateTime borrowerScannedAt;
    private LocalDateTime lenderScannedAt;

    private LocalDateTime manualBorrowerConfirmedAt;
    private LocalDateTime manualLenderConfirmedAt;

    private String conciergeWitnessId;
    private String returnPlace;
    private String returnAddress;
    private LocalDateTime submittedAt;
    private LocalDateTime acceptedAt;

    private String disputeReason;
    private String disputePhotoUrl;

    @Enumerated(EnumType.STRING)
    private ReturnStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
