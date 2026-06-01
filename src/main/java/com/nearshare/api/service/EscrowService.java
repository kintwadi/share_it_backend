package com.nearshare.api.service;

import com.nearshare.api.model.ReturnSession;
import com.nearshare.api.model.Transaction;
import com.nearshare.api.model.User;
import com.nearshare.api.model.enums.AvailabilityStatus;
import com.nearshare.api.model.enums.ReturnStatus;
import com.nearshare.api.payment.StripePayment;
import com.nearshare.api.repository.TransactionRepository;
import com.stripe.model.Account;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private static final int MAX_RELEASE_ERROR_CHARS = 250;

    private final TransactionRepository transactionRepository;
    private final StripePayment stripePayment;
    private final UserService userService;

    @Transactional
    public void markDisputed(UUID listingId, String reason) {
        Transaction tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "ESCROWED").orElse(null);
        if (tx == null) {
            tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "RELEASE_FAILED").orElse(null);
        }
        if (tx == null) {
            return;
        }
        tx.setStatus("DISPUTED");
        tx.setDisputedAt(LocalDateTime.now());
        tx.setReleaseError(truncate(reason));
        transactionRepository.save(tx);
    }

    @Transactional
    public void releaseOnSuccessfulReturn(ReturnSession session) {
        if (session == null || session.getListing() == null || session.getListing().getId() == null) {
            return;
        }

        if (session.getStatus() != ReturnStatus.COMPLETED) {
            return;
        }

        UUID listingId = session.getListing().getId();
        Transaction tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "ESCROWED").orElse(null);
        if (tx == null) {
            tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "DISPUTED").orElse(null);
            if (tx == null) {
                return;
            }
        }
        attemptRelease(tx);
    }

    @Transactional
    public boolean adminAttemptReleaseForListing(UUID listingId) {
        if (listingId == null) {
            return false;
        }
        Transaction tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "ESCROWED").orElse(null);
        if (tx == null) {
            tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "DISPUTED").orElse(null);
        }
        if (tx == null) {
            tx = transactionRepository.findTopByListingIdAndStatusOrderByTimestampDesc(listingId, "RELEASE_FAILED").orElse(null);
        }
        if (tx == null) {
            return false;
        }
        attemptRelease(tx);
        return true;
    }

    @Transactional
    public int retryFailedReleasesForPayee(UUID payeeId) {
        if (payeeId == null) {
            return 0;
        }
        int attempted = 0;
        for (Transaction tx : transactionRepository.findByPayeeIdAndStatusOrderByTimestampDesc(payeeId, "RELEASE_FAILED")) {
            if (tx == null) {
                continue;
            }
            if (tx.getStripeTransferId() != null || tx.getStripeRefundId() != null) {
                continue;
            }
            if (tx.getListing() == null) {
                continue;
            }
            if (tx.getListing().getStatus() != AvailabilityStatus.AVAILABLE) {
                continue;
            }
            if (tx.getListing().getBorrower() != null) {
                continue;
            }
            attempted++;
            attemptRelease(tx);
        }
        return attempted;
    }

    @Transactional
    public boolean adminRetryReleaseForTransaction(UUID transactionId) {
        if (transactionId == null) {
            return false;
        }
        Transaction tx = transactionRepository.findById(transactionId).orElse(null);
        if (tx == null) {
            return false;
        }
        if (tx.getListing() == null || tx.getListing().getId() == null) {
            return false;
        }
        if (tx.getListing().getStatus() != AvailabilityStatus.AVAILABLE) {
            return false;
        }
        if (tx.getListing().getBorrower() != null) {
            return false;
        }
        attemptRelease(tx);
        return true;
    }

    private void attemptRelease(Transaction tx) {
        if (tx == null) {
            return;
        }
        if ("RELEASED".equals(tx.getStatus())) {
            return;
        }
        if (tx.getListing() != null && tx.getListing().getId() != null && tx.getListing().getStatus() == AvailabilityStatus.DISPUTED) {
            markDisputed(tx.getListing().getId(), "listing_disputed");
            return;
        }

        UUID listingId = tx.getListing() != null ? tx.getListing().getId() : null;
        try {
            BigDecimal rentalAmount = tx.getRentalAmount() != null ? tx.getRentalAmount() : BigDecimal.ZERO;
            BigDecimal depositAmount = tx.getDepositAmount() != null ? tx.getDepositAmount() : BigDecimal.ZERO;

            String currency = tx.getCurrency() != null ? tx.getCurrency().toLowerCase() : "usd";

            Map<String, String> baseMeta = new HashMap<>();
            baseMeta.put("listingId", listingId != null ? listingId.toString() : "");
            baseMeta.put("transactionId", tx.getId() != null ? tx.getId().toString() : "");

            Transfer transfer = null;
            if (rentalAmount.compareTo(BigDecimal.ZERO) > 0) {
                User payee = tx.getPayee();
                if (payee == null || payee.getId() == null) {
                    tx.setStatus("RELEASE_FAILED");
                    tx.setReleaseError("missing_payee");
                    transactionRepository.save(tx);
                    return;
                }

                payee = userService.getById(payee.getId());
                String destination = payee.getStripeConnectAccountId();
                if (destination == null || destination.isBlank()) {
                    tx.setStatus("RELEASE_FAILED");
                    tx.setReleaseError("missing_stripe_connect_account");
                    transactionRepository.save(tx);
                    return;
                }

                Account acct = stripePayment.retrieveAccount(destination);
                if (acct == null || acct.getDetailsSubmitted() == null || !acct.getDetailsSubmitted()) {
                    tx.setStatus("RELEASE_FAILED");
                    tx.setReleaseError("connect_onboarding_incomplete");
                    transactionRepository.save(tx);
                    return;
                }

                String transferGroup = listingId != null ? "listing:" + listingId : null;
                String token = tx.getPaymentToken();
                boolean isPaymentIntent = token != null && !token.isBlank() && token.startsWith("pi_");
                if (isPaymentIntent) {
                    transfer = stripePayment.createTransferFromPaymentIntent(rentalAmount, currency, destination, transferGroup, baseMeta, tx.getPaymentToken());
                } else {
                    transfer = stripePayment.createTransfer(rentalAmount, currency, destination, transferGroup, baseMeta);
                }
            }

            Refund refund = null;
            if (depositAmount.compareTo(BigDecimal.ZERO) > 0 && tx.getPaymentToken() != null && !tx.getPaymentToken().isBlank()) {
                refund = stripePayment.createRefund(tx.getPaymentToken(), depositAmount, baseMeta);
            }

            if (transfer != null) {
                tx.setStripeTransferId(transfer.getId());
            }
            if (refund != null) {
                tx.setStripeRefundId(refund.getId());
            }

            tx.setStatus("RELEASED");
            tx.setReleasedAt(LocalDateTime.now());
            tx.setReleaseError(null);
            transactionRepository.save(tx);
        } catch (Exception e) {
            tx.setStatus("RELEASE_FAILED");
            tx.setReleaseError(truncate(e.getMessage()));
            transactionRepository.save(tx);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.length() <= MAX_RELEASE_ERROR_CHARS) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_RELEASE_ERROR_CHARS);
    }
}
