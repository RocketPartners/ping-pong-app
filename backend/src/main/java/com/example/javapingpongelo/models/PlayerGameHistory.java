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
 * Entity to represent a player's game history entry.
 * This replaces the @ElementCollection approach for better performance and flexibility.
 */
@Entity
@Table(name = "player_game_history", indexes = {
        @Index(name = "idx_game_history_player", columnList = "player_id"),
        @Index(name = "idx_game_history_date", columnList = "game_date"),
        @Index(name = "idx_game_history_game", columnList = "game_id"),
        @Index(name = "idx_game_history_opponent", columnList = "opponent_id"),
        @Index(name = "idx_game_history_win", columnList = "is_win"),
        @Index(name = "idx_game_history_game_type", columnList = "game_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerGameHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "game_date", nullable = false)
    private Date gameDate;

    @Column(nullable = false)
    private boolean isWin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "opponent_id")
    private UUID opponentId;

    private int playerScore;

    private int opponentScore;

    @CreationTimestamp
    @Column(name = "record_created", nullable = false, updatable = false)
    private Date recordCreated;
}