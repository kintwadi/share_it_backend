package com.nearshare.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PickupLocationDTO {
    private UUID id;
    private String referenceId;
    private String name;
    private String address;
    private LocationDTO location;
}

