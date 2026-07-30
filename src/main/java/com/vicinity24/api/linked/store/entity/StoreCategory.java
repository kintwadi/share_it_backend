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
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Entity(name = "StoreCategory")
@Table(
        name = "store_categories",
        indexes = {
                @Index(name = "idx_store_categories_store", columnList = "store_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_categories_store_parent_slug", columnNames = {"store_id", "parent_id", "slug"}),
                @UniqueConstraint(name = "uk_store_categories_id_store", columnNames = {"id", "store_id"})
        }
)
@Filter(name = "tenantFilter", condition = "store_id = :storeId")
@Getter
@Setter
@NoArgsConstructor
public class StoreCategory extends StoreScopedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "parent_id", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "store_id", referencedColumnName = "store_id", insertable = false, updatable = false)
    })
    private StoreCategory parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("name ASC")
    private List<StoreCategory> children = new ArrayList<>();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String slug;

    @Type(JsonbUserType.class)
    @Column(name = "attribute_schema", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributeSchema = new LinkedHashMap<>();

    @PrePersist
    @PreUpdate
    void normalizeState() {
        if (attributeSchema == null) {
            attributeSchema = new LinkedHashMap<>();
        }
    }

    public void setParent(StoreCategory parent) {
        this.parent = parent;
        this.parentId = parent == null ? null : parent.getId();
    }
}


