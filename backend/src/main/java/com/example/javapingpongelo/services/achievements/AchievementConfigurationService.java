package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.repositories.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for loading and managing achievement configurations from YAML files.
 * Enables data-driven achievement definition and management.
 */
@Service
@Slf4j
public class AchievementConfigurationService {

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private AchievementTriggerRepository achievementTriggerRepository;

    @Autowired
    private AchievementDependencyRepository dependencyRepository;

    private final ObjectMapper yamlMapper;
    private Map<String, AchievementConfiguration> loadedConfigurations;

    public AchievementConfigurationService() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        this.loadedConfigurations = new HashMap<>();
    }

    /**
     * Loads achievement configurations from YAML file
     */
    public void loadConfigurationsFromFile(String filename) {
        try {
            Resource resource = new ClassPathResource(filename);
            loadConfigurationsFromResource(resource);
        } catch (Exception e) {
            log.error("Error loading achievement configurations from file: {}", filename, e);
            throw new RuntimeException("Failed to load achievement configurations", e);
        }
    }

    /**
     * Loads configurations from a resource
     */
    private void loadConfigurationsFromResource(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            AchievementConfigurationWrapper wrapper = yamlMapper.readValue(inputStream, AchievementConfigurationWrapper.class);
            
            if (wrapper.getAchievements() != null) {
                for (AchievementConfiguration config : wrapper.getAchievements()) {
                    // Validate configuration
                    List<String> errors = config.validate();
                    if (!errors.isEmpty()) {
                        log.error("Invalid configuration for achievement '{}': {}", config.getName(), errors);
                        continue;
                    }

                    loadedConfigurations.put(config.getId(), config);
                    log.debug("Loaded configuration for achievement: {}", config.getName());
                }
                
                log.info("Successfully loaded {} achievement configurations", wrapper.getAchievements().size());
            }
        }
    }

    /**
     * Applies loaded configurations to the database
     */
    @Transactional
    public void applyConfigurations() {
        log.info("Applying {} achievement configurations to database", loadedConfigurations.size());
        
        int created = 0;
        int updated = 0;
        int errors = 0;

        for (AchievementConfiguration config : loadedConfigurations.values()) {
            try {
                if (applyConfiguration(config)) {
                    created++;
                } else {
                    updated++;
                }
            } catch (Exception e) {
                log.error("Error applying configuration for achievement: {}", config.getName(), e);
                errors++;
            }
        }

        log.info("Configuration application complete: {} created, {} updated, {} errors", 
                created, updated, errors);
    }

    /**
     * Applies a single configuration
     */
    private boolean applyConfiguration(AchievementConfiguration config) {
        // Check if achievement already exists by name
        Optional<Achievement> existingAchievement = achievementRepository.findByName(config.getName());
        
        Achievement achievement;
        boolean isNew = false;

        if (existingAchievement.isPresent()) {
            achievement = existingAchievement.get();
            updateAchievementFromConfig(achievement, config);
            log.debug("Updated existing achievement: {}", config.getName());
        } else {
            achievement = config.toAchievement();
            isNew = true;
            log.debug("Created new achievement: {}", config.getName());
        }

        achievement = achievementRepository.save(achievement);

        // Apply triggers
        applyTriggerConfiguration(achievement.getId(), config);

        // Apply dependencies
        applyDependencyConfiguration(achievement.getId(), config);

        return isNew;
    }

    /**
     * Updates existing achievement from configuration
     */
    private void updateAchievementFromConfig(Achievement achievement, AchievementConfiguration config) {
        achievement.setDescription(config.getDescription());
        achievement.setCategory(Achievement.AchievementCategory.valueOf(
                config.getCategory() != null ? config.getCategory().toUpperCase() : "EASY"));
        achievement.setType(Achievement.AchievementType.valueOf(
                config.getType() != null ? config.getType().toUpperCase() : "ONE_TIME"));
        achievement.setCriteria(config.criteriaToJson());
        achievement.setIcon(config.getIcon() != null ? config.getIcon() : achievement.getIcon());
        achievement.setPoints(config.getPoints() != null ? config.getPoints() : achievement.getPoints());
        achievement.setIsVisible(config.getVisible() != null ? config.getVisible() : achievement.getIsVisible());
        achievement.setDeprecated(config.getDeprecated() != null ? config.getDeprecated() : achievement.getDeprecated());
    }

    /**
     * Applies trigger configuration
     */
    private void applyTriggerConfiguration(UUID achievementId, AchievementConfiguration config) {
        if (config.getTriggers() == null || config.getTriggers().isEmpty()) {
            return;
        }

        // Remove existing triggers for this achievement
        List<AchievementTrigger> existingTriggers = achievementTriggerRepository.findByAchievementId(achievementId);
        achievementTriggerRepository.deleteAll(existingTriggers);

        // Create new triggers
        for (AchievementConfiguration.TriggerConfig triggerConfig : config.getTriggers()) {
            try {
                AchievementTrigger.TriggerType triggerType = AchievementTrigger.TriggerType.valueOf(
                        triggerConfig.getTriggerType().toUpperCase());

                List<GameType> gameTypes = null;
                if (triggerConfig.getGameTypes() != null && !triggerConfig.getGameTypes().isEmpty()) {
                    gameTypes = triggerConfig.getGameTypes().stream()
                            .map(this::mapToGameType)
                            .collect(Collectors.toList());
                }

                AchievementTrigger trigger = AchievementTrigger.builder()
                        .achievementId(achievementId)
                        .triggerType(triggerType)
                        .gameTypes(gameTypes)
                        .conditions(triggerConfig.getConditions() != null ? 
                                   triggerConfig.getConditions().toString() : null)
                        .build();

                achievementTriggerRepository.save(trigger);
                log.debug("Created trigger {} for achievement {}", triggerType, achievementId);

            } catch (Exception e) {
                log.error("Error creating trigger for achievement {}: {}", achievementId, e.getMessage());
                log.debug("Trigger config causing error: triggerType={}, gameTypes={}", 
                         triggerConfig.getTriggerType(), triggerConfig.getGameTypes());
                // Continue processing other triggers even if one fails
            }
        }
    }

    /**
     * Applies dependency configuration
     */
    private void applyDependencyConfiguration(UUID achievementId, AchievementConfiguration config) {
        if (config.getDependencies() == null || config.getDependencies().isEmpty()) {
            return;
        }

        for (AchievementConfiguration.DependencyConfig depConfig : config.getDependencies()) {
            try {
                // Find prerequisite achievement by ID (name lookup)
                AchievementConfiguration prerequisiteConfig = loadedConfigurations.get(depConfig.getPrerequisiteId());
                if (prerequisiteConfig == null) {
                    log.warn("Prerequisite achievement not found: {}", depConfig.getPrerequisiteId());
                    continue;
                }

                Optional<Achievement> prerequisiteAchievement = achievementRepository.findByName(prerequisiteConfig.getName());
                if (prerequisiteAchievement.isEmpty()) {
                    log.warn("Prerequisite achievement not in database: {}", prerequisiteConfig.getName());
                    continue;
                }

                AchievementDependency.DependencyType dependencyType = AchievementDependency.DependencyType.valueOf(
                        depConfig.getDependencyType().toUpperCase());

                AchievementDependency dependency = AchievementDependency.builder()
                        .achievementId(achievementId)
                        .prerequisiteAchievementId(prerequisiteAchievement.get().getId())
                        .dependencyType(dependencyType)
                        .dependencyOrder(depConfig.getOrder())
                        .build();

                dependencyRepository.save(dependency);
                log.debug("Created dependency {} -> {} ({})", 
                         prerequisiteConfig.getName(), config.getName(), dependencyType);

            } catch (Exception e) {
                log.error("Error creating dependency for achievement {}: {}", achievementId, e.getMessage());
            }
        }
    }

    /**
     * Exports current achievements to YAML configuration
     */
    public String exportAchievementsToYaml() {
        try {
            List<Achievement> achievements = achievementRepository.findAll();
            List<AchievementConfiguration> configurations = new ArrayList<>();

            for (Achievement achievement : achievements) {
                AchievementConfiguration config = convertToConfiguration(achievement);
                configurations.add(config);
            }

            AchievementConfigurationWrapper wrapper = new AchievementConfigurationWrapper();
            wrapper.setAchievements(configurations);

            return yamlMapper.writeValueAsString(wrapper);

        } catch (Exception e) {
            log.error("Error exporting achievements to YAML", e);
            throw new RuntimeException("Failed to export achievements", e);
        }
    }

    /**
     * Converts Achievement entity to configuration
     */
    private AchievementConfiguration convertToConfiguration(Achievement achievement) {
        // Parse criteria JSON back to config object
        AchievementConfiguration.CriteriaConfig criteriaConfig = null;
        try {
            if (achievement.getCriteria() != null && !achievement.getCriteria().isEmpty()) {
                // Simple JSON parsing for criteria
                criteriaConfig = parseCriteriaJson(achievement.getCriteria());
            }
        } catch (Exception e) {
            log.warn("Error parsing criteria for achievement {}: {}", achievement.getName(), e.getMessage());
        }

        return AchievementConfiguration.builder()
                .id(achievement.getName().toLowerCase().replaceAll("\\s+", "_"))
                .name(achievement.getName())
                .description(achievement.getDescription())
                .category(achievement.getCategory().toString())
                .type(achievement.getType().toString())
                .icon(achievement.getIcon())
                .points(achievement.getPoints())
                .visible(achievement.getIsVisible())
                .criteria(criteriaConfig)
                .build();
    }

    /**
     * Simple JSON parsing for criteria (basic implementation)
     */
    private AchievementConfiguration.CriteriaConfig parseCriteriaJson(String criteriaJson) {
        // This is a simplified parser - in production you'd use Jackson
        AchievementConfiguration.CriteriaConfig config = new AchievementConfiguration.CriteriaConfig();
        
        if (criteriaJson.contains("\"type\"")) {
            String type = extractJsonValue(criteriaJson, "type");
            config.setType(type);
        }
        if (criteriaJson.contains("\"threshold\"")) {
            String threshold = extractJsonValue(criteriaJson, "threshold");
            try {
                config.setThreshold(Integer.valueOf(threshold));
            } catch (NumberFormatException e) {
                log.warn("Invalid threshold value: {}", threshold);
            }
        }
        if (criteriaJson.contains("\"gameType\"")) {
            String gameType = extractJsonValue(criteriaJson, "gameType");
            config.setGameType(gameType);
        }

        return config;
    }

    /**
     * Extracts value from simple JSON string
     */
    private String extractJsonValue(String json, String key) {
        String searchPattern = "\"" + key + "\":\"";
        int startIndex = json.indexOf(searchPattern);
        if (startIndex == -1) {
            // Try without quotes (for numbers)
            searchPattern = "\"" + key + "\":";
            startIndex = json.indexOf(searchPattern);
            if (startIndex == -1) return null;
            
            startIndex += searchPattern.length();
            int endIndex = json.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = json.indexOf("}", startIndex);
            return json.substring(startIndex, endIndex).trim();
        }
        
        startIndex += searchPattern.length();
        int endIndex = json.indexOf("\"", startIndex);
        return json.substring(startIndex, endIndex);
    }

    /**
     * Gets loaded configurations
     */
    public Map<String, AchievementConfiguration> getLoadedConfigurations() {
        return new HashMap<>(loadedConfigurations);
    }

    /**
     * Validates all loaded configurations
     */
    public Map<String, List<String>> validateAllConfigurations() {
        Map<String, List<String>> validationResults = new HashMap<>();
        
        for (Map.Entry<String, AchievementConfiguration> entry : loadedConfigurations.entrySet()) {
            List<String> errors = entry.getValue().validate();
            if (!errors.isEmpty()) {
                validationResults.put(entry.getKey(), errors);
            }
        }
        
        return validationResults;
    }

    /**
     * Maps legacy GameType values to current enum values
     */
    private GameType mapToGameType(String gameTypeString) {
        String upperCase = gameTypeString.toUpperCase();
        switch (upperCase) {
            case "SINGLES_CASUAL":
                log.warn("Found legacy gameType SINGLES_CASUAL in YAML, mapping to SINGLES_NORMAL");
                return GameType.SINGLES_NORMAL;
            case "DOUBLES_CASUAL":
                log.warn("Found legacy gameType DOUBLES_CASUAL in YAML, mapping to DOUBLES_NORMAL");
                return GameType.DOUBLES_NORMAL;
            default:
                try {
                    return GameType.valueOf(upperCase);
                } catch (IllegalArgumentException e) {
                    log.error("Unknown GameType '{}', mapping to SINGLES_NORMAL as fallback", gameTypeString);
                    return GameType.SINGLES_NORMAL;
                }
        }
    }

    /**
     * Wrapper class for YAML structure
     */
    @lombok.Data
    public static class AchievementConfigurationWrapper {
        private List<AchievementConfiguration> achievements;
    }
}