package com.nearshare.api.model;

import com.nearshare.api.model.embeddable.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "pickup_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeLocation {
    @Id
    private UUID id;
    @Column(name = "reference_id", unique = true)
    private String referenceId;
    private String name;
    private String address;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    @Embedded
    private Location location;
    private boolean active;
}

