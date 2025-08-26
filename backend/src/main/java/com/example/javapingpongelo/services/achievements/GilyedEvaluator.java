package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Evaluator for Gilyed achievements - when someone scores 0 points in a singles game.
 * Creates two types of achievements:
 * - GILYED_WINNER: For the player who "Gilyed" someone (opponent scored 0)
 * - GILYED_LOSER: For the player who got "Gilyed" (scored 0 themselves)
 */
@Component
public class GilyedEvaluator extends AchievementEvaluator {

    @Override
    public int evaluate(Player player, Achievement achievement, EvaluationContext context) {
        JsonNode criteria = parseCriteria(achievement.getCriteria());

        // Check if the game exists in the context
        Game game = context.getGame();
        if (game == null || !game.isSinglesGame()) {
            return 0; // No game or not a singles game, no evaluation
        }

        UUID playerId = player.getPlayerId();
        String achievementType = criteria.has("achievementType") ? criteria.get("achievementType").asText() : "";

        // Check if this is a Gilyed scenario (someone scored 0)
        boolean challengerGilyed = game.getChallengerTeamScore() == 0;
        boolean opponentGilyed = game.getOpponentTeamScore() == 0;

        // No Gilyed scenario if no one scored 0 or if it was a tie at 0-0
        if (!challengerGilyed && !opponentGilyed) {
            return 0;
        }
        
        // Don't count 0-0 ties as Gilyed
        if (challengerGilyed && opponentGilyed) {
            return 0;
        }

        // Determine if this player should get this achievement
        if ("GILYED_WINNER".equals(achievementType)) {
            // Player gets achievement if they won and opponent scored 0
            boolean playerWon = isPlayerWinner(playerId, game);
            boolean opponentScoredZero = (game.getChallengerId().equals(playerId) && opponentGilyed) ||
                                       (game.getOpponentId().equals(playerId) && challengerGilyed);
            
            return (playerWon && opponentScoredZero) ? 1 : 0;
        }
        else if ("GILYED_LOSER".equals(achievementType)) {
            // Player gets achievement if they lost and scored 0
            boolean playerLost = !isPlayerWinner(playerId, game);
            boolean playerScoredZero = (game.getChallengerId().equals(playerId) && challengerGilyed) ||
                                     (game.getOpponentId().equals(playerId) && opponentGilyed);
            
            return (playerLost && playerScoredZero) ? 1 : 0;
        }

        return 0;
    }

    @Override
    public boolean canHandle(JsonNode criteria) {
        return criteria.has("type") &&
                "GILYED".equals(criteria.get("type").asText());
    }

    /**
     * Checks if the player won the game.
     */
    private boolean isPlayerWinner(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return (game.getChallengerId().equals(playerId) && game.isChallengerWin()) ||
                    (game.getOpponentId().equals(playerId) && game.isOpponentWin());
        }
        return false; // This evaluator only handles singles games
    }
}