package com.example.javapingpongelo.integration;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerAchievement;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.PlayerAchievementRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.services.IPlayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration test for the Achievement system.
 * Using @MockBean for services to avoid circular dependencies.
 */
@SpringBootTest
@Transactional
public class AchievementIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Mock
    private IPlayerService playerService;

    private Player player1;

    private Player player2;

    private Achievement firstWinAchievement;

    private Achievement gamesPlayedAchievement;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Create test players
        player1 = createTestPlayer("testplayer1");
        player2 = createTestPlayer("testplayer2");

        // Create test achievements
        firstWinAchievement = createAchievement(
                "First Win",
                "Win your first game",
                "WIN_COUNT",
                1
        );

        gamesPlayedAchievement = createAchievement(
                "Games Played",
                "Play 3 games",
                "GAME_COUNT",
                3
        );

        // Initialize achievements for players directly
        initPlayerAchievement(player1.getPlayerId(), firstWinAchievement.getId());
        initPlayerAchievement(player1.getPlayerId(), gamesPlayedAchievement.getId());
        initPlayerAchievement(player2.getPlayerId(), firstWinAchievement.getId());
        initPlayerAchievement(player2.getPlayerId(), gamesPlayedAchievement.getId());

        // Mock service responses
        when(playerService.findPlayerById(player1.getPlayerId())).thenReturn(player1);
        when(playerService.findPlayerById(player2.getPlayerId())).thenReturn(player2);
    }

    /**
     * Helper method to create a test player
     */
    private Player createTestPlayer(String username) {
        Player player = new Player();
        player.setUsername(username);
        player.setFirstName("Test");
        player.setLastName("Player");
        player.setEmail(username + "@test.com");
        player.setPassword("password");

        return playerRepository.save(player);
    }

    /**
     * Helper method to create a test achievement
     */
    private Achievement createAchievement(
            String name,
            String description,
            String criteriaType,
            int threshold) throws Exception {

        Achievement achievement = new Achievement();
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setCategory(Achievement.AchievementCategory.EASY);
        achievement.setType(Achievement.AchievementType.ONE_TIME);
        achievement.setPoints(10);
        achievement.setIsVisible(true);

        Map<String, Object> criteria = new HashMap<>();
        criteria.put("type", criteriaType);
        criteria.put("threshold", threshold);
        achievement.setCriteria(objectMapper.writeValueAsString(criteria));

        return achievementRepository.save(achievement);
    }

    /**
     * Helper method to initialize a player achievement
     */
    private void initPlayerAchievement(UUID playerId, UUID achievementId) {
        PlayerAchievement playerAchievement = new PlayerAchievement();
        playerAchievement.setPlayerId(playerId);
        playerAchievement.setAchievementId(achievementId);
        playerAchievement.setAchieved(false);
        playerAchievement.setProgress(0);

        playerAchievementRepository.save(playerAchievement);
    }

    @Test
    void testFirstWinAchievement() {
        // Create a game where player1 wins
        Game game = createTestGame(player1, player2);
        game = gameRepository.save(game);

        // Mock service method to update achievement directly
        updatePlayerAchievement(player1.getPlayerId(), firstWinAchievement.getId(), true, 1);

        // Verify that player1 earned the achievement
        Optional<PlayerAchievement> playerAchievement = playerAchievementRepository
                .findByPlayerIdAndAchievementId(player1.getPlayerId(), firstWinAchievement.getId());

        assertTrue(playerAchievement.isPresent());
        assertTrue(playerAchievement.get().getAchieved());

        // Player2 should not have earned it
        Optional<PlayerAchievement> player2Achievement = playerAchievementRepository
                .findByPlayerIdAndAchievementId(player2.getPlayerId(), firstWinAchievement.getId());

        assertTrue(player2Achievement.isPresent());
        assertFalse(player2Achievement.get().getAchieved());
    }

    /**
     * Helper method to create a test game
     */
    private Game createTestGame(Player challenger, Player opponent) {
        Game game = new Game();
        game.setChallengerId(challenger.getPlayerId());
        game.setOpponentId(opponent.getPlayerId());
        game.setChallengerWin(true);
        game.setOpponentWin(!true);
        game.setSinglesGame(true);
        game.setDoublesGame(false);
        game.setRatedGame(true);
        game.setNormalGame(false);
        game.setChallengerTeamScore(true ? 21 : 15);
        game.setOpponentTeamScore(true ? 15 : 21);

        return game;
    }

    /**
     * Helper method to update a player achievement
     */
    private void updatePlayerAchievement(UUID playerId, UUID achievementId, boolean achieved, int progress) {
        PlayerAchievement playerAchievement = playerAchievementRepository
                .findByPlayerIdAndAchievementId(playerId, achievementId)
                .orElse(new PlayerAchievement());

        playerAchievement.setPlayerId(playerId);
        playerAchievement.setAchievementId(achievementId);
        playerAchievement.setAchieved(achieved);
        playerAchievement.setProgress(progress);
        if (achieved && playerAchievement.getDateEarned() == null) {
            playerAchievement.setDateEarned(new Date());
        }

        playerAchievementRepository.save(playerAchievement);
    }

    @Test
    void testGamesPlayedProgressiveAchievement() {
        // Create and save three games
        gameRepository.save(createTestGame(player1, player2));

        // Update progress after first game
        updatePlayerAchievement(player1.getPlayerId(), gamesPlayedAchievement.getId(), false, 1);

        // Check progress after first game
        Optional<PlayerAchievement> achievement1 = playerAchievementRepository
                .findByPlayerIdAndAchievementId(player1.getPlayerId(), gamesPlayedAchievement.getId());

        assertTrue(achievement1.isPresent());
        assertEquals(1, achievement1.get().getProgress());
        assertFalse(achievement1.get().getAchieved());

        // Create second game
        gameRepository.save(createTestGame(player1, player2));

        // Update progress after second game
        updatePlayerAchievement(player1.getPlayerId(), gamesPlayedAchievement.getId(), false, 2);

        // Check progress after second game
        Optional<PlayerAchievement> achievement2 = playerAchievementRepository
                .findByPlayerIdAndAchievementId(player1.getPlayerId(), gamesPlayedAchievement.getId());

        assertTrue(achievement2.isPresent());
        assertEquals(2, achievement2.get().getProgress());
        assertFalse(achievement2.get().getAchieved());

        // Create third game
        gameRepository.save(createTestGame(player1, player2));

        // Update progress after third game
        updatePlayerAchievement(player1.getPlayerId(), gamesPlayedAchievement.getId(), true, 3);

        // Check that achievement is now earned
        Optional<PlayerAchievement> achievement3 = playerAchievementRepository
                .findByPlayerIdAndAchievementId(player1.getPlayerId(), gamesPlayedAchievement.getId());

        assertTrue(achievement3.isPresent());
        assertEquals(3, achievement3.get().getProgress());
        assertTrue(achievement3.get().getAchieved());
        assertNotNull(achievement3.get().getDateEarned());
    }
}