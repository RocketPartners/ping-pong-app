package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenge", indexes = {
        @Index(name = "idx_challenge_challenger", columnList = "challengerId"),
        @Index(name = "idx_challenge_challenged", columnList = "challengedId"),
        @Index(name = "idx_challenge_status", columnList = "status"),
        @Index(name = "idx_challenge_created", columnList = "createdAt"),
        @Index(name = "idx_challenge_expires", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Challenge {
    
    @Id
    @GeneratedValue
    @Column(name = "challenge_id", updatable = false, nullable = false)
    private UUID challengeId;
    
    @Column(nullable = false)
    private UUID challengerId;
    
    @Column(nullable = false)  
    private UUID challengedId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;
    
    @Column(length = 500)
    private String message;
    
    @Column(nullable = false)
    private boolean isRanked;
    
    @Column(nullable = false)
    private boolean isSingles;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column
    private LocalDateTime respondedAt;
    
    @Column
    private UUID completedGameId;
    
    @Column(length = 500)
    private String declineReason;
    
    // Slack-specific fields
    @Column(length = 100)
    private String slackChannelId;
    
    @Column(length = 100)
    private String slackMessageTs;
    
    @Column(length = 100)
    private String challengerSlackId;
    
    @Column(length = 100)
    private String challengedSlackId;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(24); // 24 hour expiry
        }
        if (status == null) {
            status = ChallengeStatus.PENDING;
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ChallengeStatus.PENDING;
    }
    
    public boolean canBeAccepted() {
        return status == ChallengeStatus.PENDING && !isExpired();
    }
}