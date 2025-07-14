package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluator for achievements based on number of wins.
 */
@Component
public class WinCountEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Check if the game exists in the context
        Game game = context.getGame();
        if (game == null) {
            return 0; // No game, no evaluation
        }

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Check if the player won the game
        boolean playerWon = isPlayerWinner(playerId, game);

        // Get any game type constraint
        String gameType = criteria.has("gameType") ? criteria.get("gameType").asText() : "ANY";

        // Check if the game type matches
        boolean gameTypeMatches = matchesGameType(gameType, game);

        // Return progress update
        if (playerWon && gameTypeMatches) {
            return 1; // Increment win count by 1
        }

        return 0; // No progress update
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "WIN_COUNT".equals(criteria.get("type").asText());
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

    /**
     * Checks if the game type matches the required type.
     */
    private boolean matchesGameType(String requiredType, Game game) {
        if ("ANY".equals(requiredType)) {
            return true;
        }
        else if ("SINGLES".equals(requiredType)) {
            return game.isSinglesGame();
        }
        else if ("DOUBLES".equals(requiredType)) {
            return game.isDoublesGame();
        }
        else if ("RANKED".equals(requiredType)) {
            return game.isRatedGame();
        }
        else if ("NORMAL".equals(requiredType)) {
            return game.isNormalGame();
        }
        else if ("SINGLES_RANKED".equals(requiredType)) {
            return game.isSinglesGame() && game.isRatedGame();
        }
        else if ("SINGLES_NORMAL".equals(requiredType)) {
            return game.isSinglesGame() && game.isNormalGame();
        }
        else if ("DOUBLES_RANKED".equals(requiredType)) {
            return game.isDoublesGame() && game.isRatedGame();
        }
        else if ("DOUBLES_NORMAL".equals(requiredType)) {
            return game.isDoublesGame() && game.isNormalGame();
        }

        return false;
    }
}