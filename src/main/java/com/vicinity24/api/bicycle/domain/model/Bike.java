package com.vicinity24.api.bicycle.domain.model;

import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "bikes",
        indexes = {
                @Index(name = "idx_bikes_tenant_active", columnList = "tenant_id,is_active"),
                @Index(name = "idx_bikes_tenant_category", columnList = "tenant_id,category"),
                @Index(name = "idx_bikes_tenant_sale_type", columnList = "tenant_id,sale_type"),
                @Index(name = "idx_bikes_tenant_price", columnList = "tenant_id,base_price")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_tenant_bike",
                        columnNames = {"tenant_id", "brand_name", "model_name", "model_year", "sale_type"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private BikeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private BikeSaleType saleType;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BikeSpecMapping> specMappings = new ArrayList<>();

    @OneToMany(mappedBy = "bike", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BikeSku> skus = new ArrayList<>();

    public String displayName() {
        return brandName + " " + modelName;
    }
}
