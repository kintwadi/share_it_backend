package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.model.BikeSku;
import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import com.vicinity24.api.bicycle.domain.model.BikeSpecMapping;
import com.vicinity24.api.bicycle.domain.model.BikeSpecMappingId;
import com.vicinity24.api.bicycle.domain.model.BikeSpecValue;
import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import com.vicinity24.api.bicycle.dto.BikeAdminAttributeRequest;
import com.vicinity24.api.bicycle.dto.BikeAdminUpsertBikeRequest;
import com.vicinity24.api.bicycle.dto.BikeShopDetailDto;
import com.vicinity24.api.bicycle.repository.BikeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecAttributeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class BikeAdminService {

    private final BikeRepository bikeRepository;
    private final BikeSpecAttributeRepository attributeRepository;
    private final BikeSpecValueRepository valueRepository;
    private final BikeTenantProvider tenantProvider;
    private final BikeShopService bikeShopService;

    public BikeAdminService(
            BikeRepository bikeRepository,
            BikeSpecAttributeRepository attributeRepository,
            BikeSpecValueRepository valueRepository,
            BikeTenantProvider tenantProvider,
            BikeShopService bikeShopService
    ) {
        this.bikeRepository = bikeRepository;
        this.attributeRepository = attributeRepository;
        this.valueRepository = valueRepository;
        this.tenantProvider = tenantProvider;
        this.bikeShopService = bikeShopService;
    }

    @Transactional
    public BikeSpecAttribute createAttribute(BikeAdminAttributeRequest request) {
        String tenantId = tenantProvider.requireTenantId();
        BikeSpecAttribute attribute = attributeRepository.findByTenantIdAndAttributeNameIgnoreCase(tenantId, request.attributeName().trim())
                .orElseGet(() -> attributeRepository.save(BikeSpecAttribute.builder()
                        .tenantId(tenantId)
                        .attributeName(request.attributeName().trim())
                        .isCustom(Boolean.TRUE.equals(request.isCustom()))
                        .build()));

        if (request.values() != null) {
            for (String value : request.values()) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                ensureValue(attribute, value.trim());
            }
        }
        return attributeRepository.findById(attribute.getId()).orElse(attribute);
    }

    @Transactional
    public BikeShopDetailDto upsertBike(BikeAdminUpsertBikeRequest request) {
        String tenantId = tenantProvider.requireTenantId();
        BikeSaleType saleType = parseSaleType(request.saleType());
        BikeCategory category = parseCategory(request.category());

        Bike bike = bikeRepository.findByTenantIdAndBrandNameIgnoreCaseAndModelNameIgnoreCaseAndModelYearAndSaleType(
                        tenantId,
                        request.brandName().trim(),
                        request.modelName().trim(),
                        request.modelYear(),
                        saleType
                )
                .orElseGet(() -> Bike.builder().tenantId(tenantId).createdAt(LocalDateTime.now()).build());

        bike.setBrandName(request.brandName().trim());
        bike.setModelName(request.modelName().trim());
        bike.setModelYear(request.modelYear());
        bike.setCategory(category);
        bike.setSaleType(saleType);
        bike.setBasePrice(request.basePrice());
        bike.setDescription(request.description());
        bike.setImageUrl(request.imageUrl());
        bike.setIsActive(request.isActive() == null || request.isActive());
        if (bike.getCreatedAt() == null) {
            bike.setCreatedAt(LocalDateTime.now());
        }

        bike = bikeRepository.saveAndFlush(bike);
        bike.getSpecMappings().clear();
        bike.getSkus().clear();

        Set<BikeSpecValue> resolvedValues = new LinkedHashSet<>();
        if (request.specs() != null) {
            for (BikeAdminUpsertBikeRequest.BikeAdminSpecSelectionRequest spec : request.specs()) {
                BikeSpecAttribute attribute = attributeRepository.findByTenantIdAndAttributeNameIgnoreCase(tenantId, spec.attributeName().trim())
                        .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "unknown_attribute: " + spec.attributeName()));
                for (String rawValue : spec.values()) {
                    if (rawValue == null || rawValue.isBlank()) {
                        continue;
                    }
                    resolvedValues.add(ensureValue(attribute, rawValue.trim()));
                }
            }
        }

        List<BikeSpecMapping> mappings = new ArrayList<>();
        for (BikeSpecValue value : resolvedValues) {
            mappings.add(BikeSpecMapping.builder()
                    .id(new BikeSpecMappingId(bike.getId(), value.getId()))
                    .bike(bike)
                    .specValue(value)
                    .build());
        }
        bike.getSpecMappings().addAll(mappings);

        for (BikeAdminUpsertBikeRequest.BikeAdminSkuRequest skuRequest : request.skus()) {
            bike.getSkus().add(BikeSku.builder()
                    .bike(bike)
                    .skuCode(skuRequest.skuCode().trim())
                    .colorName(skuRequest.colorName().trim())
                    .sizeValue(skuRequest.sizeValue().trim())
                    .riderHeightMinCm(skuRequest.riderHeightMinCm())
                    .riderHeightMaxCm(skuRequest.riderHeightMaxCm())
                    .stackMm(skuRequest.stackMm())
                    .reachMm(skuRequest.reachMm())
                    .stockQuantity(skuRequest.stockQuantity())
                    .priceModifier(skuRequest.priceModifier() == null ? BigDecimal.ZERO : skuRequest.priceModifier())
                    .build());
        }

        bikeRepository.save(bike);
        return bikeShopService.getDetail(bike.getId());
    }

    private BikeSpecValue ensureValue(BikeSpecAttribute attribute, String rawValue) {
        return valueRepository.findByAttributeIdAndValueTextIgnoreCase(attribute.getId(), rawValue)
                .orElseGet(() -> valueRepository.save(BikeSpecValue.builder()
                        .attribute(attribute)
                        .valueText(rawValue)
                        .build()));
    }

    private BikeSaleType parseSaleType(String value) {
        try {
            return BikeSaleType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid_sale_type");
        }
    }

    private BikeCategory parseCategory(String value) {
        try {
            return BikeCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "invalid_category");
        }
    }
}
