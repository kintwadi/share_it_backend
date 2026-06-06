package com.vicinity24.api.model;

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
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    @Id
    private UUID id;
    private String title;
    private Double latitude;
    private Double longitude;
    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private String geohash;
}

