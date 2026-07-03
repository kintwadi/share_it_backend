package com.vicinity24.api.bicycle.domain.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "bike_spec_mappings",
        indexes = {
                @Index(name = "idx_bike_spec_mappings_value", columnList = "spec_value_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BikeSpecMapping {

    @EmbeddedId
    private BikeSpecMappingId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bikeId")
    @JoinColumn(name = "bike_id", nullable = false)
    private Bike bike;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("specValueId")
    @JoinColumn(name = "spec_value_id", nullable = false)
    private BikeSpecValue specValue;
}
