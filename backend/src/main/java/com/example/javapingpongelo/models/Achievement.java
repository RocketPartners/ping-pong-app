package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity that represents an achievement that players can earn.
 * Contains achievement details, criteria, and metadata.
 */
@Entity
@Table(name = "achievement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AchievementType type;

    @Column(columnDefinition = "TEXT")
    private String criteria;

    private String icon;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private Boolean isVisible;

    @Column(nullable = true)
    @Builder.Default
    private Boolean deprecated = false;

    /**
     * Achievement categories representing difficulty levels
     */
    public enum AchievementCategory {
        EASY,
        MEDIUM,
        HARD,
        LEGENDARY
    }

    /**
     * Achievement types
     */
    public enum AchievementType {
        /**
         * One-time achievement that is earned once and cannot progress
         */
        ONE_TIME,

        /**
         * Achievement with progress tracking (e.g., "Win 10 games")
         */
        PROGRESSIVE
    }
}