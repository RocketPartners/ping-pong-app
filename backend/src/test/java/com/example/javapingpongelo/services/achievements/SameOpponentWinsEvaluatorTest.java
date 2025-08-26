package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.GameRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SameOpponentWinsEvaluatorTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private SameOpponentWinsEvaluator evaluator;

    private Player player;
    private Achievement achievement;
    private AchievementEvaluator.EvaluationContext context;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        player = new Player();
        player.setPlayerId(UUID.randomUUID());

        achievement = new Achievement();
        achievement.setCriteria("{\"type\":\"SAME_OPPONENT_WINS\",\"threshold\":10}");

        context = new AchievementEvaluator.EvaluationContext();
    }

    @Test
    void testCanHandle_SameOpponentWins_ReturnsTrue() throws Exception {
        JsonNode criteria = objectMapper.readTree("{\"type\":\"SAME_OPPONENT_WINS\",\"threshold\":10}");
        assertTrue(evaluator.canHandle(criteria));
    }

    @Test
    void testCanHandle_DifferentType_ReturnsFalse() throws Exception {
        JsonNode criteria = objectMapper.readTree("{\"type\":\"WIN_COUNT\",\"threshold\":10}");
        assertFalse(evaluator.canHandle(criteria));
    }

    @Test
    void testEvaluate_EnoughWinsAgainstSameOpponent_ReturnsOne() {
        // Arrange
        UUID opponentId = UUID.randomUUID();
        List<Game> wonGames = createWonGamesAgainstOpponent(player.getPlayerId(), opponentId, 10);
        when(gameRepository.findWinsByPlayerId(player.getPlayerId().toString())).thenReturn(wonGames);

        // Act
        int result = evaluator.evaluate(player, achievement, context);

        // Assert
        assertEquals(1, result);
    }

    @Test
    void testEvaluate_NotEnoughWinsAgainstSameOpponent_ReturnsZero() {
        // Arrange
        UUID opponentId = UUID.randomUUID();
        List<Game> wonGames = createWonGamesAgainstOpponent(player.getPlayerId(), opponentId, 5);
        when(gameRepository.findWinsByPlayerId(player.getPlayerId().toString())).thenReturn(wonGames);

        // Act
        int result = evaluator.evaluate(player, achievement, context);

        // Assert
        assertEquals(0, result);
    }

    @Test
    void testEvaluate_WinsAgainstMultipleOpponents_ReturnsCorrectResult() {
        // Arrange
        UUID opponent1 = UUID.randomUUID();
        UUID opponent2 = UUID.randomUUID();
        UUID opponent3 = UUID.randomUUID();
        
        List<Game> wonGames = new ArrayList<>();
        wonGames.addAll(createWonGamesAgainstOpponent(player.getPlayerId(), opponent1, 12)); // This should trigger the achievement
        wonGames.addAll(createWonGamesAgainstOpponent(player.getPlayerId(), opponent2, 5));
        wonGames.addAll(createWonGamesAgainstOpponent(player.getPlayerId(), opponent3, 3));
        
        when(gameRepository.findWinsByPlayerId(player.getPlayerId().toString())).thenReturn(wonGames);

        // Act
        int result = evaluator.evaluate(player, achievement, context);

        // Assert
        assertEquals(1, result);
    }

    private List<Game> createWonGamesAgainstOpponent(UUID playerId, UUID opponentId, int count) {
        List<Game> games = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Game game = new Game();
            game.setGameId(UUID.randomUUID());
            game.setSinglesGame(true);
            game.setChallengerId(playerId);
            game.setOpponentId(opponentId);
            game.setChallengerWin(true);
            game.setOpponentWin(false);
            game.setChallengerTeamScore(21);
            game.setOpponentTeamScore(15);
            games.add(game);
        }
        return games;
    }
}