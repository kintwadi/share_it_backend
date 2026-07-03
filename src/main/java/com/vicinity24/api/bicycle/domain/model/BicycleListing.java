package com.vicinity24.api.bicycle.domain.model;

import com.vicinity24.api.bicycle.domain.valueobject.BikeType;
import com.vicinity24.api.bicycle.domain.valueobject.InventoryStatus;
import com.vicinity24.api.core.model.Listing;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity 
@Table(name = "bike_listings", schema = "bicycle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BicycleListing {

    @Id
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing coreListing;

    @Column(name = "frame_size", length = 32)
    private String frameSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "bike_type", length = 32, nullable = false)
    private BikeType bikeType;

    @Column(name = "assembly_buffer_minutes", nullable = false)
    private Integer assemblyBufferMinutes;

    @Column(name = "rent_to_own_eligible", nullable = false)
    private boolean rentToOwnEligible;

    @Column(name = "retail_purchase_price", precision = 19, scale = 2)
    private BigDecimal retailPurchasePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", length = 32, nullable = false)
    private InventoryStatus inventoryStatus;

    public boolean requiresHeavyAssembly() {
        return assemblyBufferMinutes != null && assemblyBufferMinutes > 120;
    }
}
