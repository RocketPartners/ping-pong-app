package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity that stores pre-computed player statistics for efficient achievement evaluation.
 * Updated after each game to avoid expensive calculations during achievement evaluation.
 */
@Entity
@Table(name = "player_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatistics {
    
    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    // Game counts
    @Column(name = "total_games")
    @Builder.Default
    private Integer totalGames = 0;

    @Column(name = "total_wins")
    @Builder.Default
    private Integer totalWins = 0;

    @Column(name = "total_losses")
    @Builder.Default
    private Integer totalLosses = 0;

    // Game type specific counts
    @Column(name = "singles_games")
    @Builder.Default
    private Integer singlesGames = 0;

    @Column(name = "singles_wins")
    @Builder.Default
    private Integer singlesWins = 0;

    @Column(name = "doubles_games")
    @Builder.Default
    private Integer doublesGames = 0;

    @Column(name = "doubles_wins")
    @Builder.Default
    private Integer doublesWins = 0;

    @Column(name = "ranked_games")
    @Builder.Default
    private Integer rankedGames = 0;

    @Column(name = "ranked_wins")
    @Builder.Default
    private Integer rankedWins = 0;

    @Column(name = "normal_games")
    @Builder.Default
    private Integer normalGames = 0;

    @Column(name = "normal_wins")
    @Builder.Default
    private Integer normalWins = 0;

    // Streak tracking
    @Column(name = "current_win_streak")
    @Builder.Default
    private Integer currentWinStreak = 0;

    @Column(name = "max_win_streak")
    @Builder.Default
    private Integer maxWinStreak = 0;

    @Column(name = "current_loss_streak")
    @Builder.Default
    private Integer currentLossStreak = 0;

    // Points and scores
    @Column(name = "total_points_scored")
    @Builder.Default
    private Integer totalPointsScored = 0;

    @Column(name = "total_points_conceded")
    @Builder.Default
    private Integer totalPointsConceded = 0;

    // Opponent statistics (stored as JSON)
    @Column(columnDefinition = "TEXT")
    private String opponentWinCounts; // JSON: {"opponentId": winCount}

    @Column(columnDefinition = "TEXT")
    private String opponentLossCounts; // JSON: {"opponentId": lossCount}

    // Unique opponents
    @Column(name = "unique_opponents_played")
    @Builder.Default
    private Integer uniqueOpponentsPlayed = 0;

    // Special achievements tracking
    @Column(name = "shutout_wins") // Games won with opponent scoring <5
    @Builder.Default
    private Integer shutoutWins = 0;

    @Column(name = "close_wins") // Games won with 2 point difference
    @Builder.Default
    private Integer closeWins = 0;

    @Column(name = "gilyed_given") // Times player gilyed someone (0 points)
    @Builder.Default
    private Integer gilyedGiven = 0;

    @Column(name = "gilyed_received") // Times player was gilyed
    @Builder.Default
    private Integer gilyedReceived = 0;

    // Timestamps
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "last_game_date")
    private LocalDateTime lastGameDate;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Calculates win rate as percentage
     */
    public double getWinRate() {
        if (totalGames == null || totalGames == 0) {
            return 0.0;
        }
        return (totalWins * 100.0) / totalGames;
    }

    /**
     * Gets the maximum wins against any single opponent
     */
    public int getMaxWinsAgainstSingleOpponent() {
        if (opponentWinCounts == null || opponentWinCounts.isEmpty()) {
            return 0;
        }
        // This would be parsed from JSON in a real implementation
        // For now, return 0 - will be implemented in service layer
        return 0;
    }
}