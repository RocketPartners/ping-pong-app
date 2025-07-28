package com.example.javapingpongelo.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class EmailSenderImpl implements EmailSender {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine emailTemplateEngine;
    
    @Value("${app.email.from:noreply@example.com}")
    private String fromEmail;
    
    @Value("${FRONTEND_URL:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void sendVerificationEmail(String email, String fullName, String token) {
        log.info("Sending verification email to {}", email);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Verify Your Email Address");
            
            Context context = new Context();
            context.setVariable("playerName", fullName);
            context.setVariable("verificationUrl", frontendUrl + "/verify-email?token=" + token);
            
            String emailContent = emailTemplateEngine.process("email-verification", context);
            
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", email, e.getMessage());
        }
    }
    
    @Override
    public void sendInvitationEmail(String email, String invitationCode) {
        log.info("Sending invitation email to {}", email);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("You're Invited to Join Ping Pong Elo");
            
            String registrationUrl = frontendUrl + "/register?code=" + invitationCode;
            
            StringBuilder emailContent = new StringBuilder();
            emailContent.append("<div>");
            emailContent.append("<h2>You're Invited to Join Ping Pong Elo!</h2>");
            emailContent.append("<p>You've been invited to join our Ping Pong Elo rating system. Click the link below to register:</p>");
            emailContent.append("<a href=\"").append(registrationUrl).append("\">Register Now</a>");
            emailContent.append("<p>Your invitation code: <strong>").append(invitationCode).append("</strong></p>");
            emailContent.append("<p>This invitation will expire in ").append(30).append(" days.</p>");
            emailContent.append("</div>");
            
            helper.setText(emailContent.toString(), true);
            
            mailSender.send(message);
            log.info("Invitation email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send invitation email to {}: {}", email, e.getMessage());
        }
    }
}