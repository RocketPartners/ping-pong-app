package com.example.javapingpongelo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DomainRestrictionConfig {
    
    private final Set<String> allowedDomains;
    private final boolean domainRestrictionEnabled;
    private final int invitationCodeExpiryDays;
    private final boolean requireEmailVerification;
    
    public DomainRestrictionConfig(
            @Value("${app.registration.allowed-domains:}") String allowedDomainsStr,
            @Value("${app.registration.domain-restriction-enabled:false}") boolean domainRestrictionEnabled,
            @Value("${app.registration.invitation-code-expiry-days:30}") int invitationCodeExpiryDays,
            @Value("${app.registration.require-email-verification:true}") boolean requireEmailVerification) {
        
        this.allowedDomains = new HashSet<>();
        if (allowedDomainsStr != null && !allowedDomainsStr.trim().isEmpty()) {
            this.allowedDomains.addAll(Arrays.asList(allowedDomainsStr.split(",")));
        }
        this.domainRestrictionEnabled = domainRestrictionEnabled;
        this.invitationCodeExpiryDays = invitationCodeExpiryDays;
        this.requireEmailVerification = requireEmailVerification;
    }
    
    public Set<String> getAllowedDomains() {
        return allowedDomains;
    }
    
    public boolean isDomainAllowed(String email) {
        if (!domainRestrictionEnabled) {
            return true;
        }
        
        if (email == null || !email.contains("@")) {
            return false;
        }
        
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        return allowedDomains.contains(domain);
    }
    
    public boolean isDomainRestrictionEnabled() {
        return domainRestrictionEnabled;
    }
    
    public int getInvitationCodeExpiryDays() {
        return invitationCodeExpiryDays;
    }
    
    public boolean isEmailVerificationRequired() {
        return requireEmailVerification;
    }
}