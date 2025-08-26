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
}