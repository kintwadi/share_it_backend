package com.vicinity24.api.core.service;

import com.vicinity24.api.core.config.RuntimeSettingsService;
import com.vicinity24.api.core.dto.SubscriptionDTO;
import com.vicinity24.api.core.model.Subscription;
import com.vicinity24.api.core.model.SubscriptionVerificationCode;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.enums.VerificationStatus;
import com.vicinity24.api.core.payment.StripePayment;
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
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class BorrowerSubscriptionService {
    private static final Logger logger = LoggerFactory.getLogger(BorrowerSubscriptionService.class);
    private static final String BORROWER_SCOPE = "borrower";
    private static final String BORROWER_PLAN_TYPE = "verified";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final StripePayment stripePayment;
    private final RuntimeSettingsService runtimeSettingsService;
    private final SubscriptionService subscriptionService;

    @Value("${subscription.plus.stripe_price_id:}")
    private String plusStripePriceId;

    @Value("${subscription.plus.trial_days:14}")
    private int plusTrialDays;

    @Value("${subscription.plus.enabled:true}")
    private boolean plusEnabled;

    @Value("${subscription.verification.expiry_minutes:10}")
    private int verificationExpiryMinutes;

    @Value("${FRONTEND_BASE_URL:http://localhost:3001}")
    private String frontendBaseUrl;

    public BorrowerSubscriptionService(
            SubscriptionRepository subscriptionRepository,
            SubscriptionVerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            EmailService emailService,
            StripePayment stripePayment,
            RuntimeSettingsService runtimeSettingsService,
            SubscriptionService subscriptionService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.stripePayment = stripePayment;
        this.runtimeSettingsService = runtimeSettingsService;
        this.subscriptionService = subscriptionService;
    }

    public boolean isBorrowingSubscriptionEnabled() {
        return runtimeSettingsService == null || runtimeSettingsService.isEnabled("settings.enable.borrowing.subscription", true);
    }

    public Optional<SubscriptionDTO> getCurrentBorrowerSubscription(User user) {
        Optional<Subscription> subscription = findLatestBorrowerSubscription(user);
        if (subscription.isEmpty()) {
            subscription = recoverBorrowerSubscriptionFromStripe(user);
        }
        return subscription.map(subscriptionService::toSubscriptionDTO);
    }

    public boolean canBorrowDirectly(User user) {
        if (!isBorrowingSubscriptionEnabled()) return false;
        return findLatestBorrowerSubscription(user)
                .map(sub -> isActiveStatus(sub.getStatus()))
                .orElse(false);
    }

    @Transactional
    public void sendVerificationCode(User user, String language) {
        requireBorrowingSubscriptionEnabled();
        Optional<SubscriptionVerificationCode> existingCode = findLatestActiveVerificationCode(user);
        if (existingCode.isPresent()) {
            SubscriptionVerificationCode codeEntity = existingCode.get();
            if (!BORROWER_PLAN_TYPE.equalsIgnoreCase(String.valueOf(codeEntity.getPlanType()))) {
                codeEntity.setPlanType(BORROWER_PLAN_TYPE);
            }
            if (language != null && !language.isBlank()) {
                codeEntity.setLanguage(language.trim().toLowerCase());
            }
            verificationCodeRepository.save(codeEntity);
            emailService.sendSubscriptionVerificationEmail(
                    user.getEmail(),
                    safeUserName(user),
                    codeEntity.getCode(),
                    BORROWER_PLAN_TYPE,
                    codeEntity.getLanguage()
            );
            return;
        }

        verificationCodeRepository.deleteAllForUser(user);
        SubscriptionVerificationCode verificationCode = SubscriptionVerificationCode.builder()
                .user(user)
                .code(generateRandomCode())
                .expiryDate(LocalDateTime.now().plusMinutes(verificationExpiryMinutes))
                .planType(BORROWER_PLAN_TYPE)
                .language(language != null && !language.isBlank() ? language.trim().toLowerCase() : null)
                .used(false)
                .build();
        verificationCodeRepository.save(verificationCode);
        emailService.sendSubscriptionVerificationEmail(
                user.getEmail(),
                safeUserName(user),
                verificationCode.getCode(),
                BORROWER_PLAN_TYPE,
                verificationCode.getLanguage()
        );
    }

    @Transactional
    public void verifyCode(User user, String code) {
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
        verificationCode.setPlanType(BORROWER_PLAN_TYPE);
        verificationCodeRepository.save(verificationCode);

        if (user.getVerificationStatus() != VerificationStatus.VERIFIED) {
            user.setVerificationStatus(VerificationStatus.VERIFIED);
            userRepository.save(user);
        }
    }

    public Map<String, String> createCheckoutSession(User user, String returnPath, String updatedBy) {
        requireBorrowingSubscriptionEnabled();
        if (hasActiveBorrowerSubscription(user)) {
            throw new RuntimeException("borrower_subscription_already_active");
        }

        String stripePriceId = ensureBorrowerStripePriceConfigured(updatedBy != null ? updatedBy : user.getEmail());
        String effectiveReturnPath = (returnPath == null || returnPath.isBlank()) ? "/dashboard?borrower_subscription=1" : returnPath;
        if (!effectiveReturnPath.startsWith("/")) {
            effectiveReturnPath = "/" + effectiveReturnPath;
        }
        String successUrl = buildFrontendAppUrl(effectiveReturnPath);
        String cancelUrl = buildFrontendAppUrl("/verification/email?scope=borrower");

        var session = stripePayment.createSubscriptionCheckoutSession(
                stripePriceId,
                runtimeSettingsService.getInt("subscription.plus.trial_days", plusTrialDays),
                successUrl,
                cancelUrl,
                user.getEmail(),
                user.getId().toString(),
                Map.of(
                        "subscription_scope", BORROWER_SCOPE,
                        "subscription_plan_type", BORROWER_PLAN_TYPE
                )
        );

        return Map.of("sessionId", session.getId(), "url", session.getUrl());
    }

    @Transactional
    public Map<String, String> syncFromSession(User user, String sessionId) {
        var session = stripePayment.retrieveCheckoutSession(sessionId);
        String subscriptionId = session.getSubscription() != null ? session.getSubscription().toString() : null;
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new RuntimeException("missing_borrower_subscription");
        }
        try {
            com.stripe.model.Subscription stripeSubscription = com.stripe.model.Subscription.retrieve(subscriptionId);
            String status = stripeSubscription.getStatus();
            syncBorrowerSubscriptionFromStripe(user, subscriptionId, status);
            return Map.of(
                    "status", "synced",
                    "stripeSubscriptionId", subscriptionId,
                    "stripeStatus", status
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Transactional
    public void syncBorrowerSubscriptionFromStripe(User user, String stripeSubscriptionId, String stripeStatus) {
        String effectiveStatus = (stripeStatus == null || stripeStatus.isBlank()) ? "active" : stripeStatus;
        Optional<Subscription> existing = findLatestBorrowerSubscription(user);
        Subscription subscription = existing.orElseGet(() -> Subscription.builder()
                .user(user)
                .planType(BORROWER_PLAN_TYPE)
                .createdAt(LocalDateTime.now())
                .build());

        subscription.setPlanType(BORROWER_PLAN_TYPE);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStatus(effectiveStatus);
        if (subscription.getTrialStart() == null) {
            subscription.setTrialStart(LocalDateTime.now());
        }
        subscriptionRepository.save(subscription);
        subscriptionService.refreshUserBenefits(user);
    }

    @Transactional
    public void cancelBorrowerSubscription(User user) {
        Subscription sub = findLatestBorrowerSubscription(user)
                .orElseGet(() -> recoverBorrowerSubscriptionFromStripe(user).orElseThrow(() -> new RuntimeException("borrower_subscription_not_found")));

        String currentStatus = String.valueOf(sub.getStatus() == null ? "" : sub.getStatus()).trim().toLowerCase();
        if ("canceled".equals(currentStatus) || "cancelled".equals(currentStatus)) {
            return;
        }

        if (sub.getStripeSubscriptionId() != null && !sub.getStripeSubscriptionId().isBlank()) {
            stripePayment.cancelSubscription(sub.getStripeSubscriptionId());
        }

        sub.setStatus("canceled");
        subscriptionRepository.save(sub);
        subscriptionService.refreshUserBenefits(user);
    }

    private void requireBorrowingSubscriptionEnabled() {
        if (!isBorrowingSubscriptionEnabled()) {
            throw new RuntimeException("borrower_subscription_disabled");
        }
    }

    private boolean hasActiveBorrowerSubscription(User user) {
        return findLatestBorrowerSubscription(user)
                .map(sub -> isActiveStatus(sub.getStatus()))
                .orElse(false);
    }

    private boolean isBorrowerSubscriptionPlan(Subscription subscription) {
        String planType = normalizePlanType(subscription != null ? subscription.getPlanType() : null);
        return BORROWER_PLAN_TYPE.equalsIgnoreCase(planType)
                || SubscriptionService.PLAN_PLUS.equalsIgnoreCase(planType);
    }

    private String normalizePlanType(String planType) {
        return planType == null ? "" : planType.trim().toLowerCase();
    }

    private boolean isActiveStatus(String status) {
        String normalizedStatus = status == null ? "" : status.trim().toLowerCase();
        return "active".equals(normalizedStatus) || "trialing".equals(normalizedStatus) || "trial_active".equals(normalizedStatus);
    }

    private Optional<Subscription> findLatestBorrowerSubscription(User user) {
        if (user == null) return Optional.empty();
        return subscriptionRepository.findByUser(user).stream()
                .filter(this::isBorrowerSubscriptionPlan)
                .max(java.util.Comparator.comparing(sub -> sub.getCreatedAt() != null ? sub.getCreatedAt() : LocalDateTime.MIN));
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
            syncBorrowerSubscriptionFromStripe(user, stripeSubscription.getId(), stripeSubscription.getStatus());
            return findLatestBorrowerSubscription(user);
        } catch (RuntimeException e) {
            logger.warn("Failed to recover borrower subscription from Stripe for user {}", user.getId(), e);
            return Optional.empty();
        }
    }

    private String ensureBorrowerStripePriceConfigured(String updatedBy) {
        String configuredPriceId = runtimeSettingsService.getString("subscription.plus.stripe_price_id", plusStripePriceId);
        requireBorrowingSubscriptionEnabled();

        String currency = runtimeSettingsService.getString("subscription.currency", "EUR");
        int amountCents = runtimeSettingsService.getInt("subscription.plus.monthly_amount_cents", 499);
        StripePayment.SubscriptionCatalogEntry entry = stripePayment.ensureSubscriptionCatalogEntry(
                BORROWER_PLAN_TYPE,
                "Vicinity24 Borrowing Verified",
                currency,
                amountCents,
                "month",
                configuredPriceId
        );

        RuntimeSettingsService.AdminSettingsUpdate update = new RuntimeSettingsService.AdminSettingsUpdate();
        update.key = "subscription.plus.stripe_price_id";
        update.value = entry.priceId();
        runtimeSettingsService.applyUpdates(List.of(update), updatedBy != null ? updatedBy : "borrower-subscription-auto-provision");
        return entry.priceId();
    }

    private String buildFrontendAppUrl(String routePath) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = routePath == null || routePath.isBlank() ? "/" : routePath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (base.contains("/#/")) {
            return base + path;
        }
        if (base.endsWith("/#")) {
            return base + path;
        }
        return base + "/#" + path;
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
            logger.warn("Found {} active borrower verification codes for user {}. Keeping the latest and deleting duplicates.",
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
            logger.warn("Found {} borrower verification-code matches for user {} and code {}. Using the latest record.",
                    matches.size(), user != null ? user.getId() : null, code);
        }
        return Optional.of(matches.get(0));
    }

    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%04d", random.nextInt(10_000));
    }
}
