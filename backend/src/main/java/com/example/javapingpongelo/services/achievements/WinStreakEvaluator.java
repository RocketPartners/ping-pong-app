package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Evaluator for achievements based on consecutive win streaks.
 */
@Component
public class WinStreakEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // This achievement needs game history to evaluate
        List<Game> gameHistory = context.getGameHistory();
        if (gameHistory == null || gameHistory.isEmpty()) {
            return 0; // No history, no evaluation
        }

        // Get the current game
        Game currentGame = context.getGame();
        if (currentGame == null) {
            return 0; // No current game, no evaluation
        }

        // Get player ID
        UUID playerId = player.getPlayerId();

        // Check if the player won the current game
        boolean playerWonCurrentGame = isPlayerWinner(playerId, currentGame);

        if (!playerWonCurrentGame) {
            return 0; // Player didn't win, no streak progress
        }

        // Calculate current streak
        int streakLength = calculateCurrentStreak(playerId, gameHistory, currentGame);

        // Get the required streak length
        int requiredStreak = criteria.has("threshold") ? criteria.get("threshold").asInt() : 1;

        // Return progress update if threshold is reached
        if (streakLength >= requiredStreak) {
            return 1; // Achievement unlocked
        }

        return 0; // No progress update
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "WIN_STREAK".equals(criteria.get("type").asText());
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
     * Calculates the current win streak including the current game.
     */
    private int calculateCurrentStreak(UUID playerId, List<Game> gameHistory, Game currentGame) {
        // Start with 1 for the current game
        int streak = 1;

        // Sort history by date in descending order (most recent first)
        gameHistory.sort((g1, g2) -> g2.getDatePlayed().compareTo(g1.getDatePlayed()));

        // Count consecutive wins
        for (Game game : gameHistory) {
            // Skip current game if it's in the history
            if (game.getGameId().equals(currentGame.getGameId())) {
                continue;
            }

            // Check if player won this game
            if (isPlayerWinner(playerId, game)) {
                streak++;
            }
            else {
                // Streak broken
                break;
            }
        }

        return streak;
    }
}