package com.example.javapingpongelo.services;

/**
 * Interface for sending emails
 */
public interface EmailSender {
    
    /**
     * Send verification email
     */
    void sendVerificationEmail(String email, String fullName, String token);
    
    /**
     * Send invitation email
     */
    void sendInvitationEmail(String email, String invitationCode);
    
}