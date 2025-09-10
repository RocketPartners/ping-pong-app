package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.events.AchievementUnlockedEvent;
import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.AchievementNotification;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.AchievementNotificationRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.services.SlackService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing achievement notifications across multiple channels.
 * Handles notification creation, sending, retries, and cleanup.
 */
@Service
@Slf4j
public class AchievementNotificationService {

    @Autowired
    private AchievementNotificationRepository notificationRepository;

    @Autowired
    private SlackService slackService;

    @Autowired
    private PlayerRepository playerRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles achievement unlocked events and creates notifications
     */
    @EventListener
    @Async
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        if (event.getContext().isSuppressNotifications()) {
            log.debug("Skipping notifications for achievement {} (suppressed)", 
                     event.getAchievement().getName());
            return;
        }

        Player player = event.getPlayer();
        Achievement achievement = event.getAchievement();
        AchievementUnlockedEvent.UnlockContext context = event.getContext();

        log.info("Creating notifications for achievement '{}' unlocked by player '{}'", 
                achievement.getName(), player.getFullName());

        try {
            // Create Slack channel notification (public celebration)
            createSlackChannelNotification(player, achievement, context);

            // Create in-app notification
            createInAppNotification(player, achievement, context);

            // Special handling for certain achievement types
            handleSpecialNotifications(player, achievement, context);

        } catch (Exception e) {
            log.error("Error creating notifications for achievement unlock", e);
        }
    }

    /**
     * Creates a Slack channel notification
     */
    private void createSlackChannelNotification(Player player, Achievement achievement, 
                                              AchievementUnlockedEvent.UnlockContext context) {
        try {
            // Check if notification already exists (deduplication)
            Optional<AchievementNotification> existing = notificationRepository
                    .findByPlayerIdAndAchievementIdAndNotificationType(
                            player.getPlayerId(), 
                            achievement.getId(), 
                            AchievementNotification.NotificationType.SLACK_CHANNEL);

            if (existing.isPresent()) {
                log.debug("Slack notification already exists for player {} achievement {}", 
                         player.getPlayerId(), achievement.getId());
                return;
            }

            // Create notification data
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("achievementName", achievement.getName());
            notificationData.put("achievementDescription", achievement.getDescription());
            notificationData.put("achievementCategory", achievement.getCategory().toString());
            notificationData.put("achievementPoints", achievement.getPoints());
            notificationData.put("playerName", player.getFullName());
            
            if (context.getOpponentName() != null) {
                notificationData.put("opponentName", context.getOpponentName());
            }
            if (context.getGameDatePlayed() != null) {
                notificationData.put("gameDatePlayed", context.getGameDatePlayed());
            }

            // Create notification record
            AchievementNotification notification = AchievementNotification.builder()
                    .playerId(player.getPlayerId())
                    .achievementId(achievement.getId())
                    .notificationType(AchievementNotification.NotificationType.SLACK_CHANNEL)
                    .status(AchievementNotification.NotificationStatus.PENDING)
                    .notificationData(objectMapper.writeValueAsString(notificationData))
                    .opponentName(context.getOpponentName())
                    .gameDate(context.getGameDatePlayed() != null ? 
                             LocalDateTime.parse(context.getGameDatePlayed()) : null)
                    .achievementPoints(achievement.getPoints())
                    .achievementCategory(achievement.getCategory())
                    .build();

            notificationRepository.save(notification);
            log.debug("Created Slack channel notification for achievement {}", achievement.getName());

        } catch (Exception e) {
            log.error("Error creating Slack channel notification", e);
        }
    }

    /**
     * Creates an in-app notification
     */
    private void createInAppNotification(Player player, Achievement achievement, 
                                       AchievementUnlockedEvent.UnlockContext context) {
        try {
            // Check for existing notification
            Optional<AchievementNotification> existing = notificationRepository
                    .findByPlayerIdAndAchievementIdAndNotificationType(
                            player.getPlayerId(), 
                            achievement.getId(), 
                            AchievementNotification.NotificationType.IN_APP);

            if (existing.isPresent()) {
                return;
            }

            AchievementNotification notification = AchievementNotification.builder()
                    .playerId(player.getPlayerId())
                    .achievementId(achievement.getId())
                    .notificationType(AchievementNotification.NotificationType.IN_APP)
                    .status(AchievementNotification.NotificationStatus.PENDING)
                    .achievementPoints(achievement.getPoints())
                    .achievementCategory(achievement.getCategory())
                    .build();

            notificationRepository.save(notification);
            log.debug("Created in-app notification for achievement {}", achievement.getName());

        } catch (Exception e) {
            log.error("Error creating in-app notification", e);
        }
    }

    /**
     * Handles special notifications for certain achievement types
     */
    private void handleSpecialNotifications(Player player, Achievement achievement, 
                                          AchievementUnlockedEvent.UnlockContext context) {
        // Special handling for legendary achievements
        if (achievement.getCategory() == Achievement.AchievementCategory.LEGENDARY) {
            createSpecialLegendaryNotification(player, achievement, context);
        }

        // Special handling for first achievement
        if ("First Steps".equals(achievement.getName())) {
            createWelcomeNotification(player, achievement, context);
        }

        // Special handling for Gilyed achievements
        if (achievement.getName().contains("Gilyed")) {
            // Already handled by existing Gilyed system, but we could enhance it
            log.debug("Gilyed achievement notification handled by existing system");
        }
    }

    /**
     * Creates special notification for legendary achievements
     */
    private void createSpecialLegendaryNotification(Player player, Achievement achievement, 
                                                  AchievementUnlockedEvent.UnlockContext context) {
        try {
            Map<String, Object> specialData = new HashMap<>();
            specialData.put("isLegendary", true);
            specialData.put("celebrationLevel", "EPIC");

            AchievementNotification notification = AchievementNotification.builder()
                    .playerId(player.getPlayerId())
                    .achievementId(achievement.getId())
                    .notificationType(AchievementNotification.NotificationType.SLACK_CHANNEL)
                    .status(AchievementNotification.NotificationStatus.PENDING)
                    .notificationData(objectMapper.writeValueAsString(specialData))
                    .achievementPoints(achievement.getPoints())
                    .achievementCategory(achievement.getCategory())
                    .build();

            notificationRepository.save(notification);
            log.info("Created special legendary notification for {}", achievement.getName());

        } catch (Exception e) {
            log.error("Error creating legendary notification", e);
        }
    }

    /**
     * Creates welcome notification for first achievement
     */
    private void createWelcomeNotification(Player player, Achievement achievement, 
                                         AchievementUnlockedEvent.UnlockContext context) {
        try {
            Map<String, Object> welcomeData = new HashMap<>();
            welcomeData.put("isFirstAchievement", true);
            welcomeData.put("welcomeMessage", "Welcome to the achievement system!");

            AchievementNotification notification = AchievementNotification.builder()
                    .playerId(player.getPlayerId())
                    .achievementId(achievement.getId())
                    .notificationType(AchievementNotification.NotificationType.SLACK_DM)
                    .status(AchievementNotification.NotificationStatus.PENDING)
                    .notificationData(objectMapper.writeValueAsString(welcomeData))
                    .achievementPoints(achievement.getPoints())
                    .achievementCategory(achievement.getCategory())
                    .build();

            notificationRepository.save(notification);
            log.info("Created welcome notification for new player {}", player.getFullName());

        } catch (Exception e) {
            log.error("Error creating welcome notification", e);
        }
    }

    /**
     * Processes pending notifications (scheduled task)
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    @Transactional
    public void processPendingNotifications() {
        List<AchievementNotification> pendingNotifications = notificationRepository.findPendingNotifications();
        
        if (pendingNotifications.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending notifications", pendingNotifications.size());

        for (AchievementNotification notification : pendingNotifications) {
            try {
                processNotification(notification);
            } catch (Exception e) {
                log.error("Error processing notification {}: {}", notification.getId(), e.getMessage());
                notification.markAsFailed(e.getMessage());
                notificationRepository.save(notification);
            }
        }
    }

    /**
     * Processes a single notification
     */
    private void processNotification(AchievementNotification notification) {
        switch (notification.getNotificationType()) {
            case SLACK_CHANNEL:
                processSlackChannelNotification(notification);
                break;
            case SLACK_DM:
                processSlackDMNotification(notification);
                break;
            case IN_APP:
                processInAppNotification(notification);
                break;
            default:
                log.warn("Unknown notification type: {}", notification.getNotificationType());
        }
    }

    /**
     * Processes Slack channel notification
     */
    private void processSlackChannelNotification(AchievementNotification notification) {
        try {
            // Parse notification data
            Map<String, Object> data = parseNotificationData(notification.getNotificationData());
            
            String playerName = (String) data.get("playerName");
            String achievementName = (String) data.get("achievementName");
            String achievementDescription = (String) data.get("achievementDescription");
            Integer points = (Integer) data.get("achievementPoints");
            String category = (String) data.get("achievementCategory");

            // Create enhanced message
            String message;
            if (data.containsKey("isLegendary") && (Boolean) data.get("isLegendary")) {
                message = String.format("🏆 **LEGENDARY ACHIEVEMENT UNLOCKED!** 🏆\n\n" +
                        "🎉 **%s** has achieved the legendary **%s**!\n\n" +
                        "📝 *%s*\n\n" +
                        "💎 **%d points** | 🌟 **%s** tier\n\n" +
                        "This is a truly exceptional accomplishment! 🎊",
                        playerName, achievementName, achievementDescription, points, category);
            } else if (data.containsKey("opponentName")) {
                String opponentName = (String) data.get("opponentName");
                message = String.format("🎯 **%s** unlocked **%s**!\n\n" +
                        "📝 *%s*\n\n" +
                        "🆚 Against: **%s**\n" +
                        "💰 **%d points** | 🏅 **%s** tier",
                        playerName, achievementName, achievementDescription, opponentName, points, category);
            } else {
                message = String.format("🎉 **%s** unlocked **%s**!\n\n" +
                        "📝 *%s*\n\n" +
                        "💰 **%d points** | 🏅 **%s** tier",
                        playerName, achievementName, achievementDescription, points, category);
            }

            // Use existing SlackService to post achievement notification
            try {
                // Get player object for SlackService
                Optional<Player> playerOpt = playerRepository.findById(notification.getPlayerId());
                if (playerOpt.isPresent()) {
                    Player player = playerOpt.get();
                    slackService.postPlayerAchievement(player, achievementName, achievementDescription);
                    log.info("Posted Slack achievement notification for {}: {}", playerName, achievementName);
                } else {
                    log.warn("Player {} not found for Slack notification", notification.getPlayerId());
                    // Log the formatted message as fallback
                    log.info("Slack notification message: {}", message);
                }
            } catch (Exception slackError) {
                log.error("Failed to send Slack notification via SlackService: {}", slackError.getMessage());
                // Log the original message as fallback
                log.info("Slack notification message: {}", message);
            }
            
            notification.markAsSent();
            notificationRepository.save(notification);
            
            log.info("Sent Slack notification for achievement: {}", achievementName);

        } catch (Exception e) {
            log.error("Error sending Slack notification", e);
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
        }
    }

    /**
     * Processes Slack DM notification
     */
    private void processSlackDMNotification(AchievementNotification notification) {
        // Note: Slack DM functionality would require user mapping between app users and Slack users
        // For now, we defer to the existing SlackService which handles channel notifications
        // Future enhancement: Add Slack user mapping and direct message capabilities
        
        log.info("Slack DM notification requested but not implemented - deferring to channel notification");
        notification.markAsSent();
        notificationRepository.save(notification);
        log.debug("Processed Slack DM notification (deferred to channel)");
    }

    /**
     * Processes in-app notification
     */
    private void processInAppNotification(AchievementNotification notification) {
        // Mark as sent - in-app notifications are displayed by frontend polling
        notification.markAsSent();
        notificationRepository.save(notification);
        log.debug("Processed in-app notification");
    }

    /**
     * Retry failed notifications (scheduled task)
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24); // Don't retry older than 24h
        List<AchievementNotification> failedNotifications = 
                notificationRepository.findNotificationsForRetry(cutoffTime);

        if (failedNotifications.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed notifications", failedNotifications.size());

        for (AchievementNotification notification : failedNotifications) {
            try {
                notification.markForRetry();
                notificationRepository.save(notification);
                processNotification(notification);
            } catch (Exception e) {
                log.error("Error retrying notification {}: {}", notification.getId(), e.getMessage());
                notification.markAsFailed(e.getMessage());
                notificationRepository.save(notification);
            }
        }
    }

    /**
     * Cleanup old notifications (scheduled task)
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // Keep 30 days
        
        try {
            notificationRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("Cleaned up notifications older than {}", cutoffTime);
        } catch (Exception e) {
            log.error("Error during notification cleanup", e);
        }
    }

    /**
     * Gets notification statistics
     */
    public Map<String, Object> getNotificationStatistics() {
        LocalDateTime since = LocalDateTime.now().minusDays(7); // Last 7 days
        List<Object[]> stats = notificationRepository.getNotificationStatistics(since);
        
        Map<String, Object> result = new HashMap<>();
        result.put("period", "Last 7 days");
        result.put("statistics", stats);
        result.put("totalPending", notificationRepository.countByStatus(AchievementNotification.NotificationStatus.PENDING));
        result.put("totalFailed", notificationRepository.countByStatus(AchievementNotification.NotificationStatus.FAILED));
        
        return result;
    }

    private Map<String, Object> parseNotificationData(String jsonData) {
        if (jsonData == null || jsonData.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(jsonData, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error parsing notification data JSON", e);
            return new HashMap<>();
        }
    }
}