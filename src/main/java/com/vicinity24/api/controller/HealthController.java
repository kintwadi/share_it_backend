package com.vicinity24.api.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final ObjectProvider<DataSource> dataSourceProvider;

    @Value("${spring.application.name:}")
    private String applicationName;

    @Value("${application.version:}")
    private String applicationVersion;

    @Value("${api.version:v1}")
    private String apiVersion;

    public HealthController(ObjectProvider<DataSource> dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, Object> db = new LinkedHashMap<>();

        body.put("status", "UP");
        body.put("service", String.valueOf(applicationName == null ? "" : applicationName));
        body.put("version", String.valueOf(applicationVersion == null ? "" : applicationVersion));
        body.put("apiVersion", String.valueOf(apiVersion == null ? "" : apiVersion));
        body.put("database", db);

        DataSource ds = dataSourceProvider.getIfAvailable();
        if (ds == null) {
            db.put("connected", false);
            db.put("error", "No DataSource configured");
            body.put("status", "DOWN");
            return ResponseEntity.ok(body);
        }

        try (Connection connection = ds.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            db.put("connected", true);
            db.put("product", meta.getDatabaseProductName());
            db.put("productVersion", meta.getDatabaseProductVersion());
            db.put("driver", meta.getDriverName());
            db.put("driverVersion", meta.getDriverVersion());
            db.put("url", sanitizeJdbcUrl(meta.getURL()));
        } catch (Exception e) {
            db.put("connected", false);
            db.put("error", e.getClass().getSimpleName());
            db.put("message", safeMessage(e.getMessage()));
            body.put("status", "DOWN");
        }

        return ResponseEntity.ok(body);
    }

    private static String safeMessage(String message) {
        String m = String.valueOf(message == null ? "" : message).trim();
        if (m.length() > 220) m = m.substring(0, 220);
        return m;
    }

    private static String sanitizeJdbcUrl(String url) {
        String u = String.valueOf(url == null ? "" : url).trim();
        if (u.isEmpty()) return "";
        int q = u.indexOf('?');
        if (q < 0) return u;
        String base = u.substring(0, q);
        String query = u.substring(q + 1);
        StringBuilder sb = new StringBuilder();
        for (String part : query.split("&")) {
            String p = part == null ? "" : part.trim();
            if (p.isEmpty()) continue;
            String key = p.split("=", 2)[0].trim().toLowerCase();
            if (key.equals("password") || key.equals("passwd") || key.equals("pwd") || key.equals("user") || key.equals("username")) {
                continue;
            }
            if (sb.length() > 0) sb.append("&");
            sb.append(p);
        }
        if (sb.length() == 0) return base;
        return base + "?" + sb;
    }
}
