package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WinCountEvaluatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WinCountEvaluator evaluator;

    private Player player;

    private Game game;

    private Achievement achievement;

    private AchievementEvaluator.EvaluationContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create player
        player = new Player();
        player.setPlayerId(UUID.randomUUID());

        // Create game with player as challenger
        game = new Game();
        game.setGameId(UUID.randomUUID());
        game.setChallengerId(player.getPlayerId());
        game.setOpponentId(UUID.randomUUID());
        game.setChallengerWin(true);
        game.setOpponentWin(false);
        game.setSinglesGame(true);
        game.setDoublesGame(false);
        game.setRatedGame(true);
        game.setNormalGame(false);

        // Create context
        context = new AchievementEvaluator.EvaluationContext();
        context.setGame(game);

        // Create achievement with basic criteria
        achievement = new Achievement();
        achievement.setId(UUID.randomUUID());
        achievement.setName("Test Win Achievement");
        achievement.setType(Achievement.AchievementType.PROGRESSIVE);

        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", "WIN_COUNT");
            criteria.put("threshold", 5);
            achievement.setCriteria(objectMapper.writeValueAsString(criteria));
        }
        catch (Exception e) {
            fail("Error creating test criteria: " + e.getMessage());
        }
    }

    @Test
    void testCanHandle() {
        assertTrue(evaluator.canHandle(evaluator.parseCriteria(achievement.getCriteria())));

        // Test with different criteria type
        try {
            Map<String, Object> wrongCriteria = new HashMap<>();
            wrongCriteria.put("type", "GAME_COUNT");
            wrongCriteria.put("threshold", 5);
            String criteriaJson = objectMapper.writeValueAsString(wrongCriteria);

            assertFalse(evaluator.canHandle(evaluator.parseCriteria(criteriaJson)));
        }
        catch (Exception e) {
            fail("Error creating test criteria: " + e.getMessage());
        }
    }

    @Test
    void testEvaluateWhenPlayerWins() {
        // Player is the challenger and won
        game.setChallengerWin(true);
        game.setOpponentWin(false);

        int result = evaluator.evaluate(player, achievement, context);

        // Should return 1 for a win
        assertEquals(1, result);
    }

    @Test
    void testEvaluateWhenPlayerLoses() {
        // Player is the challenger but lost
        game.setChallengerWin(false);
        game.setOpponentWin(true);

        int result = evaluator.evaluate(player, achievement, context);

        // Should return 0 for a loss
        assertEquals(0, result);
    }

    @Test
    void testEvaluateForDoublesGame() {
        // Create a doubles game with player in challenger team
        game.setSinglesGame(false);
        game.setDoublesGame(true);
        game.setChallengerTeam(Arrays.asList(player.getPlayerId(), UUID.randomUUID()));
        game.setOpponentTeam(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));
        game.setChallengerTeamWin(true);
        game.setOpponentTeamWin(false);

        int result = evaluator.evaluate(player, achievement, context);

        // Should return 1 for a win
        assertEquals(1, result);
    }

    @Test
    void testEvaluateWithGameTypeConstraint() {
        try {
            // Create criteria with game type constraint
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", "WIN_COUNT");
            criteria.put("threshold", 5);
            criteria.put("gameType", "SINGLES");
            achievement.setCriteria(objectMapper.writeValueAsString(criteria));

            // Singles game should match
            game.setSinglesGame(true);
            game.setDoublesGame(false);

            int result = evaluator.evaluate(player, achievement, context);
            assertEquals(1, result);

            // Doubles game should not match
            game.setSinglesGame(false);
            game.setDoublesGame(true);
            game.setChallengerTeam(Arrays.asList(player.getPlayerId(), UUID.randomUUID()));
            game.setOpponentTeam(Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));

            result = evaluator.evaluate(player, achievement, context);
            assertEquals(0, result);
        }
        catch (Exception e) {
            fail("Error creating test criteria: " + e.getMessage());
        }
    }

    @Test
    void testEvaluateWithNoGame() {
        // No game in context
        context.setGame(null);

        int result = evaluator.evaluate(player, achievement, context);

        // Should return 0 when no game is provided
        assertEquals(0, result);
    }
}