package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Entity representing a player's easter egg hunting statistics.
 * Aggregated stats for performance and leaderboards.
 */
@Entity
@Table(name = "easter_egg_stats", indexes = {
        @Index(name = "idx_egg_stats_player", columnList = "playerId"),
        @Index(name = "idx_egg_stats_total_found", columnList = "totalEggsFound"),
        @Index(name = "idx_egg_stats_points", columnList = "totalPointsEarned")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EasterEggStats {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID playerId;

    @Column(nullable = false)
    @Builder.Default
    private int totalEggsFound = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalPointsEarned = 0;

    @Temporal(TemporalType.TIMESTAMP)
    private Date firstEggFound;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastEggFound;

    @Column(nullable = false)
    @Builder.Default
    private int commonEggsFound = 0;

    @Column(nullable = true)
    @Builder.Default
    private Integer uncommonEggsFound = 0;

    @Column(nullable = true)
    @Builder.Default
    private Integer rareEggsFound = 0;

    @Column(nullable = true)
    @Builder.Default
    private Integer epicEggsFound = 0;

    @Column(nullable = true)
    @Builder.Default
    private Integer legendaryEggsFound = 0;

    @Column(nullable = true)
    @Builder.Default
    private Integer mythicalEggsFound = 0;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;        // Current consecutive days with egg finds

    @Column(nullable = false)
    @Builder.Default
    private int longestStreak = 0;        // Longest consecutive days with egg finds

    @Column
    private String favoriteHuntingPage;   // Page where player finds most eggs

    /**
     * Update stats when a new egg is found
     */
    public void recordEggFound(EasterEgg.EggType eggType, int points, String pageFound) {
        this.totalEggsFound++;
        this.totalPointsEarned += points;
        this.lastEggFound = new Date();
        
        if (this.firstEggFound == null) {
            this.firstEggFound = new Date();
        }

        // Update egg type counts (handle nullable integers)
        switch (eggType) {
            case COMMON -> this.commonEggsFound++;
            case UNCOMMON -> this.uncommonEggsFound = (this.uncommonEggsFound != null ? this.uncommonEggsFound : 0) + 1;
            case RARE -> this.rareEggsFound = (this.rareEggsFound != null ? this.rareEggsFound : 0) + 1;
            case EPIC -> this.epicEggsFound = (this.epicEggsFound != null ? this.epicEggsFound : 0) + 1;
            case LEGENDARY -> this.legendaryEggsFound = (this.legendaryEggsFound != null ? this.legendaryEggsFound : 0) + 1;
            case MYTHICAL -> this.mythicalEggsFound = (this.mythicalEggsFound != null ? this.mythicalEggsFound : 0) + 1;
        }

        // Update favorite hunting page (simplified logic)
        this.favoriteHuntingPage = pageFound;
    }
}