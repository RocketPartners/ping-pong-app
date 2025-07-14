package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.util.Date;
import java.util.UUID;

/**
 * Entity to track changes in player ELO ratings and rankings over time.
 */
@Entity
@Table(name = "player_elo_history", indexes = {
        @Index(name = "idx_elo_history_player", columnList = "player_id"),
        @Index(name = "idx_elo_history_game", columnList = "game_id"),
        @Index(name = "idx_elo_history_date", columnList = "timestamp"),
        @Index(name = "idx_elo_history_game_type", columnList = "game_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEloHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "match_id")
    private UUID matchId;

    @Column(name = "timestamp", nullable = false)
    private Date timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @Column(name = "previous_elo", nullable = false)
    private int previousElo;

    @Column(name = "new_elo", nullable = false)
    private int newElo;

    @Column(name = "elo_change")
    private int eloChange;

    @Column(name = "rank_position")
    private int rankPosition;

    @Column(name = "total_players")
    private int totalPlayers;

    @Column(name = "percentile")
    private double percentile;

    @CreationTimestamp
    @Column(name = "record_created", nullable = false, updatable = false)
    private Date recordCreated;

    /**
     * Calculate and set the ELO change
     */
    @PrePersist
    @PreUpdate
    public void calculateEloChange() {
        this.eloChange = this.newElo - this.previousElo;

        // Calculate percentile if both rank position and total players are set
        if (this.totalPlayers > 0 && this.rankPosition > 0) {
            this.percentile = 100.0 * (1.0 - ((double) this.rankPosition / this.totalPlayers));
        }
    }
}