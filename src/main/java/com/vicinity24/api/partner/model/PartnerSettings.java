package com.vicinity24.api.partner.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerSettings {
    @Id
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id", unique = true)
    private Partner partner;
    private Integer maxLendingDays;
    private Integer depositCents;
    private Boolean autoApproval;
    private LocalDateTime updatedAt;
}
