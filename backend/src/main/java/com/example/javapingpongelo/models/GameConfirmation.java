package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track the confirmation status of a game for each player.
 * Players have a 48-hour window to reject a game after it's saved.
 */
@Entity
@Table(name = "game_confirmation", indexes = {
        @Index(name = "idx_confirmation_game", columnList = "game_id"),
        @Index(name = "idx_confirmation_player", columnList = "player_id"),
        @Index(name = "idx_confirmation_status", columnList = "status"),
        @Index(name = "idx_confirmation_token", columnList = "confirmation_token", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfirmationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "confirmation_token", nullable = false, unique = true)
    private String confirmationToken;

    @Column(name = "original_elo", nullable = false)
    private int originalElo;

    @Column(name = "new_elo", nullable = false)
    private int newElo;

    @Column(name = "expiration_date", nullable = false)
    private LocalDateTime expirationDate;

    /**
     * Checks if the confirmation window has expired
     */
    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }

    /**
     * Set up the expiration date 48 hours from creation
     */
    @PrePersist
    protected void onCreate() {
        if (expirationDate == null) {
            expirationDate = LocalDateTime.now().plusHours(48);
        }
        if (status == null) {
            status = ConfirmationStatus.PENDING;
        }
    }

    /**
     * Confirmation status enum
     */
    public enum ConfirmationStatus {
        PENDING,   // Initial state, waiting for response or expiration
        CONFIRMED, // Explicitly confirmed by player or auto-confirmed after timeout
        REJECTED   // Player has rejected the game within the 48-hour window
    }
}