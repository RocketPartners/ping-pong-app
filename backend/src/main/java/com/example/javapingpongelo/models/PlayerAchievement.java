package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Entity that represents a player's progress toward or completion of an achievement.
 * Links players to achievements and tracks their progress.
 */
@Entity
@Table(name = "player_achievement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "achievement_id"}),
        indexes = {
                @Index(name = "idx_achievement_player", columnList = "player_id"),
                @Index(name = "idx_achievement_type", columnList = "achievement_id"),
                @Index(name = "idx_achievement_status", columnList = "achieved")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @Column(nullable = false)
    private Boolean achieved;

    @Column(nullable = false)
    private Integer progress;

    // New progress tracking fields
    @Column(name = "current_value")
    private Integer currentValue;

    @Column(name = "target_value")  
    private Integer targetValue;

    @Column(columnDefinition = "TEXT")
    private String progressData; // JSON for complex progress tracking

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateEarned;

    // Fields for contextual achievements like Gilyed
    @Column(length = 100)
    private String opponentName;

    @Temporal(TemporalType.TIMESTAMP)
    private Date gameDatePlayed;

    @PrePersist
    protected void onCreate() {
        if (this.progress == null) {
            this.progress = 0;
        }
        if (this.achieved == null) {
            this.achieved = false;
        }
        if (this.currentValue == null) {
            this.currentValue = 0;
        }
        if (this.targetValue == null) {
            this.targetValue = 1;
        }
    }

    /**
     * Updates progress and marks achievement as achieved if applicable
     *
     * @param value     The progress value to add
     * @param threshold The threshold to reach for achievement
     * @return True if the achievement was just achieved, false otherwise
     */
    public boolean updateProgress(int value, int threshold) {
        if (achieved) {
            return false;
        }

        this.progress += value;

        if (this.progress >= threshold) {
            this.achieved = true;
            this.dateEarned = new Date();
            return true;
        }

        return false;
    }

    /**
     * Sets the current value and target for real progress tracking
     *
     * @param currentValue The current progress value
     * @param targetValue  The target value to reach
     * @return True if the achievement was just achieved, false otherwise
     */
    public boolean setProgress(int currentValue, int targetValue) {
        if (achieved) {
            return false;
        }

        this.currentValue = currentValue;
        this.targetValue = targetValue;
        this.progress = targetValue > 0 ? (int) ((currentValue * 100.0) / targetValue) : 0;

        if (currentValue >= targetValue) {
            this.achieved = true;
            this.dateEarned = new Date();
            return true;
        }

        return false;
    }

    /**
     * Gets the progress percentage (0-100)
     */
    public int getProgressPercentage() {
        if (targetValue == null || targetValue <= 0) {
            return progress;
        }
        if (currentValue == null) {
            return 0;
        }
        return Math.min(100, (int) ((currentValue * 100.0) / targetValue));
    }
}