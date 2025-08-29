package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.AchievementAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AchievementAnalytics entities
 */
@Repository
public interface AchievementAnalyticsRepository extends JpaRepository<AchievementAnalytics, UUID> {
    
    /**
     * Find analytics by achievement ID
     */
    Optional<AchievementAnalytics> findByAchievementId(UUID achievementId);

    /**
     * Find achievements with completion rate below threshold
     */
    List<AchievementAnalytics> findByCompletionRateLessThan(BigDecimal threshold);

    /**
     * Find achievements with completion rate above threshold
     */
    List<AchievementAnalytics> findByCompletionRateGreaterThan(BigDecimal threshold);

    /**
     * Find achievements by calculated difficulty
     */
    List<AchievementAnalytics> findByCalculatedDifficulty(AchievementAnalytics.CalculatedDifficulty difficulty);

    /**
     * Find achievements that need analytics recalculation
     */
    @Query("SELECT aa FROM AchievementAnalytics aa WHERE aa.lastCalculated < :cutoffTime")
    List<AchievementAnalytics> findStaleAnalytics(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find top performing achievements by completion rate
     */
    @Query("SELECT aa FROM AchievementAnalytics aa " +
           "WHERE aa.totalPlayersAttempted >= :minAttempts " +
           "ORDER BY aa.completionRate DESC")
    List<AchievementAnalytics> findTopPerformingAchievements(@Param("minAttempts") int minAttempts);

    /**
     * Find underperforming achievements
     */
    @Query("SELECT aa FROM AchievementAnalytics aa " +
           "WHERE aa.completionRate < :maxRate " +
           "AND aa.totalPlayersAttempted >= :minAttempts " +
           "ORDER BY aa.completionRate ASC")
    List<AchievementAnalytics> findUnderperformingAchievements(
            @Param("maxRate") BigDecimal maxRate, 
            @Param("minAttempts") int minAttempts);

    /**
     * Find achievements with increasing completion trend
     */
    List<AchievementAnalytics> findByCompletionTrend(String trend);

    /**
     * Get analytics summary statistics
     */
    @Query("SELECT " +
           "COUNT(aa), " +
           "AVG(aa.completionRate), " +
           "AVG(aa.difficultyScore), " +
           "AVG(aa.averageCompletionTimeHours) " +
           "FROM AchievementAnalytics aa " +
           "WHERE aa.totalPlayersAttempted >= :minAttempts")
    Object[] getAnalyticsSummary(@Param("minAttempts") int minAttempts);

    /**
     * Find achievements by difficulty score range
     */
    @Query("SELECT aa FROM AchievementAnalytics aa " +
           "WHERE aa.difficultyScore BETWEEN :minScore AND :maxScore " +
           "ORDER BY aa.difficultyScore DESC")
    List<AchievementAnalytics> findByDifficultyScoreRange(
            @Param("minScore") BigDecimal minScore, 
            @Param("maxScore") BigDecimal maxScore);

    /**
     * Find achievements with high player engagement
     */
    @Query("SELECT aa FROM AchievementAnalytics aa " +
           "WHERE aa.activePlayersPursuing > :minActive " +
           "ORDER BY aa.activePlayersPursuing DESC")
    List<AchievementAnalytics> findHighEngagementAchievements(@Param("minActive") int minActive);

    /**
     * Get completion rate distribution
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN aa.completionRate >= 80 THEN 'HIGH' " +
           "  WHEN aa.completionRate >= 40 THEN 'MEDIUM' " +
           "  ELSE 'LOW' " +
           "END as rate_category, " +
           "COUNT(aa) " +
           "FROM AchievementAnalytics aa " +
           "WHERE aa.totalPlayersAttempted >= :minAttempts " +
           "GROUP BY rate_category")
    List<Object[]> getCompletionRateDistribution(@Param("minAttempts") int minAttempts);

    /**
     * Find achievements needing balance adjustment
     */
    @Query("SELECT aa FROM AchievementAnalytics aa " +
           "WHERE (aa.completionRate < 5 OR aa.completionRate > 95) " +
           "AND aa.totalPlayersAttempted >= :minAttempts " +
           "ORDER BY aa.completionRate ASC")
    List<AchievementAnalytics> findAchievementsNeedingBalance(@Param("minAttempts") int minAttempts);

    /**
     * Get recent completion trends
     */
    @Query("SELECT aa.completionTrend, COUNT(aa) " +
           "FROM AchievementAnalytics aa " +
           "WHERE aa.lastCalculated > :since " +
           "GROUP BY aa.completionTrend")
    List<Object[]> getCompletionTrends(@Param("since") LocalDateTime since);
}