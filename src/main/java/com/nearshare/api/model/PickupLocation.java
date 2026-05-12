package com.nearshare.api.model;

import com.nearshare.api.model.embeddable.Location;
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
public class PickupLocation {
    @Id
    private UUID id;
    private String name;
    private String address;
    @Embedded
    private Location location;
    private boolean active;
}

