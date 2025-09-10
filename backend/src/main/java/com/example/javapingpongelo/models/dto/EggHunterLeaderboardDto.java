package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO representing a player's position on the secret egg hunters leaderboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggHunterLeaderboardDto {
    private int rank;
    private String playerName;
    private String username;
    private int totalEggsFound;
    private int totalPoints;
    private Date lastEggFound;
    private Date firstEggFound;
    
    // Special achievements for display
    private int commonEggsFound;
    private int uncommonEggsFound;
    private int rareEggsFound;
    private int epicEggsFound;
    private int legendaryEggsFound;
    private int mythicalEggsFound;
    private int longestStreak;
    private String favoriteHuntingPage;
    
    // Special badges based on achievements
    private String specialBadge;       // "Rare Hunter", "Legend Seeker", etc.
    private String badgeEmoji;         // Visual badge emoji
    
    /**
     * Calculate and assign special badge based on stats
     */
    public void assignSpecialBadge() {
        if (mythicalEggsFound > 0) {
            specialBadge = "Mythical Master";
            badgeEmoji = "🔮";
        } else if (legendaryEggsFound >= 5) {
            specialBadge = "Legend Master";
            badgeEmoji = "✨";
        } else if (legendaryEggsFound > 0) {
            specialBadge = "Legend Seeker";
            badgeEmoji = "⭐";
        } else if (epicEggsFound >= 3) {
            specialBadge = "Epic Collector";
            badgeEmoji = "💎";
        } else if (epicEggsFound > 0) {
            specialBadge = "Epic Hunter";
            badgeEmoji = "💠";
        } else if (rareEggsFound >= 10) {
            specialBadge = "Rainbow Expert";
            badgeEmoji = "🌈";
        } else if (rareEggsFound > 0) {
            specialBadge = "Rare Hunter";
            badgeEmoji = "🔍";
        } else if (totalEggsFound >= 50) {
            specialBadge = "Egg Master";
            badgeEmoji = "🏆";
        } else if (totalEggsFound >= 10) {
            specialBadge = "Experienced Hunter";
            badgeEmoji = "🥇";
        } else {
            specialBadge = "Novice Hunter";
            badgeEmoji = "🥚";
        }
    }
    
    /**
     * Get hunting efficiency (points per egg)
     */
    public double getEfficiency() {
        return totalEggsFound > 0 ? (double) totalPoints / totalEggsFound : 0.0;
    }
}