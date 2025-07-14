package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Evaluator for achievements based on playing against unique opponents.
 */
@Component
public class UniqueOpponentsEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        parseCriteria(achievement.getCriteria());

        // This achievement needs game history to evaluate
        List<Game> gameHistory = context.getGameHistory();
        Game currentGame = context.getGame();

        if (gameHistory == null || currentGame == null) {
            return 0; // No history or current game, no evaluation
        }

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Get the new opponent(s) from the current game
        Set<UUID> newOpponents = getOpponents(playerId, currentGame);

        // Get existing unique opponents from history
        Set<UUID> existingOpponents = new HashSet<>();
        for (Game game : gameHistory) {
            // Skip the current game if it's in the history
            if (game.getGameId().equals(currentGame.getGameId())) {
                continue;
            }

            existingOpponents.addAll(getOpponents(playerId, game));
        }

        // Count only new opponents from the current game
        newOpponents.removeAll(existingOpponents);

        // Return the number of new opponents
        return newOpponents.size();
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "UNIQUE_OPPONENTS".equals(criteria.get("type").asText());
    }

    /**
     * Gets the opponents for a player in a game.
     */
    private Set<UUID> getOpponents(UUID playerId, Game game) {
        Set<UUID> opponents = new HashSet<>();

        if (game.isSinglesGame()) {
            // Singles game
            if (game.getChallengerId().equals(playerId)) {
                opponents.add(game.getOpponentId());
            }
            else if (game.getOpponentId().equals(playerId)) {
                opponents.add(game.getChallengerId());
            }
        }
        else {
            // Doubles game
            if (game.getChallengerTeam().contains(playerId)) {
                opponents.addAll(game.getOpponentTeam());
            }
            else if (game.getOpponentTeam().contains(playerId)) {
                opponents.addAll(game.getChallengerTeam());
            }
        }

        return opponents;
    }
}