package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Entity representing an easter egg hidden in the application.
 * Easter eggs are hidden clickable elements that players can find for points.
 */
@Entity
@Table(name = "easter_egg", indexes = {
    @Index(name = "idx_egg_active", columnList = "isActive"),
    @Index(name = "idx_egg_page", columnList = "pageLocation"),
    @Index(name = "idx_egg_spawned", columnList = "spawnedAt"),
    @Index(name = "idx_egg_active_page", columnList = "isActive,pageLocation")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EasterEgg {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String pageLocation;           // "home", "leaderboard", "achievements", etc.

    @Column(nullable = false)
    private String cssSelector;            // CSS selector for placement zone

    @Column(columnDefinition = "TEXT")
    private String coordinates;            // JSON: {"x": 50, "y": 200, "zIndex": 1000}

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EggType type;                  // COMMON, RARE, LEGENDARY, ULTRA

    @Column(nullable = false)
    private int pointValue;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date spawnedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date foundAt;

    @Column
    private UUID foundByPlayerId;

    @Column(length = 500)
    private String secretMessage;          // Fun message when found

    @Column(columnDefinition = "TEXT")
    private String placementRules;         // JSON rules for responsive design

    @PrePersist
    protected void onCreate() {
        if (spawnedAt == null) {
            spawnedAt = new Date();
        }
    }

    /**
     * Easter egg types with different rarities, point values, and visual effects
     */
    public enum EggType {
        COMMON(5, "common-egg.png", "#8BC34A", "none", 1.0f, "Common Egg"),
        UNCOMMON(10, "uncommon-egg.png", "#FF9800", "glow", 1.2f, "Uncommon Egg"), 
        RARE(25, "rare-egg.png", "#9C27B0", "sparkle", 1.5f, "Rare Star Egg"),
        EPIC(50, "epic-egg.png", "#3F51B5", "pulse", 1.8f, "Epic Diamond Egg"),
        LEGENDARY(100, "legendary-egg.png", "#F44336", "rainbow", 2.0f, "Legendary Sparkle Egg"),
        MYTHICAL(250, "mythical-egg.png", "#E91E63", "ethereal", 2.5f, "Mythical Crystal Egg");

        private final int basePoints;
        private final String imageFilename;
        private final String color;
        private final String visualEffect;
        private final float sizeMultiplier;
        private final String displayName;

        EggType(int basePoints, String imageFilename, String color, String visualEffect, float sizeMultiplier, String displayName) {
            this.basePoints = basePoints;
            this.imageFilename = imageFilename;
            this.color = color;
            this.visualEffect = visualEffect;
            this.sizeMultiplier = sizeMultiplier;
            this.displayName = displayName;
        }

        public int getBasePoints() { return basePoints; }
        public String getImageFilename() { return imageFilename; }
        public String getColor() { return color; }
        public String getVisualEffect() { return visualEffect; }
        public float getSizeMultiplier() { return sizeMultiplier; }
        public String getDisplayName() { return displayName; }
        
        /**
         * Get rarity percentage for spawn chances
         */
        public float getRarityPercent() {
            return switch (this) {
                case COMMON -> 45.0f;      // 45%
                case UNCOMMON -> 30.0f;    // 30%
                case RARE -> 15.0f;        // 15%
                case EPIC -> 7.0f;         // 7%
                case LEGENDARY -> 2.5f;    // 2.5%
                case MYTHICAL -> 0.5f;     // 0.5%
            };
        }
    }
}