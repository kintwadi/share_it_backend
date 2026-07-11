package com.vicinity24.api.core.repository;

import com.vicinity24.api.core.model.ReviewInvite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReviewInviteRepository extends JpaRepository<ReviewInvite, UUID> {
    @EntityGraph(attributePaths = {"listing", "reviewer", "targetUser"})
    Optional<ReviewInvite> findByToken(String token);
    Optional<ReviewInvite> findFirstByReturnSessionIdAndReviewerIdAndTargetUserId(UUID returnSessionId, UUID reviewerId, UUID targetUserId);
}
