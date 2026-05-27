package com.nearshare.api.model;

import com.nearshare.api.model.embeddable.Location;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ListingType;
import com.nearshare.api.partner.model.Partner;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {
    @Id
    private UUID id;
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private ListingType type;
    private String category;
    private String imageUrl;
    @ElementCollection
    private List<String> gallery;
    private BigDecimal hourlyRate;
    private boolean autoApprove;
    private boolean insuranceRequired;
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus status;
    @Embedded
    private Location location;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id")
    private User owner;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "partner_id")
    private Partner partner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_id")
    private User borrower;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_location_id")
    private PickupLocation pickupLocation;
    private String pickupLocationCustom;
    private String pickupLocationStreet;
    private String pickupLocationHouseNumber;
    private String pickupLocationCity;
    private String pickupLocationZip;
    private LocalDateTime createdAt;

    private boolean availableUnlimited;
    private LocalDateTime availableFrom;
    private LocalDateTime availableTo;

    @Column(name = "enterprise_only", nullable = false)
    private boolean enterpriseOnly;

    private LocalDateTime partnerSubmittedAt;
    private UUID partnerSubmittedBy;
    private LocalDateTime partnerReviewedAt;
    private UUID partnerReviewedBy;
    private String partnerReviewNote;
    private String partnerRejectionReason;

    private LocalDateTime partnerBorrowRequestedAt;
    private UUID partnerBorrowRequestedBy;
    private LocalDateTime partnerBorrowReviewedAt;
    private UUID partnerBorrowReviewedBy;
    private String partnerBorrowRejectionReason;

    @Column(name = "item_reference")
    private String itemReference;

    @PrePersist
    @PreUpdate
    private void validateOwnerOrPartner() {
        if ((owner == null && partner == null) || (owner != null && partner != null)) {
            throw new IllegalStateException("listing_must_have_exactly_one_owner_or_partner");
        }
    }
}
