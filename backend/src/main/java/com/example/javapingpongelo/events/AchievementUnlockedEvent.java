package com.example.javapingpongelo.events;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerAchievement;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * Event published when a player unlocks an achievement.
 * Triggers notifications and dependency unlocking.
 */
@Getter
public class AchievementUnlockedEvent extends ApplicationEvent {
    
    private final Player player;
    private final Achievement achievement;
    private final PlayerAchievement playerAchievement;
    private final UnlockContext context;

    public AchievementUnlockedEvent(Object source, Player player, Achievement achievement, 
                                   PlayerAchievement playerAchievement, UnlockContext context) {
        super(source);
        this.player = player;
        this.achievement = achievement;
        this.playerAchievement = playerAchievement;
        this.context = context != null ? context : new UnlockContext();
    }

    public AchievementUnlockedEvent(Object source, Player player, Achievement achievement, 
                                   PlayerAchievement playerAchievement) {
        this(source, player, achievement, playerAchievement, new UnlockContext());
    }

    /**
     * Context information about how the achievement was unlocked
     */
    @Getter
    public static class UnlockContext {
        private String triggerEvent; // What triggered the unlock (game, rating, etc.)
        private String opponentName; // For contextual achievements
        private String gameDatePlayed; // When the triggering game was played
        private Map<String, Object> additionalData; // Flexible data storage
        private boolean isRecalculation; // True if this is from a recalculation
        private boolean suppressNotifications; // True to skip notifications

        public UnlockContext() {
            this.additionalData = new java.util.HashMap<>();
        }

        public UnlockContext(String triggerEvent) {
            this();
            this.triggerEvent = triggerEvent;
        }

        public UnlockContext withOpponent(String opponentName, String gameDatePlayed) {
            this.opponentName = opponentName;
            this.gameDatePlayed = gameDatePlayed;
            return this;
        }

        public UnlockContext withData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }

        public UnlockContext suppressNotifications() {
            this.suppressNotifications = true;
            return this;
        }

        public UnlockContext asRecalculation() {
            this.isRecalculation = true;
            return this;
        }

        public void setTriggerEvent(String triggerEvent) {
            this.triggerEvent = triggerEvent;
        }

        public void setOpponentName(String opponentName) {
            this.opponentName = opponentName;
        }

        public void setGameDatePlayed(String gameDatePlayed) {
            this.gameDatePlayed = gameDatePlayed;
        }

        public void setAdditionalData(Map<String, Object> additionalData) {
            this.additionalData = additionalData;
        }

        public void setRecalculation(boolean recalculation) {
            isRecalculation = recalculation;
        }

        public void setSuppressNotifications(boolean suppressNotifications) {
            this.suppressNotifications = suppressNotifications;
        }
    }
}