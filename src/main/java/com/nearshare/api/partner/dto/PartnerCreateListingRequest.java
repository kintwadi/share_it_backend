package com.nearshare.api.partner.dto;

import com.nearshare.api.dto.CreateListingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PartnerCreateListingRequest extends CreateListingRequest {
    private UUID partnerId;
}
