package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Evaluator for achievements based on win rate over a minimum number of games.
 */
@Component
public class WinRateEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the minimum win rate required
        int requiredRate = criteria.has("threshold") ? criteria.get("threshold").asInt() : 50;

        // Get the minimum number of games required
        int minGames = criteria.has("secondaryValue") ? criteria.get("secondaryValue").asInt() : 10;

        // This achievement requires game history to evaluate
        List<Game> gameHistory = context.getGameHistory();
        if (gameHistory == null) {
            return 0; // No history, no evaluation
        }

        // Current game counts too if available
        Game currentGame = context.getGame();

        // Get total games count
        int totalGames = gameHistory.size();
        if (currentGame != null && !gameHistory.contains(currentGame)) {
            totalGames++;
        }

        // Check if enough games have been played
        if (totalGames < minGames) {
            return 0; // Not enough games yet
        }

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Count wins
        int wins = countPlayerWins(playerId, gameHistory);

        // Add current game if it's a win
        if (currentGame != null && !gameHistory.contains(currentGame) && isPlayerWinner(playerId, currentGame)) {
            wins++;
        }

        // Calculate win rate
        double winRate = (double) wins / totalGames * 100;

        // Check if the win rate meets the requirement
        if (winRate >= requiredRate) {
            return 1; // Achievement unlocked
        }

        return 0; // Win rate not high enough
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "WIN_RATE".equals(criteria.get("type").asText());
    }

    /**
     * Counts the number of games won by the player in the history.
     */
    private int countPlayerWins(UUID playerId, List<Game> gameHistory) {
        int wins = 0;

        for (Game game : gameHistory) {
            if (isPlayerWinner(playerId, game)) {
                wins++;
            }
        }

        return wins;
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