package com.vicinity24.api.core.partner.dto;

import com.vicinity24.api.core.dto.CreateListingRequest;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PartnerCreateListingRequest extends CreateListingRequest {
    private UUID partnerId;
}
