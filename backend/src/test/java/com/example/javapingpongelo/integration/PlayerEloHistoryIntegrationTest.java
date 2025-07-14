package com.example.javapingpongelo.integration;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerEloHistory;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.PlayerEloHistoryRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.services.PlayerEloHistoryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ELO history tracking
 */
@SpringBootTest
public class PlayerEloHistoryIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerEloHistoryRepository eloHistoryRepository;

    @Autowired
    private PlayerEloHistoryService eloHistoryService;

    @PersistenceContext
    private EntityManager entityManager;

    private Player player1;

    private Player player2;

    private Game game;

    @BeforeEach
    void setup() {
        // Clear any existing data to avoid conflicts
        eloHistoryRepository.deleteAll();
        gameRepository.deleteAll();
        playerRepository.deleteAll();

        entityManager.flush();
        entityManager.clear();

        // Create test players
        player1 = createTestPlayer("eloplayer1");
        player2 = createTestPlayer("eloplayer2");

        // Force flush to ensure players are persisted
        entityManager.flush();

        // Create a test game
        game = createTestGame(player1, player2);

        // Final flush to ensure everything is persisted
        entityManager.flush();
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
        player.setSinglesRankedRating(1000);
        player.setDoublesRankedRating(1000);
        player.setSinglesNormalRating(1000);
        player.setDoublesNormalRating(1000);

        Player savedPlayer = playerRepository.save(player);
        entityManager.flush();
        return savedPlayer;
    }

    /**
     * Helper method to create a test game
     */
    private Game createTestGame(Player challenger, Player opponent) {
        // Create a new Game object
        Game game = new Game();
        // Don't set the gameId - let Hibernate/JPA handle this
        // game.setGameId(UUID.randomUUID()); <-- REMOVE THIS LINE

        // Set other game properties
        game.setChallengerId(challenger.getPlayerId());
        game.setOpponentId(opponent.getPlayerId());
        game.setChallengerWin(true);
        game.setOpponentWin(false);
        game.setSinglesGame(true);
        game.setDoublesGame(false);
        game.setRatedGame(true);
        game.setNormalGame(false);
        game.setChallengerTeamScore(21);
        game.setOpponentTeamScore(15);
        game.setDatePlayed(new Date());

        // Save and return the persisted game
        return gameRepository.saveAndFlush(game);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void testEloHistoryRecordingForGame() {
        // Refresh entities from database to avoid stale state
        player1 = entityManager.find(Player.class, player1.getPlayerId());
        player2 = entityManager.find(Player.class, player2.getPlayerId());
        game = entityManager.find(Game.class, game.getGameId());

        // Record initial ELO ratings
        int player1InitialElo = player1.getSinglesRankedRating();
        int player2InitialElo = player2.getSinglesRankedRating();

        // Simulate ELO calculation
        int player1NewElo = player1InitialElo + 25;
        int player2NewElo = player2InitialElo - 25;

        // Record ELO changes
        eloHistoryService.recordEloChange(
                player1.getPlayerId(),
                game.getGameId(),
                GameType.SINGLES_RANKED,
                player1InitialElo,
                player1NewElo
        );

        entityManager.flush();

        eloHistoryService.recordEloChange(
                player2.getPlayerId(),
                game.getGameId(),
                GameType.SINGLES_RANKED,
                player2InitialElo,
                player2NewElo
        );

        entityManager.flush();

        // Update player ELO ratings in database - refresh first
        player1 = entityManager.find(Player.class, player1.getPlayerId());
        player1.setSinglesRankedRating(player1NewElo);
        player1.setSinglesRankedWins(player1.getSinglesRankedWins() + 1);
        player1 = playerRepository.save(player1);

        player2 = entityManager.find(Player.class, player2.getPlayerId());
        player2.setSinglesRankedRating(player2NewElo);
        player2.setSinglesRankedLoses(player2.getSinglesRankedLoses() + 1);
        player2 = playerRepository.save(player2);

        entityManager.flush();
        entityManager.clear();

        // Verify ELO history was recorded
        List<PlayerEloHistory> player1History = eloHistoryRepository.findByPlayerId(player1.getPlayerId());
        List<PlayerEloHistory> player2History = eloHistoryRepository.findByPlayerId(player2.getPlayerId());

        // Assertions for player 1
        assertFalse(player1History.isEmpty());
        assertEquals(1, player1History.size());
        assertEquals(player1InitialElo, player1History.getFirst().getPreviousElo());
        assertEquals(player1NewElo, player1History.getFirst().getNewElo());
        assertEquals(25, player1History.getFirst().getEloChange());
        assertEquals(game.getGameId(), player1History.getFirst().getGameId());
        assertEquals(GameType.SINGLES_RANKED, player1History.getFirst().getGameType());

        // Assertions for player 2
        assertFalse(player2History.isEmpty());
        assertEquals(1, player2History.size());
        assertEquals(player2InitialElo, player2History.getFirst().getPreviousElo());
        assertEquals(player2NewElo, player2History.getFirst().getNewElo());
        assertEquals(-25, player2History.getFirst().getEloChange());
        assertEquals(game.getGameId(), player2History.getFirst().getGameId());
        assertEquals(GameType.SINGLES_RANKED, player2History.getFirst().getGameType());
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void testRankCalculation() {
        // Refresh entities from database to avoid stale state
        player1 = entityManager.find(Player.class, player1.getPlayerId());

        // First, count existing players to account for any in the test DB
        long initialPlayerCount = playerRepository.count();

        // Create additional players with varied ratings
        Player player3 = createTestPlayer("eloplayer3");
        player3.setSinglesRankedRating(1300);
        player3 = playerRepository.save(player3);

        Player player4 = createTestPlayer("eloplayer4");
        player4.setSinglesRankedRating(950);
        player4 = playerRepository.save(player4);

        Player player5 = createTestPlayer("eloplayer5");
        player5.setSinglesRankedRating(850);
        player5 = playerRepository.save(player5);

        entityManager.flush();
        entityManager.clear();

        // Get fresh game entity
        game = entityManager.find(Game.class, game.getGameId());
        player1 = entityManager.find(Player.class, player1.getPlayerId());

        // Record ELO change for player1 with new ELO
        PlayerEloHistory eloHistory = eloHistoryService.recordEloChange(
                player1.getPlayerId(),
                game.getGameId(),
                GameType.SINGLES_RANKED,
                1000,
                1050
        );

        entityManager.flush();

        // Calculate expected values based on the players we created plus any pre-existing ones
        int expectedTotalPlayers = (int) (initialPlayerCount + 3);

        // With our test setup, player1 should still be rank 2 among the players we created
        assertEquals(2, eloHistory.getRankPosition());
        assertEquals(expectedTotalPlayers, eloHistory.getTotalPlayers());

        // The percentile might vary depending on total player count, so use a more flexible check
        assertTrue(eloHistory.getPercentile() > 50.0, "Player should be in top half of rankings");
    }
}