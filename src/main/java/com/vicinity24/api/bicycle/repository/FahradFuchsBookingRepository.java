package com.vicinity24.api.bicycle.repository;

import com.vicinity24.api.bicycle.domain.model.FahradFuchsBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FahradFuchsBookingRepository extends JpaRepository<FahradFuchsBooking, UUID> {

    List<FahradFuchsBooking> findByBorrowerIdOrderByCreatedAtDesc(UUID borrowerId);

    boolean existsByBookingReference(String bookingReference);
}
