package com.shiksha.erp.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:notifications@shikshaerp.com}")
    private String mailFrom;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Async
    public void sendFeeOverdueEmail(String toEmail, String studentName, String monthYear, BigDecimal amountDue, LocalDate dueDate) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            Context context = new Context();
            context.setVariable("studentName", studentName);
            context.setVariable("monthYear", monthYear);
            context.setVariable("amountDue", amountDue != null ? amountDue.toString() : "0.00");
            context.setVariable("dueDate", dueDate != null ? dueDate.format(DATE_FMT) : "Immediate");

            String html = templateEngine.process("email/fee-overdue", context);
            sendHtmlEmail(toEmail, "Urgent: Fee Payment Overdue for " + studentName, html);
        } catch (Exception e) {
            log.error("Failed to send Fee Overdue email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendNewTicketAdminEmail(String adminEmail, String ticketSubject, String raisedBy, String category) {
        if (adminEmail == null || adminEmail.isBlank()) return;
        try {
            Context context = new Context();
            context.setVariable("ticketSubject", ticketSubject);
            context.setVariable("raisedBy", raisedBy);
            context.setVariable("category", category);

            String html = templateEngine.process("email/new-ticket", context);
            sendHtmlEmail(adminEmail, "New Support Ticket: " + ticketSubject, html);
        } catch (Exception e) {
            log.error("Failed to send New Ticket email to admin {}: {}", adminEmail, e.getMessage());
        }
    }

    @Async
    public void sendTicketResolvedEmail(String toEmail, String ticketSubject, String resolutionNote) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            Context context = new Context();
            context.setVariable("ticketSubject", ticketSubject);
            context.setVariable("resolutionNote", resolutionNote != null ? resolutionNote : "Resolved");

            String html = templateEngine.process("email/ticket-resolved", context);
            sendHtmlEmail(toEmail, "Support Ticket Resolved: " + ticketSubject, html);
        } catch (Exception e) {
            log.error("Failed to send Ticket Resolved email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendFeeGeneratedEmail(String toEmail, String studentName, String monthYear, BigDecimal amount, LocalDate dueDate) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            Context context = new Context();
            context.setVariable("studentName", studentName);
            context.setVariable("monthYear", monthYear);
            context.setVariable("amount", amount != null ? amount.toString() : "0.00");
            context.setVariable("dueDate", dueDate != null ? dueDate.format(DATE_FMT) : "10th of this month");

            String html = templateEngine.process("email/fee-generated", context);
            sendHtmlEmail(toEmail, "New Tuition Fee Invoice: " + monthYear + " (" + studentName + ")", html);
        } catch (Exception e) {
            log.error("Failed to send Fee Generated email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            Context context = new Context();
            context.setVariable("resetLink", resetLink);

            String html = templateEngine.process("email/password-reset", context);
            sendHtmlEmail(toEmail, "Password Reset Request - Shiksha ERP", html);
        } catch (Exception e) {
            log.error("Failed to send Password Reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("[MOCK EMAIL] To: {} | Subject: {} | Delivery Skipped (app.mail.enabled=false)", to, subject);
            return;
        }

        if (mailSender == null) {
            log.warn("JavaMailSender bean is not configured. Cannot send email to {}", to);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email dispatched successfully to {}", to);
        } catch (Exception e) {
            log.warn("Could not dispatch SMTP email to {}: {}", to, e.getMessage());
        }
    }
}
