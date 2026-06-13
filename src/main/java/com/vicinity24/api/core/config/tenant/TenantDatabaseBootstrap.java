package com.vicinity24.api.core.config.tenant;

import com.vicinity24.api.core.config.PostgresCheckConstraintUpdater;
import com.vicinity24.api.core.model.Category;
import com.vicinity24.api.core.model.ExchangeLocation;
import com.vicinity24.api.core.model.Listing;
import com.vicinity24.api.core.model.Message;
import com.vicinity24.api.core.model.Review;
import com.vicinity24.api.core.model.Subscription;
import com.vicinity24.api.core.model.SubscriptionVerificationCode;
import com.vicinity24.api.core.model.Transaction;
import com.vicinity24.api.core.model.User;
import com.vicinity24.api.core.model.Device;
import com.vicinity24.api.core.model.EmailVerificationToken;
import com.vicinity24.api.core.model.embeddable.Location;
import com.vicinity24.api.core.recommendation.model.MahoutIdMapping;
import com.vicinity24.api.core.model.PasswordResetToken;
import com.vicinity24.api.core.recommendation.service.RecommendationSeederService;
import com.vicinity24.api.core.recommendation.service.RecommendationService;
import com.vicinity24.api.core.repository.CategoryRepository;
import com.vicinity24.api.core.repository.ExchangeLocationRepository;
import com.vicinity24.api.core.service.MockDataSeederService;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class TenantDatabaseBootstrap implements ApplicationRunner {
    private static final String ENTITY_SCAN_PACKAGE = "com.vicinity24.api";
    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int REF_LEN = 8;
    private static final List<Class<?>> LEGACY_COMPATIBILITY_ENTITIES = List.of(
            Category.class,
            User.class,
            ExchangeLocation.class,
            Listing.class,
            Subscription.class,
            Review.class,
            Message.class,
            Transaction.class,
            Device.class,
            MahoutIdMapping.class,
            SubscriptionVerificationCode.class,
            PasswordResetToken.class,
            EmailVerificationToken.class
    );

    private final DataSource dataSource;
    private final TenantRegistry tenantRegistry;
    private final JpaProperties jpaProperties;
    private final Environment environment;
    private final ExchangeLocationRepository exchangeLocationRepository;
    private final CategoryRepository categoryRepository;
    private final MockDataSeederService mockDataSeederService;
    private final RecommendationSeederService recommendationSeederService;
    private final RecommendationService recommendationService;
    private final PostgresCheckConstraintUpdater postgresCheckConstraintUpdater;
    private final SecureRandom random = new SecureRandom();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        TenantRoutingDataSource routingDataSource = extractRoutingDataSource();
        boolean seedingEnabled = environment.getProperty("seeding.enabled", Boolean.class, false);

        for (String tenantId : tenantRegistry.getTenants().keySet()) {
            DataSource tenantDataSource = routingDataSource.getManagedDataSources().get(tenantId);
            if (tenantDataSource == null) {
                throw new IllegalStateException("No datasource registered for tenant '" + tenantId + "'");
            }

            initializeSchema(tenantId, tenantDataSource);
            applyCompatibilityMigrations(tenantId, tenantDataSource);
            runInsideTenant(tenantId, () -> initializeTenantData(tenantId, seedingEnabled));
        }
    }

    private TenantRoutingDataSource extractRoutingDataSource() {
        if (dataSource instanceof TenantRoutingDataSource tenantRoutingDataSource) {
            return tenantRoutingDataSource;
        }
        throw new IllegalStateException("Primary datasource is not a TenantRoutingDataSource");
    }

    private void initializeTenantData(String tenantId, boolean seedingEnabled) throws Exception {
        postgresCheckConstraintUpdater.run();
        if (!seedingEnabled) {
            log.info("Tenant {} schema initialized. Seed data skipped because seeding.enabled=false", tenantId);
            return;
        }

        seedPickupLocations();
        seedCategories();
        String seedResult = mockDataSeederService.seedMockData();
        recommendationSeederService.seedTransactions();
        recommendationService.rebuildModel();
        log.info("Tenant {} initialized with seed data: {}", tenantId, seedResult);
    }

    private void applyCompatibilityMigrations(String tenantId, DataSource tenantDataSource) {
        try (var connection = tenantDataSource.getConnection()) {
            for (Class<?> entityClass : LEGACY_COMPATIBILITY_ENTITIES) {
                migrateEntityColumns(connection, tenantId, entityClass);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply compatibility migrations for tenant '" + tenantId + "'", ex);
        }
    }

    private void initializeSchema(String tenantId, DataSource tenantDataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(tenantDataSource);
        entityManagerFactory.setPackagesToScan(ENTITY_SCAN_PACKAGE);
        entityManagerFactory.setPersistenceUnitName("tenant-bootstrap-" + sanitizeTenantId(tenantId));
        entityManagerFactory.setJpaVendorAdapter(buildVendorAdapter());
        entityManagerFactory.setJpaPropertyMap(buildJpaPropertyMap(tenantDataSource));

        try {
            entityManagerFactory.afterPropertiesSet();
            if (entityManagerFactory.getObject() != null) {
                entityManagerFactory.getObject().close();
            }
            log.info("Tenant {} schema is ready", tenantId);
        } finally {
            entityManagerFactory.destroy();
        }
    }

    private HibernateJpaVendorAdapter buildVendorAdapter() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(environment.getProperty("spring.jpa.show-sql", Boolean.class, false));
        String databasePlatform = environment.getProperty("spring.jpa.database-platform");
        if (hasText(databasePlatform)) {
            vendorAdapter.setDatabasePlatform(databasePlatform);
        }
        return vendorAdapter;
    }

    private Map<String, Object> buildJpaPropertyMap(DataSource tenantDataSource) {
        Map<String, Object> properties = new LinkedHashMap<>(jpaProperties.getProperties());
        properties.put("hibernate.hbm2ddl.auto", environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        copyIfPresent(properties, "hibernate.physical_naming_strategy", environment.getProperty("spring.jpa.hibernate.naming.physical-strategy"));
        copyIfPresent(properties, "hibernate.implicit_naming_strategy", environment.getProperty("spring.jpa.hibernate.naming.implicit-strategy"));
        copyIfPresent(properties, "hibernate.dialect", environment.getProperty("spring.jpa.database-platform"));
        properties.putIfAbsent("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.putIfAbsent("hibernate.dialect", detectDialect(tenantDataSource));
        return properties;
    }

    private void copyIfPresent(Map<String, Object> properties, String key, String value) {
        if (hasText(value)) {
            properties.put(key, value);
        }
    }

    private String detectDialect(DataSource tenantDataSource) {
        try (var connection = tenantDataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            if (productName == null) {
                return "org.hibernate.dialect.PostgreSQLDialect";
            }
            String normalized = productName.trim().toLowerCase();
            if (normalized.contains("sqlite")) {
                return "org.hibernate.community.dialect.SQLiteDialect";
            }
            if (normalized.contains("h2")) {
                return "org.hibernate.dialect.H2Dialect";
            }
            return "org.hibernate.dialect.PostgreSQLDialect";
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to detect database dialect", ex);
        }
    }

    private boolean hasColumn(java.sql.Connection connection, String tableName, String columnName) throws Exception {
        var metadata = connection.getMetaData();
        try (var columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (var columns = metadata.getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return columns.next();
        }
    }

    private void renameLegacyColumnIfNeeded(
            java.sql.Connection connection,
            String tenantId,
            String tableName,
            String legacyColumn,
            String expectedColumn
    ) throws Exception {
        if (!hasColumn(connection, tableName, legacyColumn)) {
            return;
        }

        try (var statement = connection.createStatement()) {
            if (hasColumn(connection, tableName, expectedColumn)) {
                statement.executeUpdate(
                        "UPDATE " + tableName + " SET " + expectedColumn + " = " + legacyColumn +
                                " WHERE " + expectedColumn + " IS NULL AND " + legacyColumn + " IS NOT NULL"
                );
                statement.executeUpdate("ALTER TABLE " + tableName + " DROP COLUMN " + legacyColumn);
                log.info("Tenant {} schema removed legacy column: {}.{}", tenantId, tableName, legacyColumn);
                return;
            }

            statement.executeUpdate("ALTER TABLE " + tableName + " RENAME COLUMN " + legacyColumn + " TO " + expectedColumn);
            log.info("Tenant {} schema updated: {}.{} -> {}", tenantId, tableName, legacyColumn, expectedColumn);
        }
    }

    private void migrateEntityColumns(java.sql.Connection connection, String tenantId, Class<?> entityClass) throws Exception {
        Table table = entityClass.getAnnotation(Table.class);
        if (table == null || !hasText(table.name())) {
            return;
        }

        String tableName = table.name();
        for (Field field : entityClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(Transient.class)) {
                continue;
            }
            if (field.isAnnotationPresent(Embedded.class)) {
                continue;
            }

            String expectedColumn = resolveExpectedColumnName(field);
            if (!hasText(expectedColumn) || !expectedColumn.contains("_")) {
                continue;
            }

            String legacyColumn = resolveLegacyColumnName(field, expectedColumn);
            if (hasText(legacyColumn) && !legacyColumn.equals(expectedColumn)) {
                renameLegacyColumnIfNeeded(connection, tenantId, tableName, legacyColumn, expectedColumn);
            }
        }
    }

    private String resolveExpectedColumnName(Field field) {
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        if (joinColumn != null && hasText(joinColumn.name())) {
            return joinColumn.name();
        }

        Column column = field.getAnnotation(Column.class);
        if (column != null && hasText(column.name())) {
            return column.name();
        }

        if (field.getType().isAnnotationPresent(jakarta.persistence.Entity.class)) {
            return toSnakeCase(field.getName()) + "_id";
        }

        return toSnakeCase(field.getName());
    }

    private String resolveLegacyColumnName(Field field, String expectedColumn) {
        if (expectedColumn.endsWith("_id") && field.getType().isAnnotationPresent(jakarta.persistence.Entity.class)) {
            return field.getName().toLowerCase() + "id";
        }
        return field.getName().toLowerCase();
    }

    private String toSnakeCase(String value) {
        if (!hasText(value)) {
            return value;
        }

        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (builder.length() > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(ch));
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private void runInsideTenant(String tenantId, TenantCallback callback) throws Exception {
        try {
            TenantContextHolder.setTenantId(tenantId);
            callback.run();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private void seedPickupLocations() {
        if (exchangeLocationRepository.count() > 0) {
            return;
        }

        exchangeLocationRepository.save(ExchangeLocation.builder()
                .id(java.util.UUID.randomUUID())
                .referenceId(generateUniqueReferenceId())
                .name("Concierge (building front desk)")
                .address("Frankfurterstr 2455, Frankfurt")
                .streetAddress("Frankfurterstr 2455")
                .city("Frankfurt")
                .postalCode("")
                .country("DE")
                .location(Location.builder().lat(50.1109).lng(8.6821).build())
                .operatingTimeFrom("08:00")
                .operatingTimeTo("20:00")
                .active(true)
                .build());

        exchangeLocationRepository.save(ExchangeLocation.builder()
                .id(java.util.UUID.randomUUID())
                .referenceId(generateUniqueReferenceId())
                .name("Local bakery partner")
                .address("Leipzigerstr 102, Frankfurt")
                .streetAddress("Leipzigerstr 102")
                .city("Frankfurt")
                .postalCode("")
                .country("DE")
                .location(Location.builder().lat(50.1180).lng(8.6512).build())
                .operatingTimeFrom("07:00")
                .operatingTimeTo("18:00")
                .active(true)
                .build());

        exchangeLocationRepository.save(ExchangeLocation.builder()
                .id(java.util.UUID.randomUUID())
                .referenceId(generateUniqueReferenceId())
                .name("Public meetup spot")
                .address("Konstablerwache 5, Frankfurt")
                .streetAddress("Konstablerwache 5")
                .city("Frankfurt")
                .postalCode("")
                .country("DE")
                .location(Location.builder().lat(50.1147).lng(8.6873).build())
                .active(true)
                .build());
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Tools").labelKey("category.tools").active(true).build());
        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Gardening").labelKey("category.gardening").active(true).build());
        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Kitchen").labelKey("category.kitchen").active(true).build());
        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Outdoors").labelKey("category.outdoors").active(true).build());
        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Music").labelKey("category.music").active(true).build());
        categoryRepository.save(Category.builder().id(java.util.UUID.randomUUID()).code("Misc").labelKey("category.misc").active(true).build());
    }

    private String generateUniqueReferenceId() {
        for (int attempt = 0; attempt < 50; attempt++) {
            String ref = randomRef();
            if (!exchangeLocationRepository.existsByReferenceId(ref)) {
                return ref;
            }
        }
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String randomRef() {
        StringBuilder builder = new StringBuilder(REF_LEN);
        for (int i = 0; i < REF_LEN; i++) {
            builder.append(REF_CHARS.charAt(random.nextInt(REF_CHARS.length())));
        }
        return builder.toString();
    }

    private String sanitizeTenantId(String tenantId) {
        return Objects.requireNonNullElse(tenantId, "default").replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface TenantCallback {
        void run() throws Exception;
    }
}
