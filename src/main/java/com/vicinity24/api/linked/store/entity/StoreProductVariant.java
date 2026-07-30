package com.vicinity24.api.linked.store.entity;

import com.vicinity24.api.linked.store.usertype.JsonbUserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity(name = "StoreProductVariant")
@Table(
        name = "store_product_variants",
        indexes = {
                @Index(name = "idx_store_product_variants_store", columnList = "store_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_product_variants_store_sku", columnNames = {"store_id", "sku"})
        }
)
@Filter(name = "tenantFilter", condition = "store_id = :storeId")
@Getter
@Setter
@NoArgsConstructor
public class StoreProductVariant extends StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    })
    private StoreProduct product;

    @Column(nullable = false)
    private String sku;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Type(JsonbUserType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> options = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    @PreUpdate
    void normalizeState() {
        if (options == null) {
            options = new LinkedHashMap<>();
        }
    }

    public void setProduct(StoreProduct product) {
        this.product = product;
        this.productId = product == null ? null : product.getId();
    }
}


