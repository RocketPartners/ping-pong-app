package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluator for achievements based on the opponent's score being below a threshold.
 */
@Component
public class OpponentScoreEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Get the maximum opponent score allowed
        int maxOpponentScore = criteria.has("threshold") ? criteria.get("threshold").asInt() : 5;

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

        // Get opponent's score
        int opponentScore = getOpponentScore(playerId, game);

        // Check if opponent's score is below the threshold
        if (opponentScore < maxOpponentScore) {
            return 1; // Achievement unlocked
        }

        return 0; // Opponent scored too many points
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "OPPONENT_SCORE_BELOW".equals(criteria.get("type").asText());
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
     * Gets the opponent's score in the game.
     */
    private int getOpponentScore(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            if (game.getChallengerId().equals(playerId)) {
                return game.getOpponentTeamScore();
            }
            else {
                return game.getChallengerTeamScore();
            }
        }
        else {
            // Doubles game
            if (game.getChallengerTeam().contains(playerId)) {
                return game.getOpponentTeamScore();
            }
            else {
                return game.getChallengerTeamScore();
            }
        }
    }
}