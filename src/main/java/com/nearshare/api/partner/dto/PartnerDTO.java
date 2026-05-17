package com.nearshare.api.partner.dto;

import com.nearshare.api.partner.model.PartnerStatus;
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
public class PartnerDTO {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String contactPerson;
    private PartnerStatus status;
}
