package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.exception.EmailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final JavaMailSender mailSender;

    public void sendAssignmentEmail(String toEmail, String toName,
            String projectName, String itemTitle,
            String dueDate, Long workItemId) {
        String subject = "New Work Item Assigned: " + itemTitle;
        String html = buildEmailHtml(toName, projectName, itemTitle, dueDate,
                frontendUrl + "/work-items/" + workItemId);
        sendEmail(toEmail, subject, html);
    }

    public void sendDueDateEmail(String toEmail, String toName,
            String projectName, String itemTitle,
            Long workItemId) {
        String subject = "Due Date Reached: " + itemTitle;
        String html = buildDueDateHtml(toName, projectName, itemTitle,
                frontendUrl + "/work-items/" + workItemId);
        sendEmail(toEmail, subject, html);
    }

    public void sendPasswordSetupEmail(String toEmail, String toName, String token) {
        String subject = "Set your password - Kanban Board";
        String setupLink = frontendUrl + "/change-password?token=" + token;
        String html = buildPasswordSetupHtml(toName, setupLink);
        sendEmail(toEmail, subject, html);
    }

    public void resendPasswordSetupEmail(String toEmail, String toName, String token) {
        String subject = "Password setup link - Kanban Board";
        String setupLink = frontendUrl + "/change-password?token=" + token;
        String html = buildPasswordSetupLinkOnlyHtml(toName, setupLink);
        sendEmail(toEmail, subject, html);
    }

    private void sendEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email send error for {}: {}", to, e.getMessage());
            throw new EmailDeliveryException(
                    "Unable to send email to " + to
                            + ". Check SMTP settings (spring.mail.username, spring.mail.password, app.email.from).",
                    e);
        }
    }

    private String buildEmailHtml(String name, String project, String title,
            String dueDate, String link) {
        return """
                <h2>Hello %s,</h2>
                <p>You have been assigned a new work item.</p>
                <table>
                  <tr><td><strong>Project:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Title:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Due Date:</strong></td><td>%s</td></tr>
                </table>
                <p><a href="%s">View Work Item</a></p>
                """.formatted(name, project, title, dueDate != null ? dueDate : "N/A", link);
    }

    private String buildDueDateHtml(String name, String project, String title, String link) {
        return """
                <h2>Hello %s,</h2>
                <p>The due date for the following work item has been reached.</p>
                <table>
                  <tr><td><strong>Project:</strong></td><td>%s</td></tr>
                  <tr><td><strong>Title:</strong></td><td>%s</td></tr>
                </table>
                <p><a href="%s">View Work Item</a></p>
                """.formatted(name, project, title, link);
    }

    private String buildPasswordSetupHtml(String name, String setupLink) {
        return """
                <h2>Hello %s,</h2>
                <p>Your account has been created by an administrator.</p>
                <p>Please set your own password using the link below:</p>
                <p><a href="%s">Set My Password</a></p>
                <p>This link expires in 24 hours and can be used once.</p>
                """.formatted(name, setupLink);
    }

    private String buildPasswordSetupLinkOnlyHtml(String name, String setupLink) {
        return """
                <h2>Hello %s,</h2>
                <p>An administrator requested a new password setup link for your account.</p>
                <p><a href="%s">Set My Password</a></p>
                <p>This link expires in 24 hours and can be used once.</p>
                """.formatted(name, setupLink);
    }
}
