package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.AchievementTrigger;
import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.example.javapingpongelo.repositories.AchievementTriggerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for intelligently filtering achievements based on trigger types and conditions.
 * Dramatically reduces the number of achievements that need to be evaluated.
 */
@Service
@Slf4j
public class SmartAchievementFilterService {

    @Autowired
    private AchievementTriggerRepository achievementTriggerRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gets achievements that should be evaluated for a specific trigger type
     */
    public List<Achievement> getAchievementsForTrigger(AchievementTrigger.TriggerType triggerType) {
        List<AchievementTrigger> triggers = achievementTriggerRepository.findByTriggerType(triggerType);
        
        Set<UUID> achievementIds = triggers.stream()
                .map(AchievementTrigger::getAchievementId)
                .collect(Collectors.toSet());

        return achievementRepository.findAllById(achievementIds);
    }

    /**
     * Gets achievements that should be evaluated for a specific trigger type and game type
     */
    public List<Achievement> getAchievementsForTriggerAndGameType(
            AchievementTrigger.TriggerType triggerType, GameType gameType) {
        
        try {
            List<AchievementTrigger> triggers = achievementTriggerRepository
                    .findByTriggerTypeAndGameType(triggerType, gameType);
            
            Set<UUID> achievementIds = triggers.stream()
                    .map(AchievementTrigger::getAchievementId)
                    .collect(Collectors.toSet());

            List<Achievement> achievements = achievementRepository.findAllById(achievementIds);
            
            log.debug("Filtered {} achievements for trigger {} and game type {}", 
                     achievements.size(), triggerType, gameType);
            
            return achievements;
        } catch (Exception e) {
            log.error("Error filtering achievements for trigger {} and game type {}: {}", 
                     triggerType, gameType, e.getMessage(), e);
            return List.of(); // Return empty list to prevent further issues
        }
    }

    /**
     * Gets achievements that apply to all game types (universal triggers)
     */
    public List<Achievement> getUniversalAchievements(AchievementTrigger.TriggerType triggerType) {
        List<AchievementTrigger> universalTriggers = achievementTriggerRepository
                .findTriggersForAllGameTypes()
                .stream()
                .filter(trigger -> trigger.getTriggerType() == triggerType)
                .collect(Collectors.toList());

        Set<UUID> achievementIds = universalTriggers.stream()
                .map(AchievementTrigger::getAchievementId)
                .collect(Collectors.toSet());

        return achievementRepository.findAllById(achievementIds);
    }

    /**
     * Gets all achievements that should be evaluated for a game completion
     * Combines game-type specific and universal achievements
     */
    public List<Achievement> getAchievementsForGameCompletion(GameType gameType) {
        // Get game-type specific achievements
        List<Achievement> gameTypeAchievements = getAchievementsForTriggerAndGameType(
                AchievementTrigger.TriggerType.GAME_COMPLETED, gameType);

        // Get universal game completion achievements
        List<Achievement> universalAchievements = getUniversalAchievements(
                AchievementTrigger.TriggerType.GAME_COMPLETED);

        // Combine and deduplicate
        Set<UUID> allIds = gameTypeAchievements.stream()
                .map(Achievement::getId)
                .collect(Collectors.toSet());

        universalAchievements.stream()
                .filter(achievement -> !allIds.contains(achievement.getId()))
                .forEach(gameTypeAchievements::add);

        long totalAchievements = achievementRepository.count();
        log.info("SmartAchievementFilterService filtered {} achievements for {} trigger (vs {} total achievements)", 
                gameTypeAchievements.size(), "GAME_COMPLETED", totalAchievements);
        
        // Additional logging to match the format seen in the error logs
        String gameTypeName = gameType.name().toLowerCase();
        log.info("Evaluating {} achievements for {} game completion (vs {} total achievements)", 
                gameTypeAchievements.size(), gameTypeName, totalAchievements);

        return gameTypeAchievements;
    }

    /**
     * Gets achievements for rating updates
     */
    public List<Achievement> getAchievementsForRatingUpdate(GameType gameType) {
        return getAchievementsForTriggerAndGameType(
                AchievementTrigger.TriggerType.RATING_UPDATED, gameType);
    }

    /**
     * Gets achievements for streak changes
     */
    public List<Achievement> getAchievementsForStreakChange() {
        return getAchievementsForTrigger(AchievementTrigger.TriggerType.STREAK_CHANGED);
    }

    /**
     * Gets achievements for tournament events
     */
    public List<Achievement> getAchievementsForTournamentEvent() {
        return getAchievementsForTrigger(AchievementTrigger.TriggerType.TOURNAMENT_EVENT);
    }

    /**
     * Populates trigger data for existing achievements (migration helper)
     * This creates appropriate triggers based on achievement criteria
     */
    public void populateTriggersForExistingAchievements() {
        log.info("Populating triggers for existing achievements...");
        
        List<Achievement> allAchievements = achievementRepository.findAll();
        int triggersCreated = 0;

        for (Achievement achievement : allAchievements) {
            // Skip if triggers already exist
            if (!achievementTriggerRepository.findByAchievementId(achievement.getId()).isEmpty()) {
                continue;
            }

            List<AchievementTrigger> triggers = createTriggersForAchievement(achievement);
            achievementTriggerRepository.saveAll(triggers);
            triggersCreated += triggers.size();
        }

        log.info("Created {} triggers for {} achievements", triggersCreated, allAchievements.size());
    }

    /**
     * Clears all existing triggers and repopulates them (for debugging/fixing)
     */
    public void clearAndRepopulateAllTriggers() {
        log.info("Clearing all existing achievement triggers...");
        achievementTriggerRepository.deleteAll();
        
        log.info("Repopulating triggers for all achievements...");
        populateTriggersForExistingAchievements();
    }

    /**
     * Creates appropriate triggers for an achievement based on its criteria
     */
    private List<AchievementTrigger> createTriggersForAchievement(Achievement achievement) {
        String criteria = achievement.getCriteria();
        List<AchievementTrigger> triggers = new ArrayList<>();
        
        try {
            JsonNode criteriaNode = objectMapper.readTree(criteria);
            String type = criteriaNode.has("type") ? criteriaNode.get("type").asText() : "";
            String gameType = criteriaNode.has("gameType") ? criteriaNode.get("gameType").asText() : "";
            
            // Determine trigger type based on criteria type
            AchievementTrigger.TriggerType triggerType;
            if (type.contains("RATING_THRESHOLD")) {
                triggerType = AchievementTrigger.TriggerType.RATING_UPDATED;
            } else if (type.contains("WIN_STREAK")) {
                triggerType = AchievementTrigger.TriggerType.STREAK_CHANGED;
            } else if (type.contains("TOURNAMENT")) {
                triggerType = AchievementTrigger.TriggerType.TOURNAMENT_EVENT;
            } else {
                // Default to game completion trigger for GAME_COUNT, WIN_COUNT, etc.
                triggerType = AchievementTrigger.TriggerType.GAME_COMPLETED;
            }
            
            // Determine applicable game types
            List<GameType> applicableGameTypes = determineGameTypes(gameType);
            
            if (applicableGameTypes.isEmpty()) {
                // Universal trigger (applies to all game types)
                triggers.add(AchievementTrigger.builder()
                        .achievementId(achievement.getId())
                        .triggerType(triggerType)
                        .gameTypes(List.of()) // Empty list means all game types
                        .build());
            } else {
                // Create trigger for specific game types
                triggers.add(AchievementTrigger.builder()
                        .achievementId(achievement.getId())
                        .triggerType(triggerType)
                        .gameTypes(applicableGameTypes)
                        .build());
            }
            
            log.debug("Created {} triggers for achievement '{}' with criteria type '{}' and gameType '{}'", 
                     triggers.size(), achievement.getName(), type, gameType);
            
        } catch (JsonProcessingException e) {
            log.error("Error parsing achievement criteria for {}: {}", achievement.getName(), e.getMessage());
            // Fallback: create a universal game completion trigger
            triggers.add(AchievementTrigger.builder()
                    .achievementId(achievement.getId())
                    .triggerType(AchievementTrigger.TriggerType.GAME_COMPLETED)
                    .gameTypes(List.of()) // Universal
                    .build());
        }
        
        return triggers;
    }
    
    /**
     * Maps achievement criteria gameType to applicable GameType enums
     */
    private List<GameType> determineGameTypes(String gameType) {
        List<GameType> result = new ArrayList<>();
        
        switch (gameType.toUpperCase()) {
            case "SINGLES":
                result.add(GameType.SINGLES_NORMAL);
                result.add(GameType.SINGLES_RANKED);
                break;
            case "DOUBLES":
                result.add(GameType.DOUBLES_NORMAL);
                result.add(GameType.DOUBLES_RANKED);
                break;
            case "RANKED":
                result.add(GameType.SINGLES_RANKED);
                result.add(GameType.DOUBLES_RANKED);
                break;
            case "NORMAL":
                result.add(GameType.SINGLES_NORMAL);
                result.add(GameType.DOUBLES_NORMAL);
                break;
            case "SINGLES_RANKED":
                result.add(GameType.SINGLES_RANKED);
                break;
            case "SINGLES_NORMAL":
                result.add(GameType.SINGLES_NORMAL);
                break;
            case "DOUBLES_RANKED":
                result.add(GameType.DOUBLES_RANKED);
                break;
            case "DOUBLES_NORMAL":
                result.add(GameType.DOUBLES_NORMAL);
                break;
            case "":
                // Empty gameType means universal (all game types)
                break;
            default:
                log.warn("Unknown gameType in achievement criteria: {}", gameType);
                break;
        }
        
        return result;
    }
}