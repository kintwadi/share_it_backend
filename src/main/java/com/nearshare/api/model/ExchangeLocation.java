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
    @Column(name = "street_address")
    private String streetAddress;
    @Column(name = "city")
    private String city;
    @Column(name = "postal_code")
    private String postalCode;
    @Column(name = "country")
    private String country;
    @Embedded
    private Location location;
    @Column(name = "operating_time_from")
    private String operatingTimeFrom;
    @Column(name = "operating_time_to")
    private String operatingTimeTo;
    private boolean active;
}
