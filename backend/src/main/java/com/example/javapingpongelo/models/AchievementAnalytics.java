package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity that stores analytics data for achievements.
 * Tracks completion rates, difficulty metrics, and player engagement.
 */
@Entity
@Table(name = "achievement_analytics",
       indexes = {
           @Index(name = "idx_achievement_analytics_achievement", columnList = "achievement_id"),
           @Index(name = "idx_achievement_analytics_completion_rate", columnList = "completion_rate"),
           @Index(name = "idx_achievement_analytics_calculated", columnList = "last_calculated")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "achievement_id", nullable = false, unique = true)
    private UUID achievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
    private Achievement achievement;

    // Completion metrics
    @Column(name = "total_completions", nullable = false)
    @Builder.Default
    private Integer totalCompletions = 0;

    @Column(name = "total_players_attempted", nullable = false)
    @Builder.Default
    private Integer totalPlayersAttempted = 0;

    @Column(name = "completion_rate", precision = 5, scale = 2)
    private BigDecimal completionRate; // Percentage (0.00 - 100.00)

    // Timing metrics
    @Column(name = "average_completion_time_hours")
    private Integer averageCompletionTimeHours;

    @Column(name = "fastest_completion_time_hours")
    private Integer fastestCompletionTimeHours;

    @Column(name = "slowest_completion_time_hours")
    private Integer slowestCompletionTimeHours;

    // Difficulty metrics
    @Column(name = "difficulty_score", precision = 3, scale = 1)
    private BigDecimal difficultyScore; // 1.0 (easy) to 10.0 (legendary)

    @Enumerated(EnumType.STRING)
    @Column(name = "calculated_difficulty")
    private CalculatedDifficulty calculatedDifficulty;

    // Engagement metrics
    @Column(name = "active_players_pursuing")
    @Builder.Default
    private Integer activePlayersPursuing = 0;

    @Column(name = "players_with_progress")
    @Builder.Default
    private Integer playersWithProgress = 0;

    @Column(name = "average_progress_percentage", precision = 5, scale = 2)
    private BigDecimal averageProgressPercentage;

    // Trend metrics
    @Column(name = "completions_last_7_days")
    @Builder.Default
    private Integer completionsLast7Days = 0;

    @Column(name = "completions_last_30_days")
    @Builder.Default
    private Integer completionsLast30Days = 0;

    @Column(name = "completion_trend")
    @Builder.Default
    private String completionTrend = "STABLE"; // INCREASING, DECREASING, STABLE

    // Quality metrics
    @Column(name = "player_satisfaction_score", precision = 3, scale = 2)
    private BigDecimal playerSatisfactionScore; // Based on feedback/engagement

    @Column(name = "achievement_value_score", precision = 3, scale = 2)
    private BigDecimal achievementValueScore; // Points/difficulty ratio

    // Timestamps
    @Column(name = "last_calculated", nullable = false)
    @Builder.Default
    private LocalDateTime lastCalculated = LocalDateTime.now();

    @Column(name = "first_completion_date")
    private LocalDateTime firstCompletionDate;

    @Column(name = "latest_completion_date")
    private LocalDateTime latestCompletionDate;

    // Additional metadata
    @Column(columnDefinition = "TEXT")
    private String analyticsMetadata; // JSON for additional metrics

    /**
     * Calculated difficulty levels based on completion rates and timing
     */
    public enum CalculatedDifficulty {
        TRIVIAL,     // >80% completion rate
        EASY,        // 60-80% completion rate
        MODERATE,    // 40-60% completion rate
        HARD,        // 20-40% completion rate
        VERY_HARD,   // 5-20% completion rate
        LEGENDARY    // <5% completion rate
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastCalculated = LocalDateTime.now();
        calculateDifficultyScore();
        calculateValueScore();
    }

    /**
     * Calculates difficulty score based on completion rate and timing
     */
    private void calculateDifficultyScore() {
        if (completionRate == null) {
            return;
        }

        double rate = completionRate.doubleValue();
        double baseScore;

        if (rate >= 80) {
            baseScore = 1.0 + (100 - rate) / 20 * 2; // 1.0 - 3.0
            this.calculatedDifficulty = CalculatedDifficulty.TRIVIAL;
        } else if (rate >= 60) {
            baseScore = 3.0 + (80 - rate) / 20 * 1; // 3.0 - 4.0
            this.calculatedDifficulty = CalculatedDifficulty.EASY;
        } else if (rate >= 40) {
            baseScore = 4.0 + (60 - rate) / 20 * 2; // 4.0 - 6.0
            this.calculatedDifficulty = CalculatedDifficulty.MODERATE;
        } else if (rate >= 20) {
            baseScore = 6.0 + (40 - rate) / 20 * 2; // 6.0 - 8.0
            this.calculatedDifficulty = CalculatedDifficulty.HARD;
        } else if (rate >= 5) {
            baseScore = 8.0 + (20 - rate) / 15 * 1; // 8.0 - 9.0
            this.calculatedDifficulty = CalculatedDifficulty.VERY_HARD;
        } else {
            baseScore = 9.0 + (5 - rate) / 5 * 1; // 9.0 - 10.0
            this.calculatedDifficulty = CalculatedDifficulty.LEGENDARY;
        }

        // Adjust based on completion time if available
        if (averageCompletionTimeHours != null && averageCompletionTimeHours > 0) {
            double timeMultiplier = Math.min(1.2, 1.0 + (averageCompletionTimeHours / 168.0) * 0.2); // Max 20% increase for week+
            baseScore *= timeMultiplier;
        }

        this.difficultyScore = BigDecimal.valueOf(Math.min(10.0, baseScore));
    }

    /**
     * Calculates value score (reward points vs difficulty)
     */
    private void calculateValueScore() {
        if (achievement == null || difficultyScore == null) {
            return;
        }

        double expectedPoints = difficultyScore.doubleValue() * 20; // Expected points for difficulty
        double actualPoints = achievement.getPoints();
        double ratio = actualPoints / expectedPoints;

        // Score from 0-5, where 3 is perfect balance
        this.achievementValueScore = BigDecimal.valueOf(Math.min(5.0, ratio * 3.0));
    }

    /**
     * Updates completion trend based on recent completions
     */
    public void updateCompletionTrend() {
        if (completionsLast7Days == 0 && completionsLast30Days == 0) {
            this.completionTrend = "STABLE";
            return;
        }

        double weeklyRate = completionsLast7Days / 7.0;
        double monthlyRate = completionsLast30Days / 30.0;

        if (weeklyRate > monthlyRate * 1.5) {
            this.completionTrend = "INCREASING";
        } else if (weeklyRate < monthlyRate * 0.5) {
            this.completionTrend = "DECREASING";
        } else {
            this.completionTrend = "STABLE";
        }
    }

    /**
     * Checks if the achievement is underperforming (low completion, high time)
     */
    public boolean isUnderperforming() {
        return completionRate != null && 
               completionRate.compareTo(BigDecimal.valueOf(10)) < 0 && // <10% completion
               averageCompletionTimeHours != null && 
               averageCompletionTimeHours > 168; // >1 week average
    }

    /**
     * Checks if the achievement is too easy (high completion, low time)
     */
    public boolean isTooEasy() {
        return completionRate != null && 
               completionRate.compareTo(BigDecimal.valueOf(90)) > 0 && // >90% completion
               averageCompletionTimeHours != null && 
               averageCompletionTimeHours < 1; // <1 hour average
    }
}