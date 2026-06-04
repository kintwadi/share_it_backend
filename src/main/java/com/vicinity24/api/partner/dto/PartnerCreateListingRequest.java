package com.vicinity24.api.partner.dto;

import com.vicinity24.api.dto.CreateListingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PartnerCreateListingRequest extends CreateListingRequest {
    private UUID partnerId;
}
