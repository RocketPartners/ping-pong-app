package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.repositories.EasterEggStatsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Evaluator for easter egg hunting achievements.
 * Handles various egg hunting milestones and progression tracking.
 */
@Component
@Slf4j
public class EasterEggEvaluator extends AchievementEvaluator {

    @Autowired
    private EasterEggStatsRepository easterEggStatsRepository;

    @Override
    public boolean canHandle(JsonNode criteria) {
        try {
            String type = criteria.get("type").asText();
            
            return type.startsWith("EASTER_EGG_") || 
                   type.equals("EGG_HUNTER") ||
                   type.equals("RARE_EGG_COLLECTOR") ||
                   type.equals("EGG_POINTS");
        } catch (Exception e) {
            log.debug("Could not parse criteria node: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        try {
            JsonNode criteria = parseCriteria(achievement.getCriteria());
            String type = criteria.get("type").asText();
            
            // Check if this is an easter egg event trigger
            if (context.getTriggerType() != AchievementTrigger.TriggerType.EASTER_EGG_FOUND) {
                return 0; // Only evaluate on easter egg events
            }

            // For easter egg achievements, we increment by 1 when an egg is found
            // The achievement service will check if the threshold is met
            switch (type) {
                case "EASTER_EGG_TOTAL":
                    return 1; // Increment total eggs found
                    
                case "EASTER_EGG_POINTS":
                    // Get points from cached event data
                    Integer eggPoints = (Integer) context.getComputedValues().get("eggPoints");
                    return eggPoints != null ? eggPoints : 0;
                    
                case "EASTER_EGG_RARE":
                    // Only count if it's a rare+ egg
                    String eggType = (String) context.getComputedValues().get("eggType");
                    if (eggType != null && isRareOrBetter(eggType)) {
                        return 1;
                    }
                    return 0;
                    
                case "EASTER_EGG_EPIC":
                    // Only count if it's epic+ egg
                    eggType = (String) context.getComputedValues().get("eggType");
                    if (eggType != null && isEpicOrBetter(eggType)) {
                        return 1;
                    }
                    return 0;
                    
                case "EASTER_EGG_LEGENDARY":
                    // Only count if it's legendary+ egg
                    eggType = (String) context.getComputedValues().get("eggType");
                    if (eggType != null && isLegendaryOrBetter(eggType)) {
                        return 1;
                    }
                    return 0;
                    
                case "EASTER_EGG_MYTHICAL":
                    // Only count if it's mythical egg
                    eggType = (String) context.getComputedValues().get("eggType");
                    if ("MYTHICAL".equals(eggType)) {
                        return 1;
                    }
                    return 0;
                    
                default:
                    return 0; // Unknown type
            }
            
        } catch (Exception e) {
            log.error("Error evaluating easter egg achievement {} for player {}: {}", 
                achievement.getName(), player.getPlayerId(), e.getMessage());
            return 0;
        }
    }

    private boolean isRareOrBetter(String eggType) {
        return "RARE".equals(eggType) || "EPIC".equals(eggType) || 
               "LEGENDARY".equals(eggType) || "MYTHICAL".equals(eggType);
    }

    private boolean isEpicOrBetter(String eggType) {
        return "EPIC".equals(eggType) || "LEGENDARY".equals(eggType) || "MYTHICAL".equals(eggType);
    }

    private boolean isLegendaryOrBetter(String eggType) {
        return "LEGENDARY".equals(eggType) || "MYTHICAL".equals(eggType);
    }
}