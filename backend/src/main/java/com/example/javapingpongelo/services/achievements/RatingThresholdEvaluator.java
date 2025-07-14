package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Evaluator for achievements based on player rating thresholds.
 */
@Component
public class RatingThresholdEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the required rating threshold
        int requiredRating = criteria.has("threshold") ? criteria.get("threshold").asInt() : 1200;

        // Get the specific game type if specified
        String gameTypeStr = criteria.has("gameType") ? criteria.get("gameType").asText() : "ANY";

        // Check if the player has reached the threshold in any game type or a specific one
        boolean thresholdReached = hasReachedRatingThreshold(player, requiredRating, gameTypeStr);

        // Return progress update
        if (thresholdReached) {
            return 1; // Achievement unlocked
        }

        return 0; // No progress update
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "RATING_THRESHOLD".equals(criteria.get("type").asText());
    }

    /**
     * Checks if the player has reached the rating threshold.
     */
    private boolean hasReachedRatingThreshold(Player player, int threshold, String gameTypeStr) {
        if ("ANY".equals(gameTypeStr)) {
            // Check any game type
            return player.getSinglesRankedRating() >= threshold ||
                    player.getSinglesNormalRating() >= threshold ||
                    player.getDoublesRankedRating() >= threshold ||
                    player.getDoublesNormalRating() >= threshold;
        }
        else if ("SINGLES_RANKED".equals(gameTypeStr) || GameType.SINGLES_RANKED.toString().equals(gameTypeStr)) {
            return player.getSinglesRankedRating() >= threshold;
        }
        else if ("SINGLES_NORMAL".equals(gameTypeStr) || GameType.SINGLES_NORMAL.toString().equals(gameTypeStr)) {
            return player.getSinglesNormalRating() >= threshold;
        }
        else if ("DOUBLES_RANKED".equals(gameTypeStr) || GameType.DOUBLES_RANKED.toString().equals(gameTypeStr)) {
            return player.getDoublesRankedRating() >= threshold;
        }
        else if ("DOUBLES_NORMAL".equals(gameTypeStr) || GameType.DOUBLES_NORMAL.toString().equals(gameTypeStr)) {
            return player.getDoublesNormalRating() >= threshold;
        }

        return false;
    }
}