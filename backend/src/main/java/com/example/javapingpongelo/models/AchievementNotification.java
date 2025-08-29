package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity that tracks achievement notifications sent to players.
 * Stores notification history and status for different notification types.
 */
@Entity
@Table(name = "achievement_notification",
       indexes = {
           @Index(name = "idx_achievement_notif_player", columnList = "player_id"),
           @Index(name = "idx_achievement_notif_achievement", columnList = "achievement_id"),
           @Index(name = "idx_achievement_notif_status", columnList = "status"),
           @Index(name = "idx_achievement_notif_type", columnList = "notification_type"),
           @Index(name = "idx_achievement_notif_sent_at", columnList = "sent_at")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
    private Achievement achievement;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String notificationData; // JSON for notification-specific data

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Error details if sending failed

    // Context information for rich notifications
    @Column(name = "opponent_name")
    private String opponentName;

    @Column(name = "game_date")
    private LocalDateTime gameDate;

    @Column(name = "achievement_points")
    private Integer achievementPoints;

    @Column(name = "achievement_category")
    @Enumerated(EnumType.STRING)
    private Achievement.AchievementCategory achievementCategory;

    /**
     * Types of notifications that can be sent
     */
    public enum NotificationType {
        SLACK_CHANNEL,      // Public Slack channel notification
        SLACK_DM,          // Private Slack DM to player
        EMAIL,             // Email notification
        IN_APP,            // In-app notification
        WEBHOOK,           // Custom webhook
        PUSH_NOTIFICATION  // Mobile push notification (future)
    }

    /**
     * Status of notification delivery
     */
    public enum NotificationStatus {
        PENDING,    // Queued for sending
        SENT,       // Successfully sent
        FAILED,     // Failed to send
        RETRYING,   // Being retried
        CANCELLED   // Cancelled (e.g., player opted out)
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }

    /**
     * Marks notification as sent
     */
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }

    /**
     * Marks notification as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }

    /**
     * Marks notification for retry
     */
    public void markForRetry() {
        this.status = NotificationStatus.RETRYING;
        this.retryCount++;
    }

    /**
     * Checks if notification should be retried
     */
    public boolean shouldRetry() {
        return status == NotificationStatus.FAILED && retryCount < 3;
    }
}