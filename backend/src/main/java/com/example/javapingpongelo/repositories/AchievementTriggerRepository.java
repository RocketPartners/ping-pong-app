package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.AchievementTrigger;
import com.example.javapingpongelo.models.GameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AchievementTrigger entities
 */
@Repository
public interface AchievementTriggerRepository extends JpaRepository<AchievementTrigger, UUID> {
    
    /**
     * Find all triggers for a specific trigger type
     */
    List<AchievementTrigger> findByTriggerType(AchievementTrigger.TriggerType triggerType);

    /**
     * Find triggers by trigger type and game type
     */
    @Query("SELECT at FROM AchievementTrigger at " +
           "WHERE at.triggerType = :triggerType " +
           "AND (at.gameTypes IS EMPTY OR :gameType MEMBER OF at.gameTypes)")
    List<AchievementTrigger> findByTriggerTypeAndGameType(
            @Param("triggerType") AchievementTrigger.TriggerType triggerType,
            @Param("gameType") GameType gameType);

    /**
     * Find all triggers for a specific achievement
     */
    List<AchievementTrigger> findByAchievementId(UUID achievementId);

    /**
     * Find triggers that apply to all game types (empty gameTypes list)
     */
    @Query("SELECT at FROM AchievementTrigger at WHERE at.gameTypes IS EMPTY")
    List<AchievementTrigger> findTriggersForAllGameTypes();
}