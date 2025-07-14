package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invitation_codes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column
    private String allowedDomain;

    @Column
    private String specificEmail;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValidForEmail(String email) {
        if (isExpired() || used) {
            return false;
        }

        // For specific email invitations
        if (specificEmail != null && !specificEmail.isEmpty()) {
            return specificEmail.equalsIgnoreCase(email);
        }

        // For domain-based invitations
        if (allowedDomain != null && !allowedDomain.isEmpty()) {
            return email.toLowerCase().endsWith("@" + allowedDomain.toLowerCase());
        }

        // General invitation (no restrictions)
        return true;
    }
}