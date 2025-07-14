# Email System Documentation

This document explains how the email system works in the Java Ping Pong Elo application.

## Overview

The email system is built using **Spring Boot Mail** with **Thymeleaf templates** for professional HTML emails. It supports email verification, game confirmations, password resets, and player invitations.

## Dependencies & Setup

### Required Dependencies
Add to `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-mail'
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
```

### SMTP Configuration
Configure in `application.properties`:
```properties
# SMTP Settings (Gmail example)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Application Settings
app.email.from=noreply@yourapp.com
app.baseUrl=http://localhost:4200
app.reset-token.expiry-minutes=30
app.email-verification.token-expiry-hours=24

# Email Verification
app.registration.require-email-verification=true
app.registration.domain-restriction-enabled=false
app.registration.allowed-domains=yourcompany.com,example.com
```

## Architecture

### Core Services

1. **EmailService** (`services/EmailService.java`)
   - Main service for game confirmations, rejections, password resets
   - Uses `JavaMailSender` and Thymeleaf templates

2. **EmailSender Interface & Implementation** 
   - `services/EmailSender.java` (interface)
   - `services/EmailSenderImpl.java` (implementation)
   - Handles verification and invitation emails

3. **EmailVerificationService**
   - `services/EmailVerificationService.java` (interface)
   - `services/EmailVerificationServiceImpl.java` (implementation)
   - Manages verification tokens and domain restrictions

### Key Configuration Classes

1. **ThymeleafConfig** (`configuration/ThymeleafConfig.java`)
   ```java
   @Bean
   public TemplateEngine emailTemplateEngine() {
       SpringTemplateEngine templateEngine = new SpringTemplateEngine();
       templateEngine.setTemplateResolver(emailTemplateResolver());
       return templateEngine;
   }
   ```

2. **DomainRestrictionConfig** (`configuration/DomainRestrictionConfig.java`)
   - Configures email domain restrictions
   - Manages verification requirements

## Email Templates

Templates are located in `src/main/resources/templates/email/`:

### 1. Email Verification (`email-verification.html`)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div style="max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif;">
        <h2>Verify Your Email</h2>
        <p>Click the button below to verify your email address:</p>
        <a th:href="${verificationUrl}" 
           style="background-color: #007bff; color: white; padding: 10px 20px; 
                  text-decoration: none; border-radius: 5px;">
            Verify Email
        </a>
    </div>
</body>
</html>
```

### 2. Game Confirmation (`game-confirmation-email.html`)
- Handles both singles and doubles games
- Includes Confirm/Reject buttons
- Player information and game details

### 3. Game Rejection Notification (`game-rejection-notification.html`)
- Notifies players when results are rejected
- Includes game details and next steps

## Data Models

### EmailVerificationToken (`models/EmailVerificationToken.java`)
```java
@Entity
public class EmailVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String token;
    
    @OneToOne(targetEntity = Player.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "player_id")
    private Player player;
    
    private Date expiryDate;
    
    // Constructor, getters, setters...
}
```

## Email Types & Usage

### 1. Email Verification
```java
// Send verification email
emailVerificationService.sendVerificationEmail(player);

// Verify token
VerificationResult result = emailVerificationService.verifyEmail(token);
```

### 2. Game Confirmations
```java
// Send game confirmation
emailService.sendGameConfirmationEmail(game, playerEmails);
```

### 3. Password Reset
```java
// Send password reset email
emailService.sendPasswordResetEmail(player, resetToken);
```

### 4. Player Invitations
```java
// Send invitation email
emailSender.sendInvitationEmail(email, inviterName, leagueName);
```

## Controllers

### EmailVerificationController (`controllers/EmailVerificationController.java`)
- **GET** `/verify-email` - Web verification page
- **GET** `/api/verify-email` - API verification endpoint
- **POST** `/api/resend-verification` - Resend verification

### Development Controllers
- **EmailTestController** - Test email sending during development
- **EmailPreviewController** - Preview email templates in browser

## Domain Restrictions

Configure domain restrictions for corporate environments:

```properties
app.registration.domain-restriction-enabled=true
app.registration.allowed-domains=company.com,subsidiary.com
```

Validation happens in `EmailVerificationServiceImpl`:
```java
if (domainRestrictionConfig.isDomainRestrictionEnabled()) {
    if (!domainRestrictionConfig.isAllowedDomain(email)) {
        throw new InvalidEmailDomainException("Email domain not allowed");
    }
}
```

## Custom Validation

### Email Validator (`validators/EmailValidator.java`)
```java
@Component
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
    
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return email != null && Pattern.matches(EMAIL_PATTERN, email);
    }
}
```

### Usage in DTOs
```java
public class PlayerRegistrationDto {
    @ValidEmail
    @NotBlank
    private String email;
    // ...
}
```

## Testing & Development

### Preview Templates
Visit `http://localhost:8080/test/email-preview/{templateName}` to preview templates.

### Test Email Sending
Use `EmailTestController` endpoints during development:
- `/test/send-verification/{email}`
- `/test/send-invitation/{email}`

## Troubleshooting

### Common Issues

1. **SMTP Authentication Failed**
   - Use app passwords for Gmail
   - Verify SMTP settings match provider requirements

2. **Templates Not Found**
   - Check `ThymeleafConfig` template resolver path
   - Ensure templates are in `src/main/resources/templates/email/`

3. **Email Not Delivered**
   - Check spam folders
   - Verify `app.email.from` is valid
   - Test with email testing services (MailHog, Mailtrap)

4. **Token Expiration**
   - Adjust `app.email-verification.token-expiry-hours`
   - Check token generation logic

### Environment Variables

For production, use environment variables:
```bash
export MAIL_HOST=smtp.yourdomain.com
export MAIL_USERNAME=noreply@yourdomain.com
export MAIL_PASSWORD=your-secure-password
```

## Security Considerations

1. **Never commit email passwords** - Use environment variables
2. **Use app passwords** for Gmail/OAuth providers  
3. **Validate email domains** if using domain restrictions
4. **Set appropriate token expiry times**
5. **Use HTTPS** for verification links in production

## Email Providers

### Gmail Setup
1. Enable 2FA on your Google account
2. Generate an App Password
3. Use `smtp.gmail.com:587` with STARTTLS

### Other Providers
- **Outlook**: `smtp-mail.outlook.com:587`
- **SendGrid**: `smtp.sendgrid.net:587`
- **Mailgun**: `smtp.mailgun.org:587`

This email system provides a robust foundation for transactional emails with professional templates and comprehensive verification workflows.