package com.vicinity24.api.core.service;

import com.vicinity24.api.core.dto.SubscriptionUpgradePreviewDTO;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import com.vicinity24.api.core.payment.StripePayment;
import com.vicinity24.api.core.dto.SubscriptionDTO;
import com.vicinity24.api.core.dto.SubscriptionInvoiceDTO;
import com.vicinity24.api.core.model.Subscription;
import com.vicinity24.api.core.model.SubscriptionInvoice;
import com.vicinity24.api.core.model.SubscriptionVerificationCode;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.config.RuntimeSettingsService;
import com.vicinity24.api.core.repository.SubscriptionInvoiceRepository;
import com.vicinity24.api.core.repository.SubscriptionRepository;
import com.vicinity24.api.core.repository.SubscriptionVerificationCodeRepository;
import com.vicinity24.api.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class SubscriptionService {
    // PLATFORM SUBSCRIPTION ONLY:
    // This service manages the legacy platform/lender subscription model.
    // Keep it separate from BorrowerSubscriptionService, which handles the
    // borrower-facing verified borrowing subscription flow.

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
    private final SubscriptionVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final StripePayment stripePayment;
    private final TrustScoreService trustScoreService;
    private final RuntimeSettingsService runtimeSettingsService;

    @Value("${subscription.pro.plan_type:pro}")
    private String proPlanType;

    @Value("${subscription.plus.stripe_price_id:}")
    private String plusStripePriceId;

    @Value("${subscription.pro.stripe_price_id:}")
    private String configuredProStripePriceId;

    public static final String PLAN_STARTER = "starter";
    public static final String PLAN_PLUS = "plus";
    public static final String PLAN_PRO = "pro";

    @Value("${subscription.pro.trial_days:14}")
    private int proTrialDays;

    @Value("${subscription.pro.monthly_amount_cents:799}")
    private int proMonthlyAmountCents;

    @Value("${subscription.pro.trust_tier:pro}")
    private String proTrustTier;

    @Value("${subscription.pro.insurance_coverage_cents:200000}")
    private int proInsuranceCoverageCents;

    @Value("${subscription.verification.expiry_minutes:10}")
    private int verificationExpiryMinutes;

    @Value("${subscription.starter.enabled:true}")
    private boolean starterEnabled;

    @Value("${subscription.plus.enabled:true}")
    private boolean plusEnabled;

    @Value("${subscription.pro.enabled:true}")
    private boolean proEnabled;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionInvoiceRepository subscriptionInvoiceRepository,
            SubscriptionVerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            EmailService emailService,
            StripePayment stripePayment,
            TrustScoreService trustScoreService,
            RuntimeSettingsService runtimeSettingsService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.stripePayment = stripePayment;
        this.trustScoreService = trustScoreService;
        this.runtimeSettingsService = runtimeSettingsService;
    }

    public boolean isSubscriptionEnabled() {
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.enable.subscription", true);
    }

    public boolean isBorrowingSubscriptionEnabled() {
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.enable.borrowing.subscription", true);
    }

    private boolean isSubscriptionEnforced() {
        return isSubscriptionEnabled();
    }



    @Transactional
    public void sendVerificationCode(User user, String planType, String language) {
        // PLATFORM SUBSCRIPTION ONLY:
        // This email verification flow belongs to platform/lender plan onboarding.
        if (!isSubscriptionEnforced()) {
            throw new RuntimeException("subscription_disabled");
        }
        String requestedPlan = planType != null && !planType.isBlank() ? planType.trim().toLowerCase() : null;
        String requestedLanguage = language != null && !language.isBlank() ? language.trim().toLowerCase() : null;
        // Check if there's already an active verification code for this user
        Optional<SubscriptionVerificationCode> existingCode = findLatestActiveVerificationCode(user);
        
        if (existingCode.isPresent()) {
            String code = existingCode.get().getCode();
            try {
                String effectivePlan = requestedPlan != null ? requestedPlan : existingCode.get().getPlanType();
                String effectiveLanguage = requestedLanguage != null ? requestedLanguage : existingCode.get().getLanguage();
                if (requestedPlan != null && (existingCode.get().getPlanType() == null || !requestedPlan.equalsIgnoreCase(existingCode.get().getPlanType()))) {
                    existingCode.get().setPlanType(requestedPlan);
                    verificationCodeRepository.save(existingCode.get());
                }
                if (requestedLanguage != null && (existingCode.get().getLanguage() == null || !requestedLanguage.equalsIgnoreCase(existingCode.get().getLanguage()))) {
                    existingCode.get().setLanguage(requestedLanguage);
                    verificationCodeRepository.save(existingCode.get());
                }
                emailService.sendSubscriptionVerificationEmail(user.getEmail(), safeUserName(user), code, effectivePlan, effectiveLanguage);
            } catch (RuntimeException e) {
                logger.error("Failed to resend subscription verification email for user {}", user.getId(), e);
                throw e;
            }
            return;
        }
        
        verificationCodeRepository.deleteAllForUser(user);

        String code = generateRandomCode();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(verificationExpiryMinutes);

        SubscriptionVerificationCode verificationCode = SubscriptionVerificationCode.builder()
                .user(user)
                .code(code)
                .expiryDate(expiry)
                .planType(requestedPlan)
                .language(requestedLanguage)
                .used(false)
                .build();

        verificationCodeRepository.save(verificationCode);

        try {
            emailService.sendSubscriptionVerificationEmail(user.getEmail(), safeUserName(user), code, requestedPlan, requestedLanguage);
        } catch (RuntimeException e) {
            logger.error("Failed to send subscription verification email for user {}", user.getId(), e);
            throw e;
        }
    }

    private String safeUserName(User user) {
        String name = user != null ? user.getName() : null;
        if (name == null || name.isBlank()) name = user != null ? user.getDisplayName() : null;
        if (name == null || name.isBlank()) return "there";
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        return space > 0 ? trimmed.substring(0, space) : trimmed;
    }

    private Optional<SubscriptionVerificationCode> findLatestActiveVerificationCode(User user) {
        List<SubscriptionVerificationCode> activeCodes = verificationCodeRepository.findActiveCodesForUser(user, LocalDateTime.now());
        if (activeCodes.isEmpty()) {
            return Optional.empty();
        }
        if (activeCodes.size() > 1) {
            logger.warn("Found {} active subscription verification codes for user {}. Keeping the latest and deleting duplicates.",
                    activeCodes.size(), user != null ? user.getId() : null);
            verificationCodeRepository.deleteAll(activeCodes.subList(1, activeCodes.size()));
        }
        return Optional.of(activeCodes.get(0));
    }

    private Optional<SubscriptionVerificationCode> findLatestVerificationCodeByValue(User user, String code) {
        List<SubscriptionVerificationCode> matches = verificationCodeRepository.findAllByUserAndCodeOrderByExpiryDateDesc(user, code);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        if (matches.size() > 1) {
            logger.warn("Found {} subscription verification-code matches for user {} and code {}. Using the latest record.",
                    matches.size(), user != null ? user.getId() : null, code);
        }
        return Optional.of(matches.get(0));
    }

    @Transactional
    public void createStarterSubscription(User user) {
        // PLATFORM SUBSCRIPTION ONLY:
        // This applies the free starter tier used by the legacy platform plan model.
        if (!isSubscriptionEnforced()) {
            throw new RuntimeException("subscription_disabled");
        }
        // Cancel existing active subscriptions
        List<String> activeStatuses = List.of("active", "trialing", "trial_active");
        List<Subscription> activeSubs = subscriptionRepository.findByUserAndStatusIn(user, activeStatuses);
        for (Subscription sub : activeSubs) {
            if (sub.getStripeSubscriptionId() != null && !sub.getStripeSubscriptionId().isBlank()) {
                try {
                    stripePayment.cancelSubscription(sub.getStripeSubscriptionId());
                } catch (Exception e) {
                    logger.warn("Failed to cancel Stripe subscription {} for user {}", sub.getStripeSubscriptionId(), user.getId());
                }
            }
            sub.setStatus("canceled");
            subscriptionRepository.save(sub);
        }

        Subscription sub = Subscription.builder()
                .user(user)
                .planType(PLAN_STARTER)
                .status("active")
                .createdAt(LocalDateTime.now())
                .autoChargeAmountCents(0)
                .build();
        subscriptionRepository.save(sub);

        downgradeUserToStandard(user);
    }

    @Transactional
    public void verifyEmailCode(User user, String code) {
        // PLATFORM SUBSCRIPTION ONLY:
        // Borrower subscription email verification is handled elsewhere.
        if (!isSubscriptionEnforced()) {
            throw new RuntimeException("subscription_disabled");
        }
        Optional<SubscriptionVerificationCode> codeOpt = findLatestVerificationCodeByValue(user, code);
        if (codeOpt.isEmpty()) {
            throw new RuntimeException("invalid_verification_code");
        }

        SubscriptionVerificationCode verificationCode = codeOpt.get();

        if (verificationCode.isExpired()) {
            throw new RuntimeException("verification_code_expired");
        }

        if (verificationCode.isUsed()) {
            throw new RuntimeException("verification_code_already_used");
        }

        verificationCode.setUsed(true);
        verificationCodeRepository.save(verificationCode);

        String planType = verificationCode.getPlanType() != null ? verificationCode.getPlanType().trim().toLowerCase() : null;
        boolean grantVerifiedMark = PLAN_PLUS.equalsIgnoreCase(planType);
        if (grantVerifiedMark && user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            userRepository.save(user);
        }
    }

    public Optional<SubscriptionDTO> getCurrentSubscription(User user) {
        if (!isSubscriptionEnforced()) {
            return Optional.empty();
        }
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user).map(sub -> {
            reconcileSubscriptionPlanType(sub);
            return toDTO(sub);
        });
    }

    public Optional<SubscriptionDTO> getCurrentBorrowerSubscription(User user) {
        Optional<Subscription> subscription = findLatestBorrowerSubscription(user);
        if (subscription.isEmpty()) {
            subscription = recoverBorrowerSubscriptionFromStripe(user);
        }
        return subscription.map(sub -> {
            reconcileSubscriptionPlanType(sub);
            return toDTO(sub);
        });
    }

    public boolean isLenderPlan(User user) {
        if (!isSubscriptionEnforced()) {
            return true;
        }
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(sub -> {
                    if (sub.getPlanType() == null) {
                        return false;
                    }
                    String plan = sub.getPlanType().toLowerCase();
                    String status = sub.getStatus() != null ? sub.getStatus().toLowerCase() : "";
                    
                    boolean isPlanValid = plan.equalsIgnoreCase(PLAN_PLUS) ||
                                          plan.equalsIgnoreCase(PLAN_PRO);
                                          
                    if (!isPlanValid) {
                        return false;
                    }
                    return "active".equals(status) || "trialing".equals(status) || "trial_active".equals(status);
                })
                .orElse(false);
    }

    public boolean isPremiumLender(User user) {
        if (!isSubscriptionEnforced()) {
            return true;
        }
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(sub -> {
                    if (sub.getPlanType() == null) {
                        return false;
                    }
                    String plan = sub.getPlanType().toLowerCase();
                    String status = sub.getStatus() != null ? sub.getStatus().toLowerCase() : "";

                    boolean isPlanValid = plan.equalsIgnoreCase(PLAN_PLUS) ||
                                          plan.equalsIgnoreCase(PLAN_PRO);
                    if (!isPlanValid) {
                        return false;
                    }
                    return "active".equals(status) || "trialing".equals(status) || "trial_active".equals(status);
                })
                .orElse(false);
    }

    public boolean isProSeller(User user) {
        if (!isSubscriptionEnforced()) {
            return true;
        }
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .map(sub -> {
                    if (sub.getPlanType() == null) {
                        return false;
                    }
                    String plan = sub.getPlanType().toLowerCase();
                    String status = sub.getStatus() != null ? sub.getStatus().toLowerCase() : "";
                    
                    // Only PRO allows selling
                    boolean isPlanValid = plan.equalsIgnoreCase(PLAN_PRO);
                                          
                    if (!isPlanValid) {
                        return false;
                    }
                    return "active".equals(status) || "trialing".equals(status) || "trial_active".equals(status);
                })
                .orElse(false);
    }

    public boolean hasActiveSubscription(User user) {
        if (!isSubscriptionEnforced()) {
            return false;
        }
        // Only consider "active" and "trialing" as active subscriptions that should block new purchases
        List<String> activeStatuses = List.of("active", "trialing");
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndStatusIn(user, activeStatuses);
        return !activeSubscriptions.isEmpty();
    }

    public boolean hasActivePaidSubscription(User user) {
        if (!isSubscriptionEnforced()) {
            return false;
        }
        List<String> activeStatuses = List.of("active", "trialing", "trial_active");
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndStatusIn(user, activeStatuses);
        for (Subscription sub : activeSubscriptions) {
            if (sub.getStripeSubscriptionId() != null && !sub.getStripeSubscriptionId().isBlank()) {
                return true;
            }
            if (!PLAN_STARTER.equalsIgnoreCase(sub.getPlanType())) {
                return true;
            }
        }
        return false;
    }

    public boolean canBorrowDirectly(User user) {
        if (!isBorrowingSubscriptionEnabled()) {
            return false;
        }
        return findLatestBorrowerSubscription(user)
                .map(sub -> isBorrowingSubscriptionPlan(sub) && isActiveSubscriptionStatus(sub.getStatus()))
                .orElse(false);
    }

    @Transactional
    public void cancelBorrowerSubscription(User user) {
        Subscription sub = findLatestBorrowerSubscription(user)
                .orElseThrow(() -> new RuntimeException("borrower_subscription_not_found"));

        String currentStatus = sub.getStatus() != null ? sub.getStatus().trim().toLowerCase() : "";
        if ("canceled".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            return;
        }

        String stripeId = sub.getStripeSubscriptionId();
        if (stripeId != null && !stripeId.isBlank()) {
            reconcileSubscriptionPlanType(sub);
            stripePayment.cancelSubscription(stripeId);
        }

        sub.setStatus("canceled");
        subscriptionRepository.save(sub);
        refreshUserBenefitsFromSubscriptions(user);
    }

    public boolean canCancelSubscription(User user) {
        if (!isSubscriptionEnforced()) {
            return false;
        }
        Optional<Subscription> subscriptionWithStripeId = subscriptionRepository.findFirstByUserAndStripeSubscriptionIdIsNotNullOrderByCreatedAtDesc(user);
        if (subscriptionWithStripeId.isPresent()) {
            Subscription sub = subscriptionWithStripeId.get();
            return ("active".equals(sub.getStatus()) || "trialing".equals(sub.getStatus()));
        }
        return false;
    }

    @Transactional
    public String fixSubscriptionStatus(User user) {
        if (!isSubscriptionEnforced()) {
            return "Subscription feature is disabled";
        }
        List<Subscription> subscriptions = subscriptionRepository.findByUser(user);
        if (subscriptions.isEmpty()) {
            return "No subscriptions found for user";
        }
        int updatedCount = 0;
        for (Subscription sub : subscriptions) {
            if ("active".equals(sub.getStatus()) || "trialing".equals(sub.getStatus()) || "trial_active".equals(sub.getStatus())) {
                sub.setStatus("canceled");
                subscriptionRepository.save(sub);
                updatedCount++;
            }
        }
        return "Updated " + updatedCount + " subscription(s) to canceled status";
    }

    @Transactional
    public void syncProSubscriptionFromStripe(User user, String stripeSubscriptionId, String stripeStatus, String planType) {
        logger.info("Syncing Stripe subscription for user {}, id: {}, status: {}, plan: {}", 
                   user.getId(), stripeSubscriptionId, stripeStatus, planType);
        LocalDateTime now = LocalDateTime.now();
        String targetPlan = (planType != null && !planType.isBlank()) ? planType : null;
        if ((targetPlan == null || targetPlan.isBlank()) && stripeSubscriptionId != null && !stripeSubscriptionId.isBlank()) {
            targetPlan = resolvePlanTypeFromStripeSubscription(stripeSubscriptionId);
        }
        if (!isSubscriptionEnforced() && !(isBorrowingSubscriptionEnabled() && isBorrowingPlanType(targetPlan))) {
            return;
        }

        Optional<Subscription> existing = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        Subscription sub;
        
        if (existing.isPresent()) {
            sub = existing.get();
            sub.setStripeSubscriptionId(stripeSubscriptionId);
            if (targetPlan == null || targetPlan.isBlank()) {
                targetPlan = sub.getPlanType();
            } else {
                sub.setPlanType(targetPlan);
            }
        } else {
            if (targetPlan == null || targetPlan.isBlank()) {
                targetPlan = proPlanType;
            }
            sub = Subscription.builder()
                .user(user)
                .planType(targetPlan)
                .createdAt(now)
                .stripeSubscriptionId(stripeSubscriptionId)
                .build();
        }

        if (sub.getPlanType() == null || sub.getPlanType().isBlank()) {
            sub.setPlanType(targetPlan);
        }

        if (stripeStatus != null && !stripeStatus.isBlank()) {
            sub.setStatus(stripeStatus);
        } else if (sub.getStatus() == null || sub.getStatus().isBlank()) {
            sub.setStatus("active");
        }
        if (sub.getTrialStart() == null) {
            sub.setTrialStart(now);
        }
        subscriptionRepository.save(sub);

        if ("canceled".equals(stripeStatus)) {
            downgradeUserToStandard(user);
        } else {
            if (PLAN_PRO.equalsIgnoreCase(targetPlan) || "premium_lender".equalsIgnoreCase(targetPlan)) {
                upgradeUserToPro(user);
            }
        }
    }

    @Transactional
    public void syncProSubscriptionFromStripe(User user, String stripeSubscriptionId, String stripeStatus) {
        syncProSubscriptionFromStripe(user, stripeSubscriptionId, stripeStatus, proPlanType);
    }

    @Transactional
    public void updateInvoiceInfoFromStripe(String stripeSubscriptionId, String invoicePdfUrl, LocalDateTime invoiceDate) {
        if (!isSubscriptionEnforced()) {
            return;
        }
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) {
            return;
        }
        Optional<Subscription> existing = subscriptionRepository.findFirstByStripeSubscriptionIdOrderByCreatedAtDesc(stripeSubscriptionId);
        if (existing.isEmpty()) {
            logger.warn("No subscription found for stripeSubscriptionId {}, cannot create invoice record", stripeSubscriptionId);
            return;
        }
        Subscription sub = existing.get();
        SubscriptionInvoice invoice = SubscriptionInvoice.builder()
                .subscription(sub)
                .invoiceDate(invoiceDate)
                .invoicePdfUrl(invoicePdfUrl)
                .build();
        subscriptionInvoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public java.util.List<SubscriptionInvoiceDTO> getInvoicesForUser(User user) {
        if (!isSubscriptionEnforced()) {
            return java.util.List.of();
        }
        Optional<Subscription> subOpt = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        if (subOpt.isEmpty()) {
            return java.util.List.of();
        }
        java.util.List<SubscriptionInvoice> invoices =
                subscriptionInvoiceRepository.findBySubscriptionOrderByInvoiceDateDesc(subOpt.get());
        return invoices.stream()
                .map(inv -> SubscriptionInvoiceDTO.builder()
                        .id(inv.getId())
                        .invoiceDate(inv.getInvoiceDate())
                        .invoicePdfUrl(inv.getInvoicePdfUrl())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void cancelProSubscription(User user) {
        if (!isSubscriptionEnforced()) {
            return;
        }
        // First try to find a subscription with a Stripe ID
        Optional<Subscription> existing = subscriptionRepository.findFirstByUserAndStripeSubscriptionIdIsNotNullOrderByCreatedAtDesc(user);
        
        // If no subscription with Stripe ID found, fall back to the most recent subscription
        if (existing.isEmpty()) {
            existing = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        }
        
        if (existing.isEmpty()) {
            logger.warn("No subscription found for user {}, cannot cancel", user.getId());
            return;
        }
        Subscription sub = existing.get();
        String stripeId = sub.getStripeSubscriptionId();
        
        logger.info("Attempting to cancel subscription for user {}, stripeId: {}, current status: {}", 
                   user.getId(), stripeId, sub.getStatus());
        
        if (stripeId != null && !stripeId.isBlank()) {
            reconcileSubscriptionPlanType(sub);
            logger.info("Calling Stripe API to cancel subscription: {}", stripeId);
            // If this call throws, we deliberately do NOT touch the local database
            stripePayment.cancelSubscription(stripeId);
            logger.info("Stripe API call successful for subscription: {}", stripeId);
            // Local subscription status and user tier will be updated by Stripe webhook
            return;
        }

        logger.warn("No stripeSubscriptionId found for user {}, canceling locally only", user.getId());

        sub.setStatus("canceled");
        subscriptionRepository.save(sub);

        downgradeUserToStandard(user);

        logger.info("User {} downgraded to standard free plan after local-only cancellation", user.getId());
        logger.info("Local cancellation completed for user {}", user.getId());
    }

    @Transactional(readOnly = true)
    public SubscriptionUpgradePreviewDTO previewUpgrade(User user, String newPlanType) {
        if (!isSubscriptionEnforced()) {
            throw new RuntimeException("subscription_disabled");
        }
        String normalizedPlanType = normalizePlanType(newPlanType);
        // Mock logic for proration calculation
        // In reality, this would query Stripe or calculate based on DB subscription
        
        Optional<Subscription> currentSubOpt = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        
        String currentPlan = "free";
        int currentPriceCents = 0;
        LocalDateTime cycleEnd = LocalDateTime.now().plusDays(30); // Default if new
        
        if (currentSubOpt.isPresent()) {
            Subscription sub = currentSubOpt.get();
            currentPlan = sub.getPlanType();
            // Simplify: assume price based on plan type name
            if ("plus".equalsIgnoreCase(currentPlan)) currentPriceCents = 499;
            else if ("pro".equalsIgnoreCase(currentPlan)) currentPriceCents = 799;
            
            if (sub.getTrialEnd() != null && sub.getTrialEnd().isAfter(LocalDateTime.now())) {
                cycleEnd = sub.getTrialEnd();
            } else if (sub.getAutoChargeDate() != null) {
                cycleEnd = sub.getAutoChargeDate();
            }
        }
        
        int newPriceCents = "pro".equalsIgnoreCase(normalizedPlanType) ? 799 : 499;
        
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), cycleEnd);
        if (daysRemaining < 0) daysRemaining = 30; // Fallback
        
        // Calculate proration
        // Credit = (CurrentPrice * DaysRemaining) / 30
        int creditCents = (int) ((currentPriceCents * daysRemaining) / 30);
        
        // Charge = (NewPrice * DaysRemaining) / 30
        int chargeCents = (int) ((newPriceCents * daysRemaining) / 30);
        
        int netImmediateCharge = chargeCents - creditCents;
        
        return SubscriptionUpgradePreviewDTO.builder()
                .currentPlan(currentPlan)
                .newPlan(normalizedPlanType)
                .cycleEndDate(cycleEnd.toLocalDate())
                .remainingDays((int) daysRemaining)
                .creditCents(-creditCents) // Negative for display
                .chargeCents(chargeCents)
                .netImmediateChargeCents(netImmediateCharge)
                .nextFullChargeCents(newPriceCents)
                .nextFullChargeDate(cycleEnd.toLocalDate())
                .build();
    }

    @Transactional
    public SubscriptionDTO confirmUpgrade(User user, String newPlanType) {
        if (!isSubscriptionEnforced()) {
            throw new RuntimeException("subscription_disabled");
        }
        String normalizedPlanType = normalizePlanType(newPlanType);
        // Execute the upgrade
        // In reality, call Stripe to update subscription with proration behavior
        
        // For now, update local DB
        Optional<Subscription> currentSubOpt = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        
        LocalDateTime now = LocalDateTime.now();
        Subscription sub;
        
        if (currentSubOpt.isPresent()) {
            sub = currentSubOpt.get();
            sub.setPlanType(normalizedPlanType);
            sub.setAutoChargeAmountCents("pro".equalsIgnoreCase(normalizedPlanType) ? 799 : 299);
            // cycle remains same, but plan changes
        } else {
            sub = Subscription.builder()
                    .user(user)
                    .planType(normalizedPlanType)
                    .status("active")
                    .createdAt(now)
                    .autoChargeDate(now.plusDays(30))
                    .autoChargeAmountCents("pro".equalsIgnoreCase(normalizedPlanType) ? 799 : 299)
                    .build();
        }
        
        subscriptionRepository.save(sub);
        
        if ("pro".equalsIgnoreCase(normalizedPlanType)) {
            upgradeUserToPro(user);
        } else if ("plus".equalsIgnoreCase(normalizedPlanType) || "verified".equalsIgnoreCase(normalizedPlanType)) {
            user.setTrustTier("verified");
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            trustScoreService.updateTrustScoreForSubscription(user, normalizedPlanType);
            userRepository.save(user);
        }
        
        return toDTO(sub);
    }

    private String normalizePlanType(String planType) {
        if (planType == null) return null;
        String v = planType.trim().toLowerCase();
        if ("premium".equals(v)) return "pro";
        if ("verified".equals(v)) return "verified";
        if ("starter".equals(v)) return "starter";
        if ("plus".equals(v)) return "plus";
        if ("pro".equals(v)) return "pro";
        return planType;
    }

    private void upgradeUserToPro(User user) {
        user.setTrustTier(proTrustTier);
        user.setInsuranceCoverageCents(proInsuranceCoverageCents);
        user.setVerificationStatus(VerificationStatus.VERIFIED);
        trustScoreService.updateTrustScoreForSubscription(user, PLAN_PRO);
        userRepository.save(user);
        logger.info("User {} upgraded to pro (tier={}, insurance={})", 
                   user.getId(), proTrustTier, proInsuranceCoverageCents);
    }

    private void downgradeUserToStandard(User user) {
        user.setTrustTier("standard");
        user.setInsuranceCoverageCents(0);
        trustScoreService.updateTrustScoreForSubscription(user, PLAN_STARTER);
        userRepository.save(user);
    }

    private SubscriptionDTO toDTO(Subscription subscription) {
        return SubscriptionDTO.builder()
                .id(subscription.getId())
                .planType(subscription.getPlanType())
                .status(subscription.getStatus())
                .active(isActiveSubscriptionStatus(subscription.getStatus()))
                .borrowDirectly(isBorrowDirectSubscription(subscription))
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .autoChargeAmountCents(subscription.getAutoChargeAmountCents())
                .autoChargeDate(subscription.getAutoChargeDate())
                .build();
    }

    public SubscriptionDTO toSubscriptionDTO(Subscription subscription) {
        return toDTO(subscription);
    }

    private boolean isBorrowDirectSubscription(Subscription subscription) {
        return isBorrowingSubscriptionPlan(subscription) && isActiveSubscriptionStatus(subscription != null ? subscription.getStatus() : null);
    }

    private Optional<Subscription> findLatestBorrowerSubscription(User user) {
        if (user == null) return Optional.empty();
        return subscriptionRepository.findByUser(user).stream()
                .filter(this::isBorrowingSubscriptionPlan)
                .max(java.util.Comparator.comparing(
                        sub -> sub.getCreatedAt() != null ? sub.getCreatedAt() : LocalDateTime.MIN
                ));
    }

    private boolean isBorrowingSubscriptionPlan(Subscription subscription) {
        String planType = normalizePlanType(subscription != null ? subscription.getPlanType() : null);
        return isBorrowingPlanType(planType);
    }

    private boolean isBorrowingPlanType(String planType) {
        String normalizedPlanType = normalizePlanType(planType);
        return "verified".equalsIgnoreCase(String.valueOf(normalizedPlanType))
                || PLAN_PLUS.equalsIgnoreCase(String.valueOf(normalizedPlanType));
    }

    private boolean isActiveSubscriptionStatus(String status) {
        String normalized = status != null ? status.trim().toLowerCase() : "";
        return "active".equals(normalized) || "trialing".equals(normalized) || "trial_active".equals(normalized);
    }

    private Optional<Subscription> recoverBorrowerSubscriptionFromStripe(User user) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) return Optional.empty();
        String effectivePlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
        if (effectivePlusPriceId == null || effectivePlusPriceId.isBlank()) return Optional.empty();
        try {
            com.stripe.model.Subscription stripeSubscription = stripePayment.findLatestSubscriptionByCustomerEmail(
                    user.getEmail(),
                    List.of(effectivePlusPriceId)
            );
            if (stripeSubscription == null || stripeSubscription.getId() == null || stripeSubscription.getId().isBlank()) {
                return Optional.empty();
            }
            syncProSubscriptionFromStripe(user, stripeSubscription.getId(), stripeSubscription.getStatus(), PLAN_PLUS);
            return findLatestBorrowerSubscription(user);
        } catch (RuntimeException e) {
            logger.warn("Failed to recover borrower subscription from Stripe for user {}", user.getId(), e);
            return Optional.empty();
        }
    }

    private void refreshUserBenefitsFromSubscriptions(User user) {
        if (user == null) return;
        List<String> activeStatuses = List.of("active", "trialing", "trial_active");
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndStatusIn(user, activeStatuses);

        boolean hasPro = activeSubscriptions.stream().anyMatch(sub -> {
            String planType = normalizePlanType(sub.getPlanType());
            return PLAN_PRO.equalsIgnoreCase(planType) || "premium_lender".equalsIgnoreCase(planType);
        });
        if (hasPro) {
            upgradeUserToPro(user);
            return;
        }

        java.util.Optional<String> verifiedPlan = activeSubscriptions.stream()
                .map(Subscription::getPlanType)
                .map(this::normalizePlanType)
                .filter(plan -> "verified".equalsIgnoreCase(plan) || PLAN_PLUS.equalsIgnoreCase(plan))
                .findFirst();
        if (verifiedPlan.isPresent()) {
            user.setTrustTier("verified");
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            user.setInsuranceCoverageCents(0);
            trustScoreService.updateTrustScoreForSubscription(user, verifiedPlan.get());
            userRepository.save(user);
            return;
        }

        downgradeUserToStandard(user);
    }

    public void refreshUserBenefits(User user) {
        refreshUserBenefitsFromSubscriptions(user);
    }

    private void reconcileSubscriptionPlanType(Subscription sub) {
        if (sub == null) return;
        String stripeSubscriptionId = sub.getStripeSubscriptionId();
        if (stripeSubscriptionId == null || stripeSubscriptionId.isBlank()) return;
        String resolvedPlan = resolvePlanTypeFromStripeSubscription(stripeSubscriptionId);
        if (resolvedPlan != null && !resolvedPlan.equalsIgnoreCase(sub.getPlanType())) {
            sub.setPlanType(resolvedPlan);
            subscriptionRepository.save(sub);
        }
    }

    private String resolvePlanTypeFromStripeSubscription(String stripeSubscriptionId) {
        try {
            com.stripe.model.Subscription stripeSub = com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
            if (stripeSub == null || stripeSub.getItems() == null || stripeSub.getItems().getData() == null || stripeSub.getItems().getData().isEmpty()) {
                return null;
            }
            String priceId = stripeSub.getItems().getData().get(0).getPrice().getId();
            return resolvePlanTypeFromPriceId(priceId);
        } catch (Exception e) {
            logger.warn("Failed to resolve plan type from Stripe subscription {}", stripeSubscriptionId, e);
            return null;
        }
    }

    private String resolvePlanTypeFromPriceId(String priceId) {
        if (priceId == null || priceId.isBlank()) return null;
        String effectivePlusPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
        String effectiveProPriceId = runtimeSettingsService.getString("subscription.pro.stripe_price_id", configuredProStripePriceId);
        if (priceId.equals(effectivePlusPriceId)) return PLAN_PLUS;
        if (priceId.equals(effectiveProPriceId)) return PLAN_PRO;
        return null;
    }

    private String generateRandomCode() {
        Random random = new Random();
        int value = 1000 + random.nextInt(9000);
        return String.valueOf(value);
    }
}
