package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluator for achievements based on close game victories (small score difference).
 */
@Component
public class CloseGameEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the maximum score difference required
        int maxDifference = criteria.has("threshold") ? criteria.get("threshold").asInt() : 2;

        // Get the current game
        Game game = context.getGame();
        if (game == null) {
            return 0; // No game, no evaluation
        }

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Check if the player won the game
        boolean playerWon = isPlayerWinner(playerId, game);
        if (!playerWon) {
            return 0; // Player didn't win, no achievement
        }

        // Calculate score difference
        int scoreDifference = Math.abs(game.getChallengerTeamScore() - game.getOpponentTeamScore());

        // Check if the score difference is within the required range
        if (scoreDifference <= maxDifference) {
            return 1; // Achievement unlocked
        }

        return 0; // Score difference too large
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "CLOSE_GAME_WIN".equals(criteria.get("type").asText());
    }

    /**
     * Checks if the player won the game.
     */
    private boolean isPlayerWinner(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return (game.getChallengerId().equals(playerId) && game.isChallengerWin()) ||
                    (game.getOpponentId().equals(playerId) && game.isOpponentWin());
        }
        else {
            // Doubles game
            return (game.getChallengerTeam().contains(playerId) && game.isChallengerTeamWin()) ||
                    (game.getOpponentTeam().contains(playerId) && game.isOpponentTeamWin());
        }
    }
}