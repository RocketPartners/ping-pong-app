package com.example.javapingpongelo.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for defining achievements in YAML format.
 * Allows data-driven achievement definition without code changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementConfiguration {
    
    private String id; // Internal ID for the achievement
    private String name;
    private String description;
    private String category; // EASY, MEDIUM, HARD, LEGENDARY
    private String type; // ONE_TIME, PROGRESSIVE
    private String icon;
    private Integer points;
    private Boolean visible;
    private Boolean deprecated; // If true, existing holders keep it but new users can't unlock it
    private CriteriaConfig criteria;
    private List<TriggerConfig> triggers;
    private List<DependencyConfig> dependencies;
    private NotificationConfig notifications;
    private Map<String, Object> metadata;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriteriaConfig {
        private String type; // WIN_COUNT, RATING_THRESHOLD, etc.
        private Integer threshold;
        private String gameType; // SINGLES_RANKED, etc.
        private Integer secondaryValue;
        private String achievementType; // For specialized criteria like GILYED
        private Map<String, Object> additionalParams;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TriggerConfig {
        private String triggerType; // GAME_COMPLETED, RATING_UPDATED, etc.
        private List<String> gameTypes; // Optional game type filtering
        private Map<String, Object> conditions; // Additional trigger conditions
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DependencyConfig {
        private String prerequisiteId; // ID of prerequisite achievement
        private String dependencyType; // REQUIRED, SUGGESTED, UNLOCKS
        private Integer order; // For ordering multiple dependencies
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationConfig {
        private Boolean slackChannel;
        private Boolean slackDm;
        private Boolean inApp;
        private Boolean email;
        private String customMessage;
        private String celebrationLevel; // NORMAL, SPECIAL, EPIC
        private Map<String, Object> templateData;
    }

    /**
     * Converts this configuration to an Achievement entity
     */
    public Achievement toAchievement() {
        Achievement.AchievementCategory categoryEnum = Achievement.AchievementCategory.valueOf(
                category != null ? category.toUpperCase() : "EASY");
        Achievement.AchievementType typeEnum = Achievement.AchievementType.valueOf(
                type != null ? type.toUpperCase() : "ONE_TIME");

        return Achievement.builder()
                .name(name)
                .description(description)
                .category(categoryEnum)
                .type(typeEnum)
                .criteria(criteriaToJson())
                .icon(icon != null ? icon : "trophy")
                .points(points != null ? points : 10)
                .isVisible(visible != null ? visible : true)
                .build();
    }

    /**
     * Converts criteria configuration to JSON string
     */
    public String criteriaToJson() {
        if (criteria == null) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        json.append("\"type\":\"").append(criteria.getType()).append("\"");
        
        if (criteria.getThreshold() != null) {
            json.append(",\"threshold\":").append(criteria.getThreshold());
        }
        if (criteria.getGameType() != null) {
            json.append(",\"gameType\":\"").append(criteria.getGameType()).append("\"");
        }
        if (criteria.getSecondaryValue() != null) {
            json.append(",\"secondaryValue\":").append(criteria.getSecondaryValue());
        }
        if (criteria.getAchievementType() != null) {
            json.append(",\"achievementType\":\"").append(criteria.getAchievementType()).append("\"");
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Validates the configuration
     */
    public List<String> validate() {
        List<String> errors = new java.util.ArrayList<>();

        if (name == null || name.trim().isEmpty()) {
            errors.add("Achievement name is required");
        }
        if (description == null || description.trim().isEmpty()) {
            errors.add("Achievement description is required");
        }
        if (criteria == null || criteria.getType() == null) {
            errors.add("Achievement criteria type is required");
        }
        if (points != null && points < 0) {
            errors.add("Achievement points cannot be negative");
        }

        // Validate category
        if (category != null) {
            try {
                Achievement.AchievementCategory.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid achievement category: " + category);
            }
        }

        // Validate type
        if (type != null) {
            try {
                Achievement.AchievementType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid achievement type: " + type);
            }
        }

        return errors;
    }
}