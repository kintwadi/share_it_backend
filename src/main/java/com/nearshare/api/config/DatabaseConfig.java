package com.nearshare.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Locale;

@Configuration
public class DatabaseConfig {
    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String dbType = effectiveDbType(env);

        String url = firstNonBlank(
                env.getProperty("spring.datasource.url"),
                env.getProperty("DB_URL"),
                defaultUrlFor(dbType)
        );
        String username = firstNonBlank(
                env.getProperty("spring.datasource.username"),
                env.getProperty("DB_USERNAME"),
                defaultUsernameFor(dbType)
        );
        String password = firstNonBlank(
                env.getProperty("spring.datasource.password"),
                env.getProperty("DB_PASSWORD"),
                defaultPasswordFor(dbType)
        );
        String driverClassName = firstNonBlank(
                env.getProperty("spring.datasource.driverClassName"),
                env.getProperty("DB_DRIVER"),
                defaultDriverFor(dbType, url)
        );

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        if (!isBlank(username)) cfg.setUsername(username);
        if (password != null) cfg.setPassword(password);
        if (!isBlank(driverClassName)) cfg.setDriverClassName(driverClassName);

        Long connectionTimeout = env.getProperty("spring.datasource.hikari.connection-timeout", Long.class);
        Integer maximumPoolSize = env.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class);
        if (connectionTimeout != null) cfg.setConnectionTimeout(connectionTimeout);
        if (maximumPoolSize != null) cfg.setMaximumPoolSize(maximumPoolSize);
        if (maximumPoolSize == null && "sqlite".equals(dbType)) cfg.setMaximumPoolSize(1);

        return new HikariDataSource(cfg);
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(Environment env) {
        return props -> {
            if (props.containsKey("hibernate.dialect")) return;
            if (!isBlank(env.getProperty("spring.jpa.database-platform"))) return;

            String dbType = effectiveDbType(env);
            String dialect = switch (dbType) {
                case "h2" -> "org.hibernate.dialect.H2Dialect";
                case "sqlite" -> "org.hibernate.community.dialect.SQLiteDialect";
                default -> "org.hibernate.dialect.PostgreSQLDialect";
            };
            props.put("hibernate.dialect", dialect);
        };
    }

    private String effectiveDbType(Environment env) {
        String configured = firstNonBlank(env.getProperty("enable.db.type"), env.getProperty("DB_TYPE"));
        if (!isBlank(configured)) return configured.trim().toLowerCase(Locale.ROOT);

        String url = firstNonBlank(env.getProperty("spring.datasource.url"), env.getProperty("DB_URL"));
        if (url == null) return "postgres";
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:h2:")) return "h2";
        if (normalized.startsWith("jdbc:sqlite:")) return "sqlite";
        return "postgres";
    }

    private String defaultUrlFor(String dbType) {
        return switch (dbType) {
            case "h2" -> "jdbc:h2:mem:nearshare;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
            case "sqlite" -> "jdbc:sqlite:./nearshare.sqlite";
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
}

