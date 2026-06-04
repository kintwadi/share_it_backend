package com.vicinity24.api.dto;

import com.vicinity24.api.model.enums.ReturnStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReturnDTOs {

    @Data
    @Builder
    public static class ReturnSessionResponse {
        private UUID id;
        private UUID listingId;
        private String borrowerCode;
        private String lenderCode;
        private boolean borrowerScanned;
        private boolean lenderScanned;
        private boolean manualBorrowerConfirmed;
        private boolean manualLenderConfirmed;
        private ReturnStatus status;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class ScanRequest {
        private String qrCode;
    }

    @Data
    public static class ManualFallbackRequest {
        private String itemNumber;
        private String conciergeWitnessId;
    }

    @Data
    public static class DisputeRequest {
        private String reason;
        private String photoUrl;
        private String conciergeWitnessId;
    }
}
