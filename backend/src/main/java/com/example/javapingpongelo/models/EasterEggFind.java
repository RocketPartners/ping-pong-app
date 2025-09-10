package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Entity representing when a player found an easter egg.
 * Tracks the history of all egg discoveries.
 */
@Entity
@Table(name = "easter_egg_find", indexes = {
        @Index(name = "idx_egg_find_player", columnList = "playerId"),
        @Index(name = "idx_egg_find_date", columnList = "foundAt"),
        @Index(name = "idx_egg_find_page", columnList = "pageFoundOn")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EasterEggFind {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID eggId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date foundAt;

    @Column(nullable = false)
    private int pointsAwarded;

    @Column(nullable = false)
    private String pageFoundOn;

    @Column
    private String sessionId;             // Track finding session for analytics

    @Column
    private String userAgent;            // Browser info for analytics

    @Enumerated(EnumType.STRING)
    @Column
    private EasterEgg.EggType eggType;    // Cache egg type for easier queries

    @PrePersist
    protected void onCreate() {
        if (foundAt == null) {
            foundAt = new Date();
        }
    }
}