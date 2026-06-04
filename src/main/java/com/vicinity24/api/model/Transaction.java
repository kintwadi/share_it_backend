package com.vicinity24.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private User payer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id")
    private User payee;
    
    private BigDecimal amount;
    @Column(name = "rental_amount")
    private BigDecimal rentalAmount;
    @Column(name = "service_fee_amount")
    private BigDecimal serviceFeeAmount;
    @Column(name = "deposit_amount")
    private BigDecimal depositAmount;
    private String currency;
    private String paymentMethod; // CARD, PAYPAL, CASH
    private String paymentToken; // PaymentIntent ID or similar
    
    @Column(name = "borrower_path")
    private String borrowerPath; // DEPOSIT, VERIFIED, FEE, NA
    
    private LocalDateTime timestamp;
    private String status; // ESCROWED, RELEASED, DISPUTED, PENDING, FAILED

    @Column(name = "stripe_transfer_id")
    private String stripeTransferId;
    @Column(name = "stripe_refund_id")
    private String stripeRefundId;
    @Column(name = "released_at")
    private LocalDateTime releasedAt;
    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;
    @Column(name = "release_error", columnDefinition = "TEXT")
    private String releaseError;
}
