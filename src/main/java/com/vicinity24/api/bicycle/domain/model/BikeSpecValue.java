package com.vicinity24.api.bicycle.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "bike_spec_values",
        indexes = {
                @Index(name = "idx_bike_spec_values_attribute", columnList = "attribute_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_attribute_value", columnNames = {"attribute_id", "value_text"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeSpecValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private BikeSpecAttribute attribute;

    @Column(name = "value_text", nullable = false, length = 100)
    private String valueText;

    @OneToMany(mappedBy = "specValue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BikeSpecMapping> mappings = new ArrayList<>();
}
