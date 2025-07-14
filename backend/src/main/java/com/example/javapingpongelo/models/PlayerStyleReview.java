package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Entity that represents a review of a player's style by another player.
 * Used to track player style feedback and adjust style ratings over time.
 */
@Entity
@Table(name = "player_style_review", indexes = {
        @Index(name = "idx_review_player", columnList = "player_id"),
        @Index(name = "idx_review_reviewer", columnList = "reviewer_id"),
        @Index(name = "idx_review_date", columnList = "review_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleReview {

    @ElementCollection
    @CollectionTable(
            name = "player_style_review_games",
            joinColumns = @JoinColumn(name = "review_id"),
            indexes = {
                    @Index(name = "idx_review_games", columnList = "review_id"),
                    @Index(name = "idx_game_id", columnList = "game_id")
            }
    )
    @Column(name = "game_id")
    @Builder.Default
    List<UUID> gameIds = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Builder.Default
    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged = false;

    @Builder.Default
    @Column(name = "is_response", nullable = false)
    private boolean response = false;

    @Column(name = "parent_review_id")
    private UUID parentReviewId;

    @ElementCollection
    @CollectionTable(
            name = "player_style_review_strengths",
            joinColumns = @JoinColumn(name = "review_id"),
            indexes = @Index(name = "idx_review_strengths", columnList = "review_id, strength")
    )
    @Column(name = "strength")
    @Enumerated(EnumType.STRING)
    private List<PlayerStyle> strengths;

    @ElementCollection
    @CollectionTable(
            name = "player_style_review_improvements",
            joinColumns = @JoinColumn(name = "review_id"),
            indexes = @Index(name = "idx_review_improvements", columnList = "review_id, improvement")
    )
    @Column(name = "improvement")
    @Enumerated(EnumType.STRING)
    private List<PlayerStyle> improvements;

    @Column(name = "acknowledged_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date acknowledgedDate;

    @Column(name = "review_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewDate;

    // New fields for reviewer information
    @Column(name = "reviewer_username")
    private String reviewerUsername;

    @Column(name = "reviewer_first_name")
    private String reviewerFirstName;

    @Column(name = "reviewer_last_name")
    private String reviewerLastName;

    @PrePersist
    protected void onCreate() {
        if (reviewDate == null) {
            reviewDate = new Date();
        }
    }
}