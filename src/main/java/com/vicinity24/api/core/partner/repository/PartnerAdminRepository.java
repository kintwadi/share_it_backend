package com.vicinity24.api.core.partner.repository;

import com.vicinity24.api.core.partner.model.PartnerAdmin;
import com.vicinity24.api.core.partner.model.PartnerAdminRole;
import com.vicinity24.api.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PartnerAdminRepository extends JpaRepository<PartnerAdmin, UUID> {
    @Query("select pa from PartnerAdmin pa where pa.user.id = :userId")
    List<PartnerAdmin> findAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from PartnerAdmin pa where pa.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    @Query("select (count(pa) > 0) from PartnerAdmin pa where pa.user.id = :userId and pa.partner.id = :partnerId and pa.role = :role")
    boolean existsByUserAndPartnerAndRole(@Param("userId") UUID userId, @Param("partnerId") UUID partnerId, @Param("role") PartnerAdminRole role);

    @Query("select pa.user from PartnerAdmin pa where pa.partner.id = :partnerId and pa.role = :role order by pa.createdAt asc")
    List<User> findUsersByPartnerIdAndRoleOrderByCreatedAtAsc(@Param("partnerId") UUID partnerId, @Param("role") PartnerAdminRole role);
}
