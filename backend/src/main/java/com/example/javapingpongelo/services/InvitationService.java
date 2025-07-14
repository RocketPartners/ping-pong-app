package com.example.javapingpongelo.services;

import com.example.javapingpongelo.configuration.DomainRestrictionConfig;
import com.example.javapingpongelo.models.InvitationCode;
import com.example.javapingpongelo.repositories.InvitationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationCodeRepository invitationCodeRepository;
    private final DomainRestrictionConfig domainRestrictionConfig;

    @Autowired
    public InvitationService(
            InvitationCodeRepository invitationCodeRepository,
            DomainRestrictionConfig domainRestrictionConfig) {
        this.invitationCodeRepository = invitationCodeRepository;
        this.domainRestrictionConfig = domainRestrictionConfig;
    }

    /**
     * Generate a domain-specific invitation code
     */
    @Transactional
    public String generateDomainInvitationCode(String domain) {
        String code = generateRandomCode();
        
        InvitationCode invitationCode = InvitationCode.builder()
                .code(code)
                .allowedDomain(domain)
                .specificEmail(null)
                .expiryDate(LocalDateTime.now().plusDays(domainRestrictionConfig.getInvitationCodeExpiryDays()))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
                
        invitationCodeRepository.save(invitationCode);
        return code;
    }
    
    /**
     * Generate a specific email invitation code
     */
    @Transactional
    public String generateSpecificEmailInvitationCode(String email) {
        String code = generateRandomCode();
        
        InvitationCode invitationCode = InvitationCode.builder()
                .code(code)
                .allowedDomain(null)
                .specificEmail(email)
                .expiryDate(LocalDateTime.now().plusDays(domainRestrictionConfig.getInvitationCodeExpiryDays()))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
                
        invitationCodeRepository.save(invitationCode);
        return code;
    }
    
    /**
     * Generate a general invitation code (no restrictions)
     */
    @Transactional
    public String generateGeneralInvitationCode() {
        String code = generateRandomCode();
        
        InvitationCode invitationCode = InvitationCode.builder()
                .code(code)
                .allowedDomain(null)
                .specificEmail(null)
                .expiryDate(LocalDateTime.now().plusDays(domainRestrictionConfig.getInvitationCodeExpiryDays()))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
                
        invitationCodeRepository.save(invitationCode);
        return code;
    }

    /**
     * Validate an invitation code for a specific email
     */
    @Transactional(readOnly = true)
    public boolean validateInvitationCode(String code, String email) {
        Optional<InvitationCode> invitationCodeOpt = invitationCodeRepository.findByCode(code);
        
        if (invitationCodeOpt.isEmpty()) {
            return false;
        }
        
        InvitationCode invitationCode = invitationCodeOpt.get();
        return invitationCode.isValidForEmail(email);
    }
    
    /**
     * Mark an invitation code as used
     */
    @Transactional
    public void markInvitationCodeAsUsed(String code) {
        invitationCodeRepository.findByCode(code).ifPresent(invitationCode -> {
            invitationCode.setUsed(true);
            invitationCodeRepository.save(invitationCode);
        });
    }
    
    /**
     * Check if an email is allowed to register (either by domain or invitation code)
     */
    @Transactional(readOnly = true)
    public boolean isEmailAllowedToRegister(String email, String invitationCode) {
        // If domain restriction isn't enabled, allow any email
        if (!domainRestrictionConfig.isDomainRestrictionEnabled()) {
            return true;
        }
        
        // Check if the email domain is in the allowed domains list
        if (domainRestrictionConfig.isDomainAllowed(email)) {
            return true;
        }
        
        // If we have an invitation code, validate it
        if (invitationCode != null && !invitationCode.isEmpty()) {
            return validateInvitationCode(invitationCode, email);
        }
        
        // No valid domain or invitation code
        return false;
    }
    
    /**
     * Get all active invitation codes
     */
    @Transactional(readOnly = true)
    public List<InvitationCode> getAllActiveInvitationCodes() {
        return invitationCodeRepository.findByUsedFalseAndExpiryDateAfter(LocalDateTime.now());
    }
    
    /**
     * Generate a random alphanumeric code
     */
    private String generateRandomCode() {
        // Generate a 12-character alphanumeric code
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        
        // Format as XXXX-XXXX-XXXX
        return uuid.replaceAll("(.{4})(.{4})(.{4})", "$1-$2-$3");
    }
}