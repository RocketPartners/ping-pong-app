package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.AchievementNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AchievementNotification entities
 */
@Repository
public interface AchievementNotificationRepository extends JpaRepository<AchievementNotification, UUID> {
    
    /**
     * Find notifications by player ID
     */
    List<AchievementNotification> findByPlayerId(UUID playerId);

    /**
     * Find notifications by status
     */
    List<AchievementNotification> findByStatus(AchievementNotification.NotificationStatus status);

    /**
     * Find notifications by type and status
     */
    List<AchievementNotification> findByNotificationTypeAndStatus(
            AchievementNotification.NotificationType notificationType,
            AchievementNotification.NotificationStatus status);

    /**
     * Find pending notifications that need to be sent
     */
    @Query("SELECT an FROM AchievementNotification an " +
           "WHERE an.status = 'PENDING' " +
           "ORDER BY an.createdAt ASC")
    List<AchievementNotification> findPendingNotifications();

    /**
     * Find failed notifications that should be retried
     */
    @Query("SELECT an FROM AchievementNotification an " +
           "WHERE an.status = 'FAILED' " +
           "AND an.retryCount < 3 " +
           "AND an.createdAt > :cutoffTime " +
           "ORDER BY an.createdAt ASC")
    List<AchievementNotification> findNotificationsForRetry(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Check if a notification already exists for a player/achievement/type combination
     */
    Optional<AchievementNotification> findByPlayerIdAndAchievementIdAndNotificationType(
            UUID playerId, UUID achievementId, AchievementNotification.NotificationType notificationType);

    /**
     * Find recent notifications for a player (for deduplication)
     */
    @Query("SELECT an FROM AchievementNotification an " +
           "WHERE an.playerId = :playerId " +
           "AND an.createdAt > :since " +
           "ORDER BY an.createdAt DESC")
    List<AchievementNotification> findRecentNotificationsByPlayer(
            @Param("playerId") UUID playerId, 
            @Param("since") LocalDateTime since);

    /**
     * Find notifications for specific achievement (for analytics)
     */
    List<AchievementNotification> findByAchievementId(UUID achievementId);

    /**
     * Count notifications by status (for monitoring)
     */
    long countByStatus(AchievementNotification.NotificationStatus status);

    /**
     * Find notifications that have been pending too long (for cleanup)
     */
    @Query("SELECT an FROM AchievementNotification an " +
           "WHERE an.status = 'PENDING' " +
           "AND an.createdAt < :cutoffTime")
    List<AchievementNotification> findStaleNotifications(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete old notifications (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffTime);

    /**
     * Get notification statistics
     */
    @Query("SELECT an.notificationType, an.status, COUNT(an) " +
           "FROM AchievementNotification an " +
           "WHERE an.createdAt > :since " +
           "GROUP BY an.notificationType, an.status")
    List<Object[]> getNotificationStatistics(@Param("since") LocalDateTime since);
}