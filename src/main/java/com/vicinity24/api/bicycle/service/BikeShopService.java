package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.model.BikeSku;
import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import com.vicinity24.api.bicycle.domain.model.BikeSpecValue;
import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import com.vicinity24.api.bicycle.dto.BikeShopDetailDto;
import com.vicinity24.api.bicycle.dto.BikeShopFilterOptionDto;
import com.vicinity24.api.bicycle.dto.BikeShopFilterSectionDto;
import com.vicinity24.api.bicycle.dto.BikeShopFilterSelectionRequest;
import com.vicinity24.api.bicycle.dto.BikeShopPaginationDto;
import com.vicinity24.api.bicycle.dto.BikeShopProductPreviewDto;
import com.vicinity24.api.bicycle.dto.BikeShopSearchRequest;
import com.vicinity24.api.bicycle.dto.BikeShopSearchResponse;
import com.vicinity24.api.bicycle.dto.BikeShopSidebarDto;
import com.vicinity24.api.bicycle.dto.BikeShopSkuDto;
import com.vicinity24.api.bicycle.dto.BikeShopSpecGroupDto;
import com.vicinity24.api.bicycle.repository.BikeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecAttributeRepository;
import com.vicinity24.api.bicycle.repository.BikeSpecValueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class BikeShopService {

    private static final Set<String> COMPONENT_ATTRIBUTE_NAMES = Set.of(
            "brake type",
            "gearing type",
            "shifting type",
            "drivetrain",
            "drivetrain speed",
            "groupset",
            "cassette",
            "chainring",
            "motor system",
            "battery range"
    );

    private final BikeRepository bikeRepository;
    private final BikeSpecAttributeRepository attributeRepository;
    private final BikeSpecValueRepository valueRepository;
    private final BikeTenantProvider tenantProvider;
    private final BikeSpecificationBuilder specificationBuilder;
    private final BikeCatalogImageService imageService;

    public BikeShopService(
            BikeRepository bikeRepository,
            BikeSpecAttributeRepository attributeRepository,
            BikeSpecValueRepository valueRepository,
            BikeTenantProvider tenantProvider,
            BikeSpecificationBuilder specificationBuilder,
            BikeCatalogImageService imageService
    ) {
        this.bikeRepository = bikeRepository;
        this.attributeRepository = attributeRepository;
        this.valueRepository = valueRepository;
        this.tenantProvider = tenantProvider;
        this.specificationBuilder = specificationBuilder;
        this.imageService = imageService;
    }

    @Transactional(readOnly = true)
    public BikeShopSearchResponse search(BikeShopSearchRequest request) {
        String tenantId = tenantProvider.requireTenantId();
        List<BikeSpecAttribute> attributes = attributeRepository.findByTenantIdOrderByAttributeNameAsc(tenantId);
        BikeSearchCriteria criteria = resolveCriteria(request, tenantId, attributes);

        Page<Bike> page = bikeRepository.findAll(
                specificationBuilder.build(criteria),
                PageRequest.of(criteria.page(), criteria.size(), resolveSort(criteria.sort()))
        );

        return new BikeShopSearchResponse(
                page.getContent().stream().map(this::mapPreview).toList(),
                buildPagination(page),
                buildSidebar(criteria, attributes)
        );
    }

    @Transactional(readOnly = true)
    public BikeShopDetailDto getDetail(Long bikeId) {
        String tenantId = tenantProvider.requireTenantId();
        Bike bike = bikeRepository.findWithSkusAndSpecsByIdAndTenantId(bikeId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "bike_not_found"));

        List<BikeShopSkuDto> skus = bike.getSkus().stream()
                .sorted(Comparator.comparing(BikeSku::getColorName).thenComparing(BikeSku::getSizeValue))
                .map(sku -> new BikeShopSkuDto(
                        sku.getId(),
                        sku.getSkuCode(),
                        sku.getColorName(),
                        sku.getSizeValue(),
                        sku.getRiderHeightMinCm(),
                        sku.getRiderHeightMaxCm(),
                        sku.getStackMm(),
                        sku.getReachMm(),
                        sku.getStockQuantity(),
                        sku.getPriceModifier()
                ))
                .toList();

        Map<String, List<String>> specsByAttribute = new TreeMap<>();
        Map<String, Boolean> customByAttribute = new LinkedHashMap<>();
        bike.getSpecMappings().forEach(mapping -> {
            String attributeName = mapping.getSpecValue().getAttribute().getAttributeName();
            specsByAttribute.computeIfAbsent(attributeName, ignored -> new ArrayList<>()).add(mapping.getSpecValue().getValueText());
            customByAttribute.put(attributeName, Boolean.TRUE.equals(mapping.getSpecValue().getAttribute().getIsCustom()));
        });

        List<BikeShopSpecGroupDto> specs = specsByAttribute.entrySet().stream()
                .map(entry -> new BikeShopSpecGroupDto(
                        entry.getKey(),
                        Boolean.TRUE.equals(customByAttribute.get(entry.getKey())),
                        entry.getValue().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()
                ))
                .toList();

        return new BikeShopDetailDto(
                bike.getId(),
                bike.getBrandName(),
                bike.getModelName(),
                bike.displayName(),
                bike.getModelYear(),
                bike.getCategory().name(),
                bike.getSaleType().name(),
                bike.getBasePrice(),
                bike.getDescription(),
                imageService.buildPreviewImage(bike),
                Boolean.TRUE.equals(bike.getIsActive()),
                skus,
                specs
        );
    }

    private BikeShopSidebarDto buildSidebar(BikeSearchCriteria criteria, List<BikeSpecAttribute> attributes) {
        List<BikeShopFilterSectionDto> sections = new ArrayList<>();
        sections.add(buildSaleTypeSection(criteria));
        sections.add(buildCategorySection(criteria));

        for (BikeSpecAttribute attribute : attributes) {
            if (criteria.saleType() == BikeSaleType.FRAMESET && isComponentSpecific(attribute.getAttributeName())) {
                continue;
            }
            sections.add(buildDynamicAttributeSection(criteria, attribute));
        }

        BigDecimal minAvailablePrice = findExtremePrice(criteria, true);
        BigDecimal maxAvailablePrice = findExtremePrice(criteria, false);

        return new BikeShopSidebarDto(
                minAvailablePrice,
                maxAvailablePrice,
                criteria.minPrice(),
                criteria.maxPrice(),
                criteria.saleType() == BikeSaleType.FRAMESET,
                sections
        );
    }

    private BikeShopFilterSectionDto buildSaleTypeSection(BikeSearchCriteria criteria) {
        List<BikeShopFilterOptionDto> options = List.of(BikeSaleType.COMPLETE_BIKE, BikeSaleType.FRAMESET).stream()
                .map(option -> {
                    long count = bikeRepository.count(specificationBuilder.build(criteria, Set.of(), Map.of(), option, null));
                    boolean selected = criteria.saleType() == option;
                    return new BikeShopFilterOptionDto(
                            option.name(),
                            formatEnum(option.name()),
                            displayOptionLabel("saleType", option.name(), formatEnum(option.name())),
                            count,
                            selected,
                            count == 0 && !selected
                    );
                })
                .toList();
        return new BikeShopFilterSectionDto("saleType", "Sale Type", displaySectionLabel("saleType", "Sale Type"), "SEGMENTED", false, false, false, options);
    }

    private BikeShopFilterSectionDto buildCategorySection(BikeSearchCriteria criteria) {
        List<BikeShopFilterOptionDto> options = EnumSet.allOf(BikeCategory.class).stream()
                .map(category -> {
                    long count = bikeRepository.count(specificationBuilder.build(
                            criteria,
                            Set.of(),
                            Map.of(),
                            null,
                            List.of(category)
                    ));
                    boolean selected = criteria.categories().contains(category);
                    return new BikeShopFilterOptionDto(
                            category.name(),
                            formatEnum(category.name()),
                            displayOptionLabel("category", category.name(), formatEnum(category.name())),
                            count,
                            selected,
                            count == 0 && !selected
                    );
                })
                .toList();
        return new BikeShopFilterSectionDto("category", "Category", displaySectionLabel("category", "Category"), "CHECKBOX_LIST", false, true, false, options);
    }

    private BikeShopFilterSectionDto buildDynamicAttributeSection(BikeSearchCriteria criteria, BikeSpecAttribute attribute) {
        String key = toFilterKey(attribute.getAttributeName());
        Map<String, Long> countsByValue = new LinkedHashMap<>();
        List<BikeSpecValue> values = valueRepository.findByAttributeIdOrderByValueTextAsc(attribute.getId());

        for (BikeSpecValue value : values) {
            long count = bikeRepository.count(specificationBuilder.build(
                    criteria,
                    Set.of(key),
                    Map.of(key, List.of(value.getValueText())),
                    null,
                    null
            ));
            countsByValue.put(value.getValueText(), count);
        }

        List<String> selectedValues = criteria.filters().stream()
                .filter(filter -> filter.key().equals(key))
                .findFirst()
                .map(BikeSearchCriteria.ResolvedFilterGroup::values)
                .orElse(List.of());

        List<BikeShopFilterOptionDto> options = values.stream()
                .map(value -> {
                    long count = countsByValue.getOrDefault(value.getValueText(), 0L);
                    boolean selected = selectedValues.stream().anyMatch(selectedValue -> selectedValue.equalsIgnoreCase(value.getValueText()));
                    return new BikeShopFilterOptionDto(
                            value.getValueText(),
                            value.getValueText(),
                            displayOptionLabel(key, value.getValueText(), value.getValueText()),
                            count,
                            selected,
                            count == 0 && !selected
                    );
                })
                .toList();

        return new BikeShopFilterSectionDto(
                key,
                attribute.getAttributeName(),
                displaySectionLabel(key, attribute.getAttributeName()),
                "CHECKBOX_LIST",
                Boolean.TRUE.equals(attribute.getIsCustom()),
                true,
                isComponentSpecific(attribute.getAttributeName()),
                options
        );
    }

    private BigDecimal findExtremePrice(BikeSearchCriteria criteria, boolean ascending) {
        BikeSearchCriteria contextWithoutPrice = new BikeSearchCriteria(
                criteria.tenantId(),
                criteria.query(),
                criteria.saleType(),
                criteria.categories(),
                null,
                null,
                criteria.sort(),
                0,
                1,
                criteria.filters()
        );
        return bikeRepository.findAll(
                        specificationBuilder.build(contextWithoutPrice),
                        PageRequest.of(0, 1, Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, "basePrice"))
                )
                .stream()
                .findFirst()
                .map(Bike::getBasePrice)
                .orElse(null);
    }

    private BikeShopProductPreviewDto mapPreview(Bike bike) {
        int totalStock = bike.getSkus().stream().map(BikeSku::getStockQuantity).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        String defaultColor = bike.getSkus().stream()
                .map(BikeSku::getColorName)
                .filter(color -> color != null && !color.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .findFirst()
                .orElse(imageService.buildSpecChip(bike.getCategory()));

        return new BikeShopProductPreviewDto(
                bike.getId(),
                bike.getBrandName(),
                bike.getModelName(),
                bike.displayName(),
                bike.getModelYear(),
                bike.getCategory().name(),
                bike.getSaleType().name(),
                bike.getBasePrice(),
                bike.getDescription(),
                imageService.buildPreviewImage(bike),
                Boolean.TRUE.equals(bike.getIsActive()),
                totalStock > 0,
                totalStock,
                bike.getSkus().size(),
                defaultColor
        );
    }

    private BikeSearchCriteria resolveCriteria(BikeShopSearchRequest request, String tenantId, List<BikeSpecAttribute> attributes) {
        Map<String, BikeSpecAttribute> attributesByKey = attributes.stream()
                .collect(Collectors.toMap(attribute -> toFilterKey(attribute.getAttributeName()), attribute -> attribute));

        BikeSaleType saleType = parseSaleType(request.saleType());
        Set<BikeCategory> categories = parseCategories(request.categories());
        List<BikeSearchCriteria.ResolvedFilterGroup> filters = new ArrayList<>();

        if (request.filters() != null) {
            for (BikeShopFilterSelectionRequest filter : request.filters()) {
                BikeSpecAttribute attribute = attributesByKey.get(filter.key());
                if (attribute == null) {
                    continue;
                }
                List<String> values = normalizeValues(filter.values());
                if (values.isEmpty()) {
                    continue;
                }
                boolean componentSpecific = isComponentSpecific(attribute.getAttributeName());
                if (saleType == BikeSaleType.FRAMESET && componentSpecific) {
                    continue;
                }
                filters.add(new BikeSearchCriteria.ResolvedFilterGroup(
                        filter.key(),
                        attribute.getAttributeName(),
                        values,
                        componentSpecific
                ));
            }
        }

        return new BikeSearchCriteria(
                tenantId,
                normalizeText(request.query()),
                saleType,
                categories,
                request.minPrice(),
                request.maxPrice(),
                request.resolvedSort(),
                request.resolvedPage(),
                request.resolvedSize(),
                filters
        );
    }

    private Sort resolveSort(String sort) {
        return switch (sort) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "basePrice").and(Sort.by("brandName")).and(Sort.by("modelName"));
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "basePrice").and(Sort.by("brandName")).and(Sort.by("modelName"));
            case "modelYearAsc" -> Sort.by(Sort.Direction.ASC, "modelYear").and(Sort.by("brandName"));
            case "modelYearDesc" -> Sort.by(Sort.Direction.DESC, "modelYear").and(Sort.by("brandName"));
            case "brandAsc" -> Sort.by(Sort.Direction.ASC, "brandName").and(Sort.by("modelName"));
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "brandName"));
        };
    }

    private BikeSaleType parseSaleType(String saleType) {
        if (saleType == null || saleType.isBlank()) {
            return null;
        }
        return BikeSaleType.valueOf(saleType.trim().toUpperCase(Locale.ROOT));
    }

    private Set<BikeCategory> parseCategories(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return EnumSet.noneOf(BikeCategory.class);
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> BikeCategory.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(BikeCategory.class)));
    }

    private List<String> normalizeValues(Collection<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BikeShopPaginationDto buildPagination(Page<Bike> page) {
        int totalPages = Math.max(page.getTotalPages(), 1);
        int currentPage = Math.min(page.getNumber(), totalPages - 1);
        int start = Math.max(0, currentPage - 4);
        int end = Math.min(totalPages, start + 9);
        if (end - start < 9) {
            start = Math.max(0, end - 9);
        }
        long from = page.getTotalElements() == 0 ? 0 : ((long) currentPage * page.getSize()) + 1;
        long to = page.getTotalElements() == 0 ? 0 : Math.min(from + page.getNumberOfElements() - 1, page.getTotalElements());

        return new BikeShopPaginationDto(
                currentPage,
                page.getSize(),
                totalPages,
                page.getTotalElements(),
                from,
                to,
                page.hasNext(),
                page.hasPrevious(),
                IntStream.range(start, end).boxed().toList()
        );
    }

    public static String toFilterKey(String attributeName) {
        if (attributeName == null || attributeName.isBlank()) {
            return "spec";
        }
        return attributeName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private boolean isComponentSpecific(String attributeName) {
        return COMPONENT_ATTRIBUTE_NAMES.contains(attributeName.toLowerCase(Locale.ROOT));
    }

    private String formatEnum(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder formatted = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!formatted.isEmpty()) {
                formatted.append(' ');
            }
            formatted.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                formatted.append(part.substring(1));
            }
        }
        return formatted.toString();
    }

    private String displaySectionLabel(String key, String fallback) {
        return switch (key) {
            case "saleType" -> "Sale Format";
            case "category" -> "Riding Category";
            case "wheel-size" -> "Wheel Format";
            case "mounting-points" -> "Accessory Mounts";
            case "motor-system" -> "Drive System";
            case "battery-range" -> "Range Profile";
            case "bottom-bracket" -> "Bottom Bracket Standard";
            default -> fallback;
        };
    }

    private String displayOptionLabel(String sectionKey, String value, String fallback) {
        return switch (sectionKey) {
            case "saleType" -> switch (value) {
                case "COMPLETE_BIKE" -> "Complete Bike";
                case "FRAMESET" -> "Frameset";
                default -> fallback;
            };
            case "category" -> switch (value) {
                case "MTB" -> "Mountain Bike";
                case "E_BIKE" -> "E-Bike";
                default -> fallback;
            };
            case "wheel-size" -> switch (value) {
                case "700c" -> "700C";
                case "29in" -> "29 in";
                default -> fallback;
            };
            case "mounting-points" -> switch (value) {
                case "Adventure Ready" -> "Adventure-Ready";
                case "Commuter Rack Ready" -> "Commuter Rack-Ready";
                default -> fallback;
            };
            default -> fallback;
        };
    }
}
