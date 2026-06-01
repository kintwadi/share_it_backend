package com.nearshare.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final RESTMailSender restMailSender;
    
    @Value("${spring.mail.from:noreply@nearshare.com}")
    private String fromEmail;
    
    public EmailService(JavaMailSender mailSender, RESTMailSender restMailSender) {
        this.mailSender = mailSender;
        this.restMailSender = restMailSender;
    }
    
    public void sendPasswordResetEmail(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Code - NearShare");
            
            String htmlContent = buildPasswordResetEmail(code);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Password reset email sent to: {}", toEmail);
            
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
        }
    }
    
    public void sendSubscriptionVerificationEmail(String toEmail, String recipientName, String code, String planType, String language) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(buildSubscriptionVerificationSubject(planType, language));
            
            String htmlContent = buildSubscriptionVerificationEmail(recipientName, code, planType, language);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            logger.info("Subscription verification email sent to: {}", toEmail);
            
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send subscription verification email to: {}", toEmail, e);
        }
    }

    public void sendSignupEmailVerificationEmail(String toEmail, String recipientName, String code, String language) {
        String subject = buildSignupVerificationSubject(language);
        String htmlContent = buildSignupVerificationEmail(recipientName, code, language);
        if (restMailSender != null && restMailSender.isConfigured()) {
            try {
                restMailSender.sendTransactionalEmail(toEmail, recipientName, subject, htmlContent);
                logger.info("Signup email verification email sent via Brevo API to: {}", toEmail);
                return;
            } catch (Exception e) {
                logger.error("Brevo API send failed for signup email verification to: {}", toEmail, e);
            }
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Signup email verification email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send signup email verification email to: {}", toEmail, e);
        }
    }

    public void sendReturnRatingEmail(String toEmail, String recipientName, String otherPartyName, String listingTitle, String ratingLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Rate your recent handover");

            String htmlContent = buildReturnRatingEmail(recipientName, otherPartyName, listingTitle, ratingLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("Return rating email sent to: {}", toEmail);
        } catch (MessagingException | MailException e) {
            logger.error("Failed to send return rating email to: {}", toEmail, e);
        }
    }
    
    private String buildPasswordResetEmail(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4F46E5; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 5px; }
                    .code { 
                        font-size: 32px; 
                        font-weight: bold; 
                        letter-spacing: 5px; 
                        text-align: center; 
                        margin: 20px 0; 
                        color: #4F46E5;
                    }
                    .footer { margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>NearShare Password Reset</h2>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>You requested to reset your password. Use the verification code below to proceed:</p>
                        
                        <div class="code">%s</div>
                        
                        <p>This code will expire in 15 minutes for security reasons.</p>
                        
                        <p>If you didn't request this reset, please ignore this email or contact support if you have concerns.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from NearShare. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
    
    private String buildSubscriptionVerificationSubject(String planType, String language) {
        String lang = language != null ? language.toLowerCase() : "en";
        String plan = planType != null ? planType.toLowerCase() : "plus";
        if ("pt".equals(lang)) {
            return switch (plan) {
                case "starter" -> "Verifique seu e-mail para o NeighborShare Starter";
                case "pro" -> "Verifique seu e-mail para o NeighborShare Pro";
                default -> "Confirme seu teste do NeighborShare Plus";
            };
        }
        if ("de".equals(lang)) {
            return switch (plan) {
                case "starter" -> "Bestätige deine E-Mail für NeighborShare Starter";
                case "pro" -> "Bestätige deine E-Mail für NeighborShare Pro";
                default -> "Bestätige deinen NeighborShare Plus-Test";
            };
        }
        return switch (plan) {
            case "starter" -> "Verify your email for NeighborShare Starter";
            case "pro" -> "Verify your email for NeighborShare Pro";
            default -> "Confirm your NeighborShare Plus trial";
        };
    }

    private String buildSubscriptionVerificationEmail(String recipientName, String code, String planType, String language) {
        String safeRecipient = recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "there";
        String lang = language != null ? language.toLowerCase() : "en";
        String plan = planType != null ? planType.toLowerCase() : "plus";
        String headerTitle;
        String greeting;
        String intro;
        String validity;
        String ignore;

        if ("pt".equals(lang)) {
            headerTitle = switch (plan) {
                case "starter" -> "Verifique seu e-mail para o NeighborShare Starter";
                case "pro" -> "Verifique seu e-mail para o NeighborShare Pro";
                default -> "Verifique seu e-mail para o NeighborShare Plus";
            };
            greeting = "Olá %s,".formatted(safeRecipient);
            intro = switch (plan) {
                case "starter" -> "Para ativar o plano Starter, insira o código de verificação abaixo no app:";
                case "pro" -> "Para ativar o plano Pro, insira o código de verificação abaixo no app:";
                default -> "Para iniciar o teste de 14 dias do plano Plus, insira o código de verificação abaixo no app:";
            };
            validity = "Este código é válido por pouco tempo por motivos de segurança.";
            ignore = "Se você não solicitou isso, pode ignorar este e-mail.";
        } else if ("de".equals(lang)) {
            headerTitle = switch (plan) {
                case "starter" -> "Bestätige deine E-Mail für NeighborShare Starter";
                case "pro" -> "Bestätige deine E-Mail für NeighborShare Pro";
                default -> "Bestätige deine E-Mail für NeighborShare Plus";
            };
            greeting = "Hallo %s,".formatted(safeRecipient);
            intro = switch (plan) {
                case "starter" -> "Um deinen Starter-Plan zu aktivieren, gib den Bestätigungscode unten in der App ein:";
                case "pro" -> "Um deinen Pro-Plan zu aktivieren, gib den Bestätigungscode unten in der App ein:";
                default -> "Um deinen 14-tägigen Plus-Test zu starten, gib den Bestätigungscode unten in der App ein:";
            };
            validity = "Dieser Code ist aus Sicherheitsgründen nur für kurze Zeit gültig.";
            ignore = "Wenn du das nicht angefordert hast, kannst du diese E-Mail ignorieren.";
        } else {
            headerTitle = switch (plan) {
                case "starter" -> "Verify your email for NeighborShare Starter";
                case "pro" -> "Verify your email for NeighborShare Pro";
                default -> "Verify your email for NeighborShare Plus";
            };
            greeting = "Hello %s,".formatted(safeRecipient);
            intro = switch (plan) {
                case "starter" -> "To activate your Starter plan, enter the verification code below in the app:";
                case "pro" -> "To activate your Pro plan, enter the verification code below in the app:";
                default -> "To start your 14-day NeighborShare Plus trial, enter the verification code below in the app:";
            };
            validity = "This code is valid for a short time for security reasons.";
            ignore = "If you did not request this, you can ignore this email.";
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #0F766E; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 5px; }
                    .code { 
                        font-size: 32px; 
                        font-weight: bold; 
                        letter-spacing: 5px; 
                        text-align: center; 
                        margin: 20px 0; 
                        color: #0F766E;
                    }
                    .footer { margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>%s</h2>
                    </div>
                    <div class="content">
                        <p>%s</p>
                        <p>%s</p>
                        
                        <div class="code">%s</div>
                        
                        <p>%s</p>
                        
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from NeighborShare. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(headerTitle, greeting, intro, code, validity, ignore);
    }

    private String buildSignupVerificationSubject(String language) {
        String lang = language != null ? language.toLowerCase() : "en";
        if ("pt".equals(lang)) return "Confirme o seu e-mail - ShareIt";
        if ("de".equals(lang)) return "Bestätige deine E-Mail - ShareIt";
        return "Verify your email - ShareIt";
    }

    private String buildSignupVerificationEmail(String recipientName, String code, String language) {
        String safeRecipient = recipientName != null && !recipientName.isBlank() ? recipientName.trim() : "there";
        String lang = language != null ? language.toLowerCase() : "en";
        String headerTitle;
        String greeting;
        String intro;
        String validity;
        String ignore;

        if ("pt".equals(lang)) {
            headerTitle = "Confirme o seu e-mail";
            greeting = "Olá %s,".formatted(safeRecipient);
            intro = "Para concluir o registo, introduza o código de verificação abaixo na aplicação:";
            validity = "Este código é válido por pouco tempo por motivos de segurança.";
            ignore = "Se não solicitou isto, pode ignorar este e-mail.";
        } else if ("de".equals(lang)) {
            headerTitle = "Bestätige deine E-Mail";
            greeting = "Hallo %s,".formatted(safeRecipient);
            intro = "Um die Registrierung abzuschließen, gib den Bestätigungscode unten in der App ein:";
            validity = "Dieser Code ist aus Sicherheitsgründen nur für kurze Zeit gültig.";
            ignore = "Wenn du das nicht angefordert hast, kannst du diese E-Mail ignorieren.";
        } else {
            headerTitle = "Verify your email";
            greeting = "Hello %s,".formatted(safeRecipient);
            intro = "To complete your sign up, enter the verification code below in the app:";
            validity = "This code is valid for a short time for security reasons.";
            ignore = "If you did not request this, you can ignore this email.";
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #111827; color: white; padding: 20px; text-align: center; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 5px; }
                    .code {
                        font-size: 32px;
                        font-weight: bold;
                        letter-spacing: 6px;
                        text-align: center;
                        margin: 20px 0;
                        color: #111827;
                    }
                    .footer { margin-top: 20px; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>%s</h2>
                    </div>
                    <div class="content">
                        <p>%s</p>
                        <p>%s</p>
                        <div class="code">%s</div>
                        <p>%s</p>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from ShareIt. Please do not reply to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(headerTitle, greeting, intro, code, validity, ignore);
    }

    private String buildReturnRatingEmail(String recipientName, String otherPartyName, String listingTitle, String ratingLink) {
        String safeRecipient = recipientName != null && !recipientName.isBlank() ? recipientName : "there";
        String safeOther = otherPartyName != null && !otherPartyName.isBlank() ? otherPartyName : "your neighbor";
        String safeTitle = listingTitle != null && !listingTitle.isBlank() ? listingTitle : "your listing";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 640px; margin: 0 auto; padding: 20px; }
                    .header { background: #111827; color: white; padding: 18px 20px; text-align: left; border-radius: 10px 10px 0 0; }
                    .content { background: #f9fafb; padding: 22px 20px; border-radius: 0 0 10px 10px; border: 1px solid #e5e7eb; border-top: 0; }
                    .btn { display: inline-block; background: #2563eb; color: white !important; padding: 12px 16px; border-radius: 10px; text-decoration: none; font-weight: bold; }
                    .meta { font-size: 12px; color: #6b7280; margin-top: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2 style="margin:0;">Thanks for using share it</h2>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>Your return for <strong>%s</strong> has been completed.</p>
                        <p>Please take a moment to rate <strong>%s</strong> based on your experience.</p>
                        <p><a class="btn" href="%s" target="_blank" rel="noopener noreferrer">Rate now</a></p>
                        <p class="meta">This link is personal to your account. If you did not complete this handover, you can ignore this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(safeRecipient, safeTitle, safeOther, ratingLink);
    }
}
