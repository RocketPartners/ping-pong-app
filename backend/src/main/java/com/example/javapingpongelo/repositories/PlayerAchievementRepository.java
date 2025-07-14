package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.PlayerAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerAchievementRepository extends JpaRepository<PlayerAchievement, UUID> {
    /**
     * Find all achievements for a player
     */
    List<PlayerAchievement> findByPlayerId(UUID playerId);

    /**
     * Find all players who have earned a specific achievement
     */
    List<PlayerAchievement> findByAchievementIdAndAchievedTrue(UUID achievementId);

    /**
     * Find a specific player's achievement
     */
    Optional<PlayerAchievement> findByPlayerIdAndAchievementId(UUID playerId, UUID achievementId);

    /**
     * Get count of achieved achievements for a player
     */
    @Query("SELECT COUNT(pa) FROM PlayerAchievement pa WHERE pa.playerId = :playerId AND pa.achieved = true")
    int countAchievedAchievements(@Param("playerId") UUID playerId);

    /**
     * Find all achieved achievements for a player
     */
    List<PlayerAchievement> findByPlayerIdAndAchievedTrue(UUID playerId);

    /**
     * Find all in-progress (not yet achieved) achievements for a player
     */
    List<PlayerAchievement> findByPlayerIdAndAchievedFalse(UUID playerId);
}