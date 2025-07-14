package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Evaluator for achievements based on reaching a threshold rating in all game types.
 */
@Component
public class AllRatingsThresholdEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the required rating threshold
        int requiredRating = criteria.has("threshold") ? criteria.get("threshold").asInt() : 1300;

        // Check if player meets the threshold in all game types
        boolean singlesRankedMet = player.getSinglesRankedRating() >= requiredRating;
        boolean singlesNormalMet = player.getSinglesNormalRating() >= requiredRating;
        boolean doublesRankedMet = player.getDoublesRankedRating() >= requiredRating;
        boolean doublesNormalMet = player.getDoublesNormalRating() >= requiredRating;

        // All ratings must meet the threshold
        if (singlesRankedMet && singlesNormalMet && doublesRankedMet && doublesNormalMet) {
            return 1; // Achievement unlocked
        }

        return 0; // Not all ratings meet the threshold
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "ALL_RATINGS_THRESHOLD".equals(criteria.get("type").asText());
    }
}