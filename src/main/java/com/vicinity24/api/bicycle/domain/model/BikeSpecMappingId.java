package com.vicinity24.api.bicycle.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BikeSpecMappingId implements Serializable {

    @Column(name = "bike_id", nullable = false)
    private Long bikeId;

    @Column(name = "spec_value_id", nullable = false)
    private Long specValueId;
}
