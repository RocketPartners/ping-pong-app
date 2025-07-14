package com.example.javapingpongelo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.util.Date;

/**
 * Represents the result of a game played by a player.
 * This is used in the Player entity to track game history.
 */
@Embeddable
@Audited
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameResult {

    @Column(nullable = false)
    private Date date;

    @Column(nullable = false)
    private boolean isWin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameType gameType;

    // Add a field to identify the game in audit tables
    @Column(name = "game_history", nullable = false)
    @Builder.Default
    private String gameIdentifier = "GAME_RESULT";
}