package com.example.javapingpongelo.services;

import com.example.javapingpongelo.configuration.DomainRestrictionConfig;
import com.example.javapingpongelo.models.EmailVerificationToken;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.EmailVerificationTokenRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final PlayerRepository playerRepository;
    private final DomainRestrictionConfig domainRestrictionConfig;
    private final EmailSender emailSender;
    
    @Value("${app.email-verification.token-expiry-hours:24}")
    private int tokenExpiryHours;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public EmailVerificationServiceImpl(
            EmailVerificationTokenRepository tokenRepository,
            PlayerRepository playerRepository,
            DomainRestrictionConfig domainRestrictionConfig,
            EmailSender emailSender) {
        this.tokenRepository = tokenRepository;
        this.playerRepository = playerRepository;
        this.domainRestrictionConfig = domainRestrictionConfig;
        this.emailSender = emailSender;
    }

    /**
     * Create a verification token for a player and send verification email
     */
    @Override
    @Transactional
    public void createVerificationTokenAndSendEmail(Player player) {
        // Check if verification is required
        if (!domainRestrictionConfig.isEmailVerificationRequired()) {
            // Auto-verify if not required
            player.setEmailVerified(true);
            playerRepository.save(player);
            return;
        }
        
        // Create a new verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .player(player)
                .expiryDate(LocalDateTime.now().plusHours(tokenExpiryHours))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
                
        tokenRepository.save(verificationToken);
        
        // Send verification email
        emailSender.sendVerificationEmail(player.getEmail(), player.getFullName(), token);
    }
    
    /**
     * Verify email using token
     */
    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        EmailVerificationToken verificationToken = tokenOpt.get();
        
        // Check if token is expired or already used
        if (verificationToken.isExpired() || verificationToken.isUsed()) {
            return false;
        }
        
        // Mark token as used
        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);
        
        // Update player's verified status
        Player player = verificationToken.getPlayer();
        player.setEmailVerified(true);
        playerRepository.save(player);
        
        return true;
    }
    
    /**
     * Resend verification email
     */
    @Override
    @Transactional
    public boolean resendVerificationEmail(String email) {
        Optional<Player> playerOpt = Optional.ofNullable(playerRepository.findByEmail(email));
        
        if (playerOpt.isEmpty()) {
            return false;
        }
        
        Player player = playerOpt.get();
        
        // Check if player is already verified
        if (player.isEmailVerified()) {
            return false;
        }
        
        // Invalidate existing tokens
        tokenRepository.findByPlayer(player).forEach(token -> {
            token.setUsed(true);
            tokenRepository.save(token);
        });
        
        // Create new token and send email
        createVerificationTokenAndSendEmail(player);
        return true;
    }
}