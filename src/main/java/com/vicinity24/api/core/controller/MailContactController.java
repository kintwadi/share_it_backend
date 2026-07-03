package com.vicinity24.api.core.controller;

import com.vicinity24.api.core.dto.MailContactRequest;
import com.vicinity24.api.core.service.EmailService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MailContactController {

    private final EmailService emailService;
    private final String publicHeaderName;
    private final String publicHeaderValue;
    private final String contactRecipient;

    public MailContactController(
            EmailService emailService,
            @Value("${mail.contact.public-header-name:X-Public-Origin}") String publicHeaderName,
            @Value("${mail.contact.public-key:vicinity24.com}") String publicHeaderValue,
            @Value("${mail.contact.to:${spring.mail.from:noreply@vicinity24.com}}") String contactRecipient
    ) {
        this.emailService = emailService;
        this.publicHeaderName = publicHeaderName;
        this.publicHeaderValue = publicHeaderValue;
        this.contactRecipient = contactRecipient;
    }

    @PostMapping("/mail-contact-request")
    public ResponseEntity<Map<String, String>> submitContactRequest(
            HttpServletRequest httpRequest,
            @Valid @RequestBody MailContactRequest request
    ) {
        String headerValue = httpRequest.getHeader(publicHeaderName);
        if (headerValue == null || !publicHeaderValue.equals(headerValue.trim())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "invalid_public_key"));
        }

        boolean sent = emailService.sendContactInquiryEmail(contactRecipient, request);
        if (!sent) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "email_send_failed"));
        }

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
