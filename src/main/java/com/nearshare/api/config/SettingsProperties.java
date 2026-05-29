package com.nearshare.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "settings")
@Data
public class SettingsProperties {
    @Data
    public static class EnableConfig {
        private boolean enterprise = false;
        private boolean subscription = true;
        private boolean sell = false;
    }

    @Data
    public static class ServiceConfig {
        private double fee = 2.99;
    }

    @Data
    public static class TabConfig {
        private boolean enabled = true;
    }

    @Data
    public static class ReturnsConfig {
        @Data
        public static class ReturnMethodConfig {
            private boolean enabled = true;
        }

        private String mode;
        private ReturnMethodConfig qr = new ReturnMethodConfig();
        private ReturnMethodConfig manual = new ReturnMethodConfig();
        private ReturnMethodConfig dispute = new ReturnMethodConfig();
    }

    @Data
    public static class BorrowPathConfig {
        private boolean enabled = true;
    }

    @Data
    public static class BorrowingConfig {
        @Data
        public static class BorrowPaymentConfig {
            private boolean enabled = true;
        }

        @Data
        public static class BorrowPaymentsConfig {
            private BorrowPaymentConfig card = new BorrowPaymentConfig();
            private BorrowPaymentConfig paypal = new BorrowPaymentConfig();
            private BorrowPaymentConfig cash = new BorrowPaymentConfig();
        }

        private BorrowPathConfig deposit = new BorrowPathConfig();
        private BorrowPathConfig verified = new BorrowPathConfig();
        private BorrowPathConfig fee = new BorrowPathConfig();
        private BorrowPaymentsConfig payments = new BorrowPaymentsConfig();
    }

    @Data
    public static class SupportConfig {
        private ContactConfig contact = new ContactConfig();
        private TabConfig faq = new TabConfig();
    }

    @Data
    public static class ContactConfig {
        private boolean enabled = true;
        private String email;
        private String phone;
        private String address;
    }

    @Data
    public static class ConnectConfig {
        private boolean showHero = true;
        private boolean showTrustScore = true;
        private boolean showHyperLocal = true;
        private boolean showPrivacyBox = true;
        private boolean showPrivacyBullets = true;
        private boolean showTermsCheckbox = true;
        private boolean showFooterInfo = true;
        private boolean showDemoSection = true;
    }

    @Data
    public static class HomeConfig {
        private boolean showHeroBadge = true;
        private boolean showHeroTitle = true;
        private boolean showHeroDesc = true;
    }

    private Map<String, TabConfig> tabs = new HashMap<>();
    private Map<String, Object> overview = new HashMap<>();
    private Map<String, Object> profile = new HashMap<>();
    private Map<String, Object> subscription = new HashMap<>();
    private Map<String, Object> security = new HashMap<>();
    private Map<String, Object> privacy = new HashMap<>();
    private Map<String, Object> notifications = new HashMap<>();
    private Map<String, Object> payments = new HashMap<>();
    private Map<String, Object> building = new HashMap<>();
    private Map<String, Object> stats = new HashMap<>();
    private SupportConfig support = new SupportConfig();
    private ConnectConfig connect = new ConnectConfig();
    private HomeConfig home = new HomeConfig();
    private BorrowingConfig borrowing = new BorrowingConfig();
    private ReturnsConfig returns = new ReturnsConfig();
    private Map<String, TabConfig> header = new HashMap<>();
    private Map<String, TabConfig> footer = new HashMap<>();
    private EnableConfig enable = new EnableConfig();
    private ServiceConfig service = new ServiceConfig();
}
