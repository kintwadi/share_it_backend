package com.vicinity24.api.bicycle.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "bike_skus",
        indexes = {
                @Index(name = "idx_bike_skus_bike", columnList = "bike_id"),
                @Index(name = "idx_bike_skus_stock", columnList = "stock_quantity")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_tenant_sku", columnNames = {"sku_code"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @Column(name = "sku_code", nullable = false, length = 100)
    private String skuCode;

    @Column(name = "color_name", nullable = false, length = 50)
    private String colorName;

    @Column(name = "size_value", nullable = false, length = 20)
    private String sizeValue;

    @Column(name = "rider_height_min_cm")
    private Integer riderHeightMinCm;

    @Column(name = "rider_height_max_cm")
    private Integer riderHeightMaxCm;

    @Column(name = "stack_mm")
    private Integer stackMm;

    @Column(name = "reach_mm")
    private Integer reachMm;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "price_modifier", precision = 10, scale = 2)
    private BigDecimal priceModifier;
}
