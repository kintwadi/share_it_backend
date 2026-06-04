package com.vicinity24.api.repository;

import com.vicinity24.api.model.Subscription;
import com.vicinity24.api.model.SubscriptionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, UUID> {

    List<SubscriptionInvoice> findBySubscriptionOrderByInvoiceDateDesc(Subscription subscription);
}
