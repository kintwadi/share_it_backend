package com.nearshare.api.recommendation.service;

import com.nearshare.api.recommendation.model.MahoutIdMapping;
import com.nearshare.api.recommendation.repository.MahoutIdMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class MahoutIdService {
    private final MahoutIdMappingRepository repository;
    private final AtomicLong idCounter = new AtomicLong(0);

    @Transactional
    public Long getMahoutId(UUID entityId, String entityType) {
        return repository.findByEntityIdAndEntityType(entityId, entityType)
                .map(MahoutIdMapping::getMahoutId)
                .orElseGet(() -> createMapping(entityId, entityType));
    }

    @Transactional
    public UUID getEntityId(Long mahoutId, String entityType) {
        return repository.findByMahoutIdAndEntityType(mahoutId, entityType)
                .map(MahoutIdMapping::getEntityId)
                .orElse(null);
    }

    private Long createMapping(UUID entityId, String entityType) {
        synchronized (this) {
            // Check again in case of race condition
            return repository.findByEntityIdAndEntityType(entityId, entityType)
                    .map(MahoutIdMapping::getMahoutId)
                    .orElseGet(() -> {
                        if (idCounter.get() == 0) {
                            MahoutIdMapping max = repository.findTopByOrderByMahoutIdDesc();
                            idCounter.set(max != null ? max.getMahoutId() : 0);
                        }
                        Long newId = idCounter.incrementAndGet();
                        MahoutIdMapping mapping = MahoutIdMapping.builder()
                                .entityId(entityId)
                                .entityType(entityType)
                                .mahoutId(newId)
                                .build();
                        repository.save(mapping);
                        return newId;
                    });
        }
    }
}
