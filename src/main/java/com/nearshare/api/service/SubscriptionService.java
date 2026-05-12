package com.nearshare.api.service;

import com.nearshare.api.dto.SubscriptionDTO;
import com.nearshare.api.dto.SubscriptionInvoiceDTO;
import com.nearshare.api.model.Subscription;
import com.nearshare.api.model.SubscriptionInvoice;
import com.nearshare.api.model.SubscriptionVerificationCode;
import com.nearshare.api.model.User;
import com.nearshare.api.repository.SubscriptionInvoiceRepository;
import com.nearshare.api.repository.SubscriptionRepository;
import com.nearshare.api.repository.SubscriptionVerificationCodeRepository;
import com.nearshare.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionInvoiceRepository subscriptionInvoiceRepository;
    private final SubscriptionVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final com.nearshare.api.payment.StripePayment stripePayment;
    private final TrustScoreService trustScoreService;

    @Value("${subscription.pro.plan_type:pro}")
    private String proPlanType;

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
            com.nearshare.api.payment.StripePayment stripePayment,
            TrustScoreService trustScoreService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionInvoiceRepository = subscriptionInvoiceRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.stripePayment = stripePayment;
        this.trustScoreService = trustScoreService;
    }



    @Transactional
    public void sendVerificationCode(User user, String planType, String language) {
        String requestedPlan = planType != null && !planType.isBlank() ? planType.trim().toLowerCase() : null;
        String requestedLanguage = language != null && !language.isBlank() ? language.trim().toLowerCase() : null;
        // Check if there's already an active verification code for this user
        Optional<SubscriptionVerificationCode> existingCode = verificationCodeRepository.findActiveCodeForUser(
            user, LocalDateTime.now());
        
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

    @Transactional
    public void createStarterSubscription(User user) {
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
        Optional<SubscriptionVerificationCode> codeOpt = verificationCodeRepository.findByUserAndCode(user, code);
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
        if (grantVerifiedMark && user.getVerificationStatus() != com.nearshare.api.model.enums.VerificationStatus.VERIFIED) {
            user.setVerificationStatus(com.nearshare.api.model.enums.VerificationStatus.VERIFIED);
            userRepository.save(user);
        }
    }

    public Optional<SubscriptionDTO> getCurrentSubscription(User user) {
        return subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user).map(this::toDTO);
    }

    public boolean isLenderPlan(User user) {
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
        // Only consider "active" and "trialing" as active subscriptions that should block new purchases
        List<String> activeStatuses = List.of("active", "trialing");
        List<Subscription> activeSubscriptions = subscriptionRepository.findByUserAndStatusIn(user, activeStatuses);
        return !activeSubscriptions.isEmpty();
    }

    public boolean hasActivePaidSubscription(User user) {
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

    public boolean canCancelSubscription(User user) {
        Optional<Subscription> subscriptionWithStripeId = subscriptionRepository.findFirstByUserAndStripeSubscriptionIdIsNotNullOrderByCreatedAtDesc(user);
        if (subscriptionWithStripeId.isPresent()) {
            Subscription sub = subscriptionWithStripeId.get();
            return ("active".equals(sub.getStatus()) || "trialing".equals(sub.getStatus()));
        }
        return false;
    }

    @Transactional
    public String fixSubscriptionStatus(User user) {
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
        String targetPlan = (planType != null && !planType.isBlank()) ? planType : proPlanType;

        Optional<Subscription> existing = subscriptionRepository.findFirstByUserOrderByCreatedAtDesc(user);
        Subscription sub;
        
        if (existing.isPresent()) {
            sub = existing.get();
            sub.setStripeSubscriptionId(stripeSubscriptionId);
            sub.setPlanType(targetPlan);
        } else {
            sub = Subscription.builder()
                .user(user)
                .planType(targetPlan)
                .createdAt(now)
                .stripeSubscriptionId(stripeSubscriptionId)
                .build();
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
    public com.nearshare.api.dto.SubscriptionUpgradePreviewDTO previewUpgrade(User user, String newPlanType) {
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
        
        return com.nearshare.api.dto.SubscriptionUpgradePreviewDTO.builder()
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
            user.setVerificationStatus(com.nearshare.api.model.enums.VerificationStatus.VERIFIED);
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
        user.setVerificationStatus(com.nearshare.api.model.enums.VerificationStatus.VERIFIED);
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
                .trialStart(subscription.getTrialStart())
                .trialEnd(subscription.getTrialEnd())
                .autoChargeAmountCents(subscription.getAutoChargeAmountCents())
                .autoChargeDate(subscription.getAutoChargeDate())
                .build();
    }

    private String generateRandomCode() {
        Random random = new Random();
        int value = 1000 + random.nextInt(9000);
        return String.valueOf(value);
    }
}
