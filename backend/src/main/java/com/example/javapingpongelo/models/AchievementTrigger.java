package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Entity that defines when and how achievements should be evaluated.
 * Links achievements to specific trigger events for performance optimization.
 */
@Entity
@Table(name = "achievement_trigger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementTrigger {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
    private Achievement achievement;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @ElementCollection
    @CollectionTable(name = "achievement_trigger_game_types", 
                    joinColumns = @JoinColumn(name = "trigger_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    private List<GameType> gameTypes;

    @Column(columnDefinition = "TEXT")
    private String conditions; // JSON for additional conditions

    /**
     * Types of events that can trigger achievement evaluation
     */
    public enum TriggerType {
        GAME_COMPLETED,    // After any game is finished
        RATING_UPDATED,    // When player ratings change
        STREAK_CHANGED,    // When win/loss streaks change
        MATCH_COMPLETED,   // After match completion
        TOURNAMENT_EVENT,  // Tournament-related events
        EASTER_EGG_FOUND,  // When player finds an easter egg
        MANUAL_TRIGGER,    // For admin-triggered evaluations
        PERIODIC           // For time-based achievements
    }
}