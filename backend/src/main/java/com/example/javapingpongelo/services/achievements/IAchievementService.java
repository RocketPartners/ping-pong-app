package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerAchievement;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for achievement-related operations
 */
public interface IAchievementService {

    /**
     * Retrieve all achievements
     */
    List<Achievement> findAllAchievements();

    /**
     * Retrieve visible achievements only
     */
    List<Achievement> findVisibleAchievements();

    /**
     * Create a new achievement
     */
    Achievement createAchievement(Achievement achievement);

    /**
     * Find achievement by ID
     */
    Achievement findAchievementById(UUID id);

    /**
     * Find a player's achievements
     */
    List<PlayerAchievement> findPlayerAchievements(UUID playerId);

    /**
     * Find a player's achieved achievements
     */
    List<PlayerAchievement> findPlayerAchievedAchievements(UUID playerId);

    /**
     * Initialize achievements for a new player
     */
    void initializePlayerAchievements(UUID playerId);

    /**
     * Mark an achievement as achieved for a player
     */
    void achieveAchievement(UUID playerId, UUID achievementId);

    /**
     * Update progress for an achievement
     */
    PlayerAchievement updateAchievementProgress(UUID playerId, UUID achievementId, int progressValue);

    /**
     * Update progress for a contextual achievement (like Gilyed) with opponent info
     */
    PlayerAchievement updateAchievementProgress(UUID playerId, UUID achievementId, int progressValue, 
                                               String opponentName, java.util.Date gameDatePlayed);

    /**
     * Evaluate achievements after a game is played
     */
    void evaluateAchievementsForGame(Game game, Player... players);

    /**
     * Recalculate all achievements for a player
     */
    void recalculatePlayerAchievements(UUID playerId);

    /**
     * Get all achievements (alias for findAllAchievements)
     */
    List<Achievement> getAllAchievements();

    /**
     * Evaluate all achievements for a specific player
     */
    void evaluateAllAchievementsForPlayer(UUID playerId);

    /**
     * Reset all player achievement progress (DANGEROUS - admin only)
     */
    void resetAllPlayerProgress();

    /**
     * Get recent achievement notifications for a player
     */
    List<Map<String, Object>> getRecentAchievementNotifications(UUID playerId, int days);

    /**
     * Acknowledge/mark achievement notifications as read for a player
     */
    void acknowledgeAchievementNotifications(UUID playerId);
}