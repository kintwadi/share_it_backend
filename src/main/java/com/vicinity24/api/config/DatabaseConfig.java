package com.vicinity24.api.config;

import com.vicinity24.api.config.tenant.TenantConfigurationProperties;
import com.vicinity24.api.config.tenant.TenantFilter;
import com.vicinity24.api.config.tenant.TenantRegistry;
import com.vicinity24.api.config.tenant.TenantRoutingDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;

@Configuration
@EnableConfigurationProperties(TenantConfigurationProperties.class)
public class DatabaseConfig {
    @Bean
    public TenantRegistry tenantRegistry(Environment env, TenantConfigurationProperties properties) {
        Map<String, TenantRegistry.TenantDefinition> tenants = resolveTenantDefinitions(env, properties);
        String defaultTenantId = resolveDefaultTenantId(properties, tenants);
        if (!tenants.containsKey(defaultTenantId)) {
            throw new IllegalStateException("Default tenant '" + defaultTenantId + "' is not configured");
        }
        return new TenantRegistry(
                firstNonBlank(properties.getHeaderName(), "X-Tenant-ID"),
                defaultTenantId,
                properties.isUseDefaultDatabase(),
                tenants
        );
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantRegistry tenantRegistry) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantFilter(tenantRegistry));
        registration.setName("tenantFilter");
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    @Bean
    @Primary
    public DataSource dataSource(Environment env, TenantRegistry tenantRegistry) {
        Map<Object, Object> targetDataSources = new LinkedHashMap<>();
        tenantRegistry.getTenants().forEach((tenantId, definition) ->
                targetDataSources.put(tenantId, buildTenantDataSource(tenantId, definition, env))
        );

        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource();
        routingDataSource.setManagedTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(targetDataSources.get(tenantRegistry.getDefaultTenantId()));
        routingDataSource.setLenientFallback(false);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Environment env, TenantRegistry tenantRegistry) {
        return props -> {
            if (props.containsKey("hibernate.dialect")) return;
            if (!isBlank(env.getProperty("spring.jpa.database-platform"))) return;

            String dbType = effectiveDbType(env, tenantRegistry);
            String dialect = switch (dbType) {
                case "h2" -> "org.hibernate.dialect.H2Dialect";
                case "sqlite" -> "org.hibernate.community.dialect.SQLiteDialect";
                default -> "org.hibernate.dialect.PostgreSQLDialect";
            };
            props.put("hibernate.dialect", dialect);
        };
    }

    private DataSource buildTenantDataSource(String tenantId, TenantRegistry.TenantDefinition definition, Environment env) {
        String dbType = detectDbType(definition.url());
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("tenant-" + sanitizeTenantId(tenantId) + "-pool");
        cfg.setJdbcUrl(definition.url());
        if (!isBlank(definition.username())) cfg.setUsername(definition.username());
        if (definition.password() != null) cfg.setPassword(definition.password());
        if (!isBlank(definition.driverClassName())) cfg.setDriverClassName(definition.driverClassName());

        Long connectionTimeout = env.getProperty("spring.datasource.hikari.connection-timeout", Long.class);
        Integer maximumPoolSize = env.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        if (connectionTimeout != null) cfg.setConnectionTimeout(connectionTimeout);
        if (maximumPoolSize != null) cfg.setMaximumPoolSize(maximumPoolSize);
        if (maximumPoolSize == null && "sqlite".equals(dbType)) cfg.setMaximumPoolSize(1);
        if ("postgres".equals(dbType) && isSupabasePoolerUrl(definition.url())) {
            cfg.addDataSourceProperty("preferQueryMode", "simple");
            cfg.addDataSourceProperty("preparedStatementCacheQueries", "0");
            cfg.addDataSourceProperty("preparedStatementCacheSizeMiB", "0");
        }

        return new HikariDataSource(cfg);
    }

    private Map<String, TenantRegistry.TenantDefinition> resolveTenantDefinitions(Environment env, TenantConfigurationProperties properties) {
        Map<String, TenantRegistry.TenantDefinition> tenants = new LinkedHashMap<>();
        properties.getConfig().forEach((tenantId, tenantDataSourceProperties) -> {
            if (!hasConfiguredUrl(tenantDataSourceProperties)) {
                return;
            }

            DbConnectionInfo normalized = normalizeDbConnection(
                    tenantDataSourceProperties.getUrl(),
                    tenantDataSourceProperties.getUsername(),
                    tenantDataSourceProperties.getPassword()
            );
            String driverClassName = firstNonBlank(
                    tenantDataSourceProperties.getDriverClassName(),
                    defaultDriverFor(detectDbType(normalized.url()), normalized.url())
            );

            if (isBlank(normalized.url())) {
                throw new IllegalStateException("Tenant '" + tenantId + "' is missing a JDBC URL");
            }
            if (isBlank(driverClassName)) {
                throw new IllegalStateException("Tenant '" + tenantId + "' is missing a driver class name");
            }

            tenants.put(tenantId, new TenantRegistry.TenantDefinition(
                    tenantId,
                    normalized.url(),
                    normalized.username(),
                    normalized.password(),
                    driverClassName
            ));
        });

        if (!tenants.isEmpty()) {
            return tenants;
        }

        String dbType = effectiveDbType(env, null);
        String url = firstNonBlank(
                env.getProperty("spring.datasource.url"),
                defaultUrlFor(dbType)
        );
        String username = firstNonBlank(
                env.getProperty("spring.datasource.username"),
                defaultUsernameFor(dbType)
        );
        String password = firstNonBlank(
                env.getProperty("spring.datasource.password"),
                defaultPasswordFor(dbType)
        );
        String driverClassName = firstNonBlank(
                env.getProperty("spring.datasource.driver-class-name"),
                env.getProperty("spring.datasource.driverClassName"),
                defaultDriverFor(dbType, url)
        );

        DbConnectionInfo normalized = normalizeDbConnection(url, username, password);
        String defaultTenantId = firstNonBlank(properties.getDefaultTenantId(), "default");
        if (isBlank(normalized.url())) {
            throw new IllegalStateException("No default datasource URL configured for tenant '" + defaultTenantId + "'");
        }
        if (isBlank(driverClassName)) {
            throw new IllegalStateException("No driver class name configured for tenant '" + defaultTenantId + "'");
        }
        tenants.put(defaultTenantId, new TenantRegistry.TenantDefinition(
                defaultTenantId,
                normalized.url(),
                normalized.username(),
                normalized.password(),
                driverClassName
        ));
        return tenants;
    }

    private String resolveDefaultTenantId(
            TenantConfigurationProperties properties,
            Map<String, TenantRegistry.TenantDefinition> tenants
    ) {
        String configuredDefault = firstNonBlank(properties.getDefaultTenantId(), "default");
        if (tenants.containsKey(configuredDefault)) {
            return configuredDefault;
        }
        return tenants.keySet().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("At least one tenant must be configured"));
    }

    private boolean hasConfiguredUrl(TenantConfigurationProperties.TenantDataSourceProperties properties) {
        return properties != null && !isBlank(properties.getUrl());
    }

    private String effectiveDbType(Environment env, TenantRegistry tenantRegistry) {
        if (tenantRegistry != null) {
            TenantRegistry.TenantDefinition defaultTenant = tenantRegistry.getTenants().get(tenantRegistry.getDefaultTenantId());
            if (defaultTenant != null && !isBlank(defaultTenant.url())) {
                return detectDbType(defaultTenant.url());
            }
        }

        String configured = firstNonBlank(env.getProperty("enable.db.type"), env.getProperty("DB_TYPE"));
        if (!isBlank(configured)) return configured.trim().toLowerCase(Locale.ROOT);

        String url = env.getProperty("spring.datasource.url");
        return detectDbType(url);
    }

    private String detectDbType(String url) {
        if (isBlank(url)) return "postgres";
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:h2:")) return "h2";
        if (normalized.startsWith("jdbc:sqlite:")) return "sqlite";
        return "postgres";
    }

    private String defaultUrlFor(String dbType) {
        return switch (dbType) {
            case "h2" -> "jdbc:h2:mem:vicinity24;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
            case "sqlite" -> "jdbc:sqlite:./vicinity24.sqlite";
            default -> null;
        };
    }

    private String defaultUsernameFor(String dbType) {
        return switch (dbType) {
            case "h2" -> "sa";
            case "sqlite" -> "";
            default -> null;
        };
    }

    private String defaultPasswordFor(String dbType) {
        return switch (dbType) {
            case "h2" -> "";
            case "sqlite" -> "sqlite_local_password";
            default -> null;
        };
    }

    private String defaultDriverFor(String dbType, String url) {
        if (!isBlank(url)) {
            String normalized = url.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("jdbc:h2:")) return "org.h2.Driver";
            if (normalized.startsWith("jdbc:sqlite:")) return "org.sqlite.JDBC";
            if (normalized.startsWith("jdbc:postgresql:")) return "org.postgresql.Driver";
        }
        return switch (dbType) {
            case "h2" -> "org.h2.Driver";
            case "sqlite" -> "org.sqlite.JDBC";
            default -> "org.postgresql.Driver";
        };
    }

    private DbConnectionInfo normalizeDbConnection(String url, String username, String password) {
        String u = String.valueOf(url == null ? "" : url).trim();
        if (u.isEmpty()) return new DbConnectionInfo(u, username, password);
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.startsWith("jdbc:")) return new DbConnectionInfo(u, username, password);
        if (lower.startsWith("postgresql://") || lower.startsWith("postgres://")) {
            Optional<ParsedUrl> parsed = parsePostgresUrl(u);
            if (parsed.isEmpty()) return new DbConnectionInfo(u, username, password);
            ParsedUrl p = parsed.get();
            String jdbc = "jdbc:postgresql://" + p.host + (p.port != null ? ":" + p.port : "") + "/" + p.database;
            String user = isBlank(username) ? p.username : username;
            String pass = password == null || password.isBlank() ? p.password : password;
            return new DbConnectionInfo(jdbc, user, pass);
        }
        return new DbConnectionInfo(u, username, password);
    }

    private Optional<ParsedUrl> parsePostgresUrl(String raw) {
        String s = String.valueOf(raw == null ? "" : raw).trim();
        int schemeIdx = s.indexOf("://");
        if (schemeIdx < 0) return Optional.empty();
        String afterScheme = s.substring(schemeIdx + 3);
        int at = afterScheme.lastIndexOf('@');
        String authorityAndPath = afterScheme;
        String user = null;
        String pass = null;
        if (at >= 0) {
            String userInfo = afterScheme.substring(0, at);
            authorityAndPath = afterScheme.substring(at + 1);
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                user = userInfo.substring(0, colon);
                pass = userInfo.substring(colon + 1);
            } else {
                user = userInfo;
            }
        }

        int slash = authorityAndPath.indexOf('/');
        if (slash < 0) return Optional.empty();
        String hostPort = authorityAndPath.substring(0, slash);
        String path = authorityAndPath.substring(slash + 1);
        if (isBlank(hostPort) || isBlank(path)) return Optional.empty();

        int q = path.indexOf('?');
        String dbName = (q >= 0 ? path.substring(0, q) : path).trim();
        if (dbName.isEmpty()) return Optional.empty();

        String host = hostPort;
        Integer port = null;
        int colon = hostPort.lastIndexOf(':');
        if (colon > 0 && colon < hostPort.length() - 1 && hostPort.indexOf(']') < 0) {
            host = hostPort.substring(0, colon);
            try {
                port = Integer.parseInt(hostPort.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                port = null;
                host = hostPort;
            }
        }
        host = host.trim();
        if (host.isEmpty()) return Optional.empty();

        return Optional.of(new ParsedUrl(host, port, dbName, user, pass));
    }

    private record ParsedUrl(String host, Integer port, String database, String username, String password) { }

    private record DbConnectionInfo(String url, String username, String password) { }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (!isBlank(value)) return value;
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private boolean isSupabasePoolerUrl(String url) {
        if (isBlank(url)) return false;
        String u = url.trim().toLowerCase(Locale.ROOT);
        return u.contains("pooler.supabase.com") || u.contains("pgbouncer");
    }

    private String sanitizeTenantId(String tenantId) {
        if (isBlank(tenantId)) return "default";
        return tenantId.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
