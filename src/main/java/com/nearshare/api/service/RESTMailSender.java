package com.nearshare.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RESTMailSender {
    private static final Logger logger = LoggerFactory.getLogger(RESTMailSender.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${brevo.api.key:}")
    private String apiKey;

    @Value("${brevo.api.url:https://api.brevo.com/v3/smtp/email}")
    private String apiUrl;

    @Value("${brevo.sender.email:}")
    private String senderEmail;

    @Value("${brevo.sender.name:ShareIt}")
    private String senderName;

    public RESTMailSender(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isConfigured() {
        String k = String.valueOf(apiKey == null ? "" : apiKey).trim();
        return !k.isEmpty();
    }

    public void sendTransactionalEmail(String toEmail, String toName, String subject, String htmlContent) {
        String k = String.valueOf(apiKey == null ? "" : apiKey).trim();
        if (k.isEmpty()) throw new IllegalStateException("brevo_api_key_missing");
        String lower = k.toLowerCase();
        if (lower.startsWith("xsmtpsib-")) {
            throw new IllegalStateException("brevo_api_key_required_not_smtp_key");
        }
        String to = String.valueOf(toEmail == null ? "" : toEmail).trim();
        if (to.isEmpty()) throw new IllegalArgumentException("invalid_to_email");
        String from = String.valueOf(senderEmail == null ? "" : senderEmail).trim();
        if (from.isEmpty()) throw new IllegalStateException("brevo_sender_email_missing");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sender", Map.of("name", String.valueOf(senderName == null ? "" : senderName), "email", from));
        payload.put("to", List.of(Map.of(
                "name", String.valueOf(toName == null ? "" : toName).trim(),
                "email", to
        )));
        payload.put("subject", String.valueOf(subject == null ? "" : subject));
        payload.put("htmlContent", String.valueOf(htmlContent == null ? "" : htmlContent));

        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("api-key", k)
                    .header("x-api-key", k)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 201) {
                logger.info("Brevo email queued: to={}", to);
                return;
            }

            if (status == 401 && apiUrl.contains("api.brevo.com")) {
                String fallbackUrl = apiUrl.replace("api.brevo.com", "api.sendinblue.com");
                HttpRequest retry = HttpRequest.newBuilder()
                        .uri(URI.create(fallbackUrl))
                        .timeout(Duration.ofSeconds(20))
                        .header("accept", "application/json")
                        .header("content-type", "application/json")
                        .header("api-key", k)
                        .header("x-api-key", k)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                HttpResponse<String> retryResp = httpClient.send(retry, HttpResponse.BodyHandlers.ofString());
                int retryStatus = retryResp.statusCode();
                if (retryStatus == 201) {
                    logger.info("Brevo email queued (fallback domain): to={}", to);
                    return;
                }
                String retryBody = retryResp.body();
                logger.error("Brevo email failed (fallback domain): status={} to={} body={}", retryStatus, to, retryBody);
                throw new RuntimeException("brevo_send_failed_" + retryStatus);
            }

            String body = response.body();
            logger.error("Brevo email failed: status={} to={} body={}", status, to, body);
            throw new RuntimeException("brevo_send_failed_" + status);
        } catch (Exception e) {
            throw new RuntimeException("brevo_send_failed", e);
        }
    }
}
