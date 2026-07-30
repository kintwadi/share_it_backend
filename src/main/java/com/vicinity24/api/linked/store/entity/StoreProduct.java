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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity(name = "StoreProduct")
@Table(
        name = "store_products",
        indexes = {
                @Index(name = "idx_store_products_store", columnList = "store_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_products_store_sku", columnNames = {"store_id", "sku"}),
                @UniqueConstraint(name = "uk_store_products_id_store", columnNames = {"id", "store_id"})
        }
)
@Filter(name = "tenantFilter", condition = "store_id = :storeId")
@Getter
@Setter
@NoArgsConstructor
public class StoreProduct extends StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(name = "category_id")
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    })
    private StoreCategory category;

    @Type(JsonbUserType.class)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> properties = new LinkedHashMap<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "product")
    @OrderBy("createdAt ASC")
    private List<StoreProductVariant> variants = new ArrayList<>();

    @PrePersist
    @PreUpdate
    void normalizeState() {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        if (currency == null || currency.isBlank()) {
            currency = "EUR";
        }
    }

    public void setCategory(StoreCategory category) {
        this.category = category;
        this.categoryId = category == null ? null : category.getId();
    }
}


