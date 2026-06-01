package com.nearshare.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BrevoEmailSenderIT {
    @Test
    void sendEmail() throws Exception {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("BREVO_TEST_RUN")));

        Map<String, String> env = loadDotEnvIfPresent(new File(".env"));
        String apiKey = firstNonBlank(System.getenv("STPM_API_KEY"), env.get("STPM_API_KEY"));
        String senderEmail = firstNonBlank(System.getenv("BREVO_SENDER_EMAIL"), env.get("BREVO_SENDER_EMAIL"), System.getenv("MAIL_USERNAME"), env.get("MAIL_USERNAME"));
        String senderName = firstNonBlank(System.getenv("BREVO_SENDER_NAME"), env.get("BREVO_SENDER_NAME"), "ShareIt");
        String toEmail = firstNonBlank(System.getenv("BREVO_TEST_TO"), env.get("BREVO_TEST_TO"), "chskoop@gmail.com");

        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "Missing STPM_API_KEY");
        Assumptions.assumeTrue(senderEmail != null && !senderEmail.isBlank(), "Missing BREVO_SENDER_EMAIL (or MAIL_USERNAME)");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of("name", senderName, "email", senderEmail));
        payload.put("to", List.of(Map.of("name", "Test", "email", toEmail)));
        payload.put("subject", "ShareIt Brevo API test");
        payload.put("htmlContent", "<html><body><h2>Brevo API test</h2><p>If you received this, Brevo REST sending works.</p></body></html>");

        String json = new ObjectMapper().writeValueAsString(payload);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        HttpRequest accountReq = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/account"))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("api-key", apiKey.trim())
                .GET()
                .build();
        HttpResponse<String> accountResp = client.send(accountReq, HttpResponse.BodyHandlers.ofString());
        System.out.println("Brevo /account status: " + accountResp.statusCode());
        if (accountResp.body() != null && !accountResp.body().isBlank()) System.out.println(accountResp.body());
        Assumptions.assumeTrue(accountResp.statusCode() == 200, "Brevo API key rejected by /account");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .timeout(Duration.ofSeconds(20))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", apiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        String body = response.body();
        System.out.println("Brevo status: " + status);
        if (body != null && !body.isBlank()) System.out.println(body);
        Assumptions.assumeTrue(status == 201, "Brevo send failed with status " + status);
    }

    private static Map<String, String> loadDotEnvIfPresent(File file) {
        Map<String, String> out = new LinkedHashMap<>();
        if (file == null || !file.exists() || !file.isFile()) return out;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) continue;
                int eq = s.indexOf('=');
                if (eq <= 0) continue;
                String k = s.substring(0, eq).trim();
                String v = s.substring(eq + 1).trim();
                if (!k.isEmpty()) out.put(k, v);
            }
        } catch (Exception ignored) {
            return out;
        }
        return out;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) return v.trim();
        }
        return null;
    }
}
