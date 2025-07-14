package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.PasswordResetToken;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.repositories.PasswordResetTokenRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class PasswordResetService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.reset-token.expiry-minutes:30}")
    private int tokenExpiryMinutes;

    @Transactional
    public void createPasswordResetTokenForEmail(String email) {
        log.info("Creating password reset token for email: {}", email);

        Player player = playerRepository.findByEmail(email);
        if (player == null) {
            // Don't reveal that the email doesn't exist for security reasons
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        // Delete any existing tokens for this player
        tokenRepository.deleteByPlayerId(player.getPlayerId());

        // Create a new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                                                          .token(token)
                                                          .playerId(player.getPlayerId())
                                                          .expiryDate(LocalDateTime.now().plusMinutes(tokenExpiryMinutes))
                                                          .used(false)
                                                          .build();

        tokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(player.getEmail(), token);
        }
        catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", player.getEmail(), e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                                                       .orElseThrow(() -> new ResourceNotFoundException("Invalid password reset token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Password reset token has expired");
        }

        if (resetToken.isUsed()) {
            throw new BadRequestException("Password reset token has already been used");
        }

        Player player = playerRepository.findById(resetToken.getPlayerId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Player not found"));

        // Update password
        player.setPassword(passwordEncoder.encode(newPassword));
        playerRepository.save(player);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for player: {}", player.getUsername());
    }

    @Transactional(readOnly = true)
    public boolean validatePasswordResetToken(String token) {
        return tokenRepository.findByToken(token)
                              .map(resetToken -> !resetToken.isExpired() && !resetToken.isUsed())
                              .orElse(false);
    }
}