package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Player;

/**
 * Service for handling email verification
 */
public interface EmailVerificationService {
    
    /**
     * Create a verification token for a player and send verification email
     */
    void createVerificationTokenAndSendEmail(Player player);
    
    /**
     * Verify email using token
     */
    boolean verifyEmail(String token);
    
    /**
     * Resend verification email
     */
    boolean resendVerificationEmail(String email);
}