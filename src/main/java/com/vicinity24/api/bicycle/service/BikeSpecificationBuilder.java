package com.vicinity24.api.bicycle.service;

import com.vicinity24.api.bicycle.domain.model.Bike;
import com.vicinity24.api.bicycle.domain.model.BikeSpecAttribute;
import com.vicinity24.api.bicycle.domain.model.BikeSpecMapping;
import com.vicinity24.api.bicycle.domain.model.BikeSpecValue;
import com.vicinity24.api.bicycle.domain.valueobject.BikeCategory;
import com.vicinity24.api.bicycle.domain.valueobject.BikeSaleType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class BikeSpecificationBuilder {

    public Specification<Bike> build(BikeSearchCriteria criteria) {
        return build(criteria, Set.of(), Map.of(), null, null);
    }

    public Specification<Bike> build(
            BikeSearchCriteria criteria,
            Set<String> excludedFilterKeys,
            Map<String, List<String>> overridingFilterValues,
            BikeSaleType saleTypeOverride,
            Collection<BikeCategory> categoriesOverride
    ) {
        Map<String, BikeSearchCriteria.ResolvedFilterGroup> groups = new LinkedHashMap<>();
        for (BikeSearchCriteria.ResolvedFilterGroup filter : criteria.filters()) {
            if (!excludedFilterKeys.contains(filter.key())) {
                groups.put(filter.key(), filter);
            }
        }
        overridingFilterValues.forEach((key, values) -> groups.computeIfPresent(
                key,
                (ignored, current) -> new BikeSearchCriteria.ResolvedFilterGroup(
                        current.key(),
                        current.attributeName(),
                        normalizeValues(values),
                        current.componentSpecific()
                )
        ));

        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), criteria.tenantId()));
            predicates.add(cb.isTrue(root.get("isActive")));

            if (criteria.query() != null && !criteria.query().isBlank()) {
                String search = "%" + criteria.query().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("brandName")), search),
                        cb.like(cb.lower(root.get("modelName")), search),
                        cb.like(cb.lower(root.get("description")), search)
                ));
            }

            BikeSaleType saleType = saleTypeOverride != null ? saleTypeOverride : criteria.saleType();
            if (saleType != null) {
                predicates.add(cb.equal(root.get("saleType"), saleType));
            }

            Collection<BikeCategory> categories = categoriesOverride != null ? categoriesOverride : criteria.categories();
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").in(categories));
            }

            if (criteria.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), criteria.minPrice()));
            }

            if (criteria.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), criteria.maxPrice()));
            }

            for (BikeSearchCriteria.ResolvedFilterGroup filter : groups.values()) {
                if (filter.values().isEmpty()) {
                    continue;
                }
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<BikeSpecMapping> mappingRoot = subquery.from(BikeSpecMapping.class);
                Join<BikeSpecMapping, BikeSpecValue> valueJoin = mappingRoot.join("specValue");
                Join<BikeSpecValue, BikeSpecAttribute> attributeJoin = valueJoin.join("attribute");
                subquery.select(cb.literal(1L));
                Predicate bikeJoin = cb.equal(mappingRoot.get("bike").get("id"), root.get("id"));
                Predicate attributeMatch = cb.equal(cb.lower(attributeJoin.get("attributeName")), filter.attributeName().toLowerCase(Locale.ROOT));
                Predicate valueMatch = cb.lower(valueJoin.get("valueText")).in(filter.values().stream()
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .toList());
                subquery.where(cb.and(bikeJoin, attributeMatch, valueMatch));
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
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
}
