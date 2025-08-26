package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Evaluator for achievements based on winning against the same opponent multiple times.
 */
@Component
public class SameOpponentWinsEvaluator extends AchievementEvaluator {

    @Autowired
    private GameRepository gameRepository;

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the required number of wins against the same opponent
        int requiredWins = criteria.has("threshold") ? criteria.get("threshold").asInt() : 10;

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Find the maximum number of wins against any single opponent
        int maxWinsAgainstSameOpponent = getMaxWinsAgainstSameOpponent(playerId);

        // Check if the player has reached the threshold
        if (maxWinsAgainstSameOpponent >= requiredWins) {
            return 1; // Achievement unlocked
        }

        return 0; // Not enough wins against any single opponent
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "SAME_OPPONENT_WINS".equals(criteria.get("type").asText());
    }

    /**
     * Gets the maximum number of wins against any single opponent.
     */
    private int getMaxWinsAgainstSameOpponent(UUID playerId) {
        // Get all games where this player won
        List<Game> wonGames = gameRepository.findWinsByPlayerId(playerId.toString());

        // Count wins against each opponent
        java.util.Map<UUID, Integer> winsPerOpponent = new java.util.HashMap<>();

        for (Game game : wonGames) {
            UUID opponentId = getOpponentId(playerId, game);
            if (opponentId != null) {
                winsPerOpponent.put(opponentId, winsPerOpponent.getOrDefault(opponentId, 0) + 1);
            }
        }

        // Return the maximum wins against any single opponent
        return winsPerOpponent.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    /**
     * Gets the opponent ID for a given player in a game.
     */
    private UUID getOpponentId(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            if (game.getChallengerId().equals(playerId)) {
                return game.getOpponentId();
            } else if (game.getOpponentId().equals(playerId)) {
                return game.getChallengerId();
            }
        } else {
            // For doubles games, this is more complex as there are multiple opponents
            // For now, we'll skip doubles games for this achievement
            return null;
        }
        return null;
    }
}