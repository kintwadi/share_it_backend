package com.nearshare.api.repository;

import com.nearshare.api.model.Item;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    interface ItemDistanceRow {
        UUID getId();
        Double getDistanceKm();
    }

    @Query(value = """
            select t.id as id, t.distance_km as distanceKm
            from (
                select i.id as id,
                       (6371 * acos(
                           cos(radians(:borrowerLat)) * cos(radians(i.latitude)) *
                           cos(radians(i.longitude) - radians(:borrowerLng)) +
                           sin(radians(:borrowerLat)) * sin(radians(i.latitude))
                       )) as distance_km
                from items i
                where i.latitude is not null
                  and i.longitude is not null
            ) t
            where t.distance_km <= :radiusKm
            order by t.distance_km asc
            """, nativeQuery = true)
    List<ItemDistanceRow> findNearby(
            @Param("borrowerLat") double borrowerLat,
            @Param("borrowerLng") double borrowerLng,
            @Param("radiusKm") double radiusKm,
            Pageable pageable
    );
}

