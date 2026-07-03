package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.BicycleListing;
import com.vicinity24.api.bicycle.dto.RentToOwnQuoteDto;
import com.vicinity24.api.bicycle.repository.BicycleListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class RentToOwnConversionService {

    private final BicycleListingRepository bicycleListingRepository;
    private final CorePaymentGateway corePaymentGateway;

    public RentToOwnConversionService(BicycleListingRepository bicycleListingRepository, CorePaymentGateway corePaymentGateway) {
        this.bicycleListingRepository = bicycleListingRepository;
        this.corePaymentGateway = corePaymentGateway;
    }

    @Transactional(readOnly = true)
    public RentToOwnQuoteDto quote(UUID listingId, UUID borrowerId) {
        BicycleListing bicycleListing = bicycleListingRepository.findWithCoreListingByListingId(listingId)
                .orElseThrow(() -> new RuntimeException("bike_listing_not_found"));

        BigDecimal retailPrice = bicycleListing.getRetailPurchasePrice() != null
                ? bicycleListing.getRetailPurchasePrice()
                : BigDecimal.ZERO;
        BigDecimal rentalCredit = corePaymentGateway.getTotalPaidForListing(listingId, borrowerId);
        BigDecimal settlementAmount = retailPrice.subtract(rentalCredit);
        if (settlementAmount.signum() < 0) {
            settlementAmount = BigDecimal.ZERO;
        }

        return RentToOwnQuoteDto.builder()
                .listingId(listingId)
                .borrowerId(borrowerId)
                .retailPurchasePrice(retailPrice)
                .rentalCreditApplied(rentalCredit)
                .settlementAmount(settlementAmount)
                .currency("EUR")
                .rentToOwnEligible(bicycleListing.isRentToOwnEligible())
                .build();
    }

    @Transactional(readOnly = true)
    public RentToOwnQuoteDto convert(UUID listingId, UUID borrowerId, String paymentMethod, String paymentToken) {
        RentToOwnQuoteDto quote = quote(listingId, borrowerId);
        if (!quote.isRentToOwnEligible()) {
            throw new RuntimeException("rent_to_own_not_available");
        }
        boolean success = corePaymentGateway.executeCharge(
                quote.getSettlementAmount(),
                quote.getCurrency(),
                paymentMethod,
                paymentToken
        );
        if (!success) {
            throw new RuntimeException("rent_to_own_charge_failed");
        }
        return quote;
    }
}
