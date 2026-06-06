package com.vicinity24.api.config;

import com.vicinity24.api.model.enums.AvailabilityStatus;
import com.vicinity24.api.model.enums.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PostgresCheckConstraintUpdater implements CommandLineRunner {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (!isPostgres()) {
            return;
        }

        try {
            jdbcTemplate.update("UPDATE listings SET status = 'PARTNER_INACTIVE' WHERE partner_id IS NOT NULL AND status = 'PARTNER_PENDING_APPROVAL'");
            jdbcTemplate.update("UPDATE listings SET status = 'PARTNER_ACTIVE' WHERE partner_id IS NOT NULL AND status = 'APPROVED'");
        } catch (Exception ignored) {
        }

        updateEnumCheckConstraint(
                "listings",
                "listings_status_check",
                "status",
                Arrays.stream(AvailabilityStatus.values()).map(Enum::name).collect(Collectors.toList())
        );

        updateEnumCheckConstraint(
                "return_sessions",
                "return_sessions_status_check",
                "status",
                Arrays.stream(ReturnStatus.values()).map(Enum::name).collect(Collectors.toList())
        );
    }

    private boolean isPostgres() {
        try (Connection c = dataSource.getConnection()) {
            String product = c.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase(Locale.ROOT).contains("postgres");
        } catch (Exception e) {
            return false;
        }
    }

    private void updateEnumCheckConstraint(String table, String constraint, String column, java.util.List<String> allowed) {
        try {
            jdbcTemplate.execute("ALTER TABLE IF EXISTS " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
        } catch (Exception ignored) {
        }

        String inList = allowed.stream()
                .map(v -> "'" + v.replace("'", "''") + "'")
                .collect(Collectors.joining(", "));
        String addSql = "ALTER TABLE " + table + " ADD CONSTRAINT " + constraint + " CHECK (" + column + " IN (" + inList + "))";

        try {
            jdbcTemplate.execute(addSql);
        } catch (Exception ignored) {
        }
    }
}
