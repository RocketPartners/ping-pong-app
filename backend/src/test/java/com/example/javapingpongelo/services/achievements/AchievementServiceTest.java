package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerAchievement;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.MatchRepository;
import com.example.javapingpongelo.repositories.PlayerAchievementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AchievementServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AchievementServiceImpl achievementService;

    @Mock
    private AchievementRepository achievementRepository;

    @Mock
    private PlayerAchievementRepository playerAchievementRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private AchievementEvaluatorFactory evaluatorFactory;

    @Mock
    private WinCountEvaluator winCountEvaluator;

    private Player player;

    private Game game;

    private Achievement achievement;

    private PlayerAchievement playerAchievement;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(achievementService, "objectMapper", objectMapper);

        // Create player
        player = new Player();
        player.setPlayerId(UUID.randomUUID());
        player.setUsername("testuser");

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

        // Create achievement
        achievement = new Achievement();
        achievement.setId(UUID.randomUUID());
        achievement.setName("Test Win Achievement");
        achievement.setDescription("Win games");
        achievement.setCategory(Achievement.AchievementCategory.EASY);
        achievement.setType(Achievement.AchievementType.PROGRESSIVE);
        achievement.setPoints(10);
        achievement.setIsVisible(true);

        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", "WIN_COUNT");
            criteria.put("threshold", 5);
            achievement.setCriteria(objectMapper.writeValueAsString(criteria));
        }
        catch (Exception e) {
            fail("Error creating test criteria: " + e.getMessage());
        }

        // Create player achievement
        playerAchievement = new PlayerAchievement();
        playerAchievement.setId(UUID.randomUUID());
        playerAchievement.setPlayerId(player.getPlayerId());
        playerAchievement.setAchievementId(achievement.getId());
        playerAchievement.setAchieved(false);
        playerAchievement.setProgress(0);

        // Setup mocks
        when(achievementRepository.findById(achievement.getId())).thenReturn(Optional.of(achievement));
        when(playerAchievementRepository.findByPlayerIdAndAchievementId(player.getPlayerId(), achievement.getId()))
                .thenReturn(Optional.of(playerAchievement));
        when(evaluatorFactory.getEvaluator(anyString())).thenReturn(winCountEvaluator);
    }

    @Test
    void testFindAllAchievements() {
        List<Achievement> expectedAchievements = Collections.singletonList(achievement);
        when(achievementRepository.findAll()).thenReturn(expectedAchievements);

        List<Achievement> actualAchievements = achievementService.findAllAchievements();

        assertEquals(expectedAchievements, actualAchievements);
        verify(achievementRepository).findAll();
    }

    @Test
    void testFindVisibleAchievements() {
        List<Achievement> expectedAchievements = Collections.singletonList(achievement);
        when(achievementRepository.findByIsVisible(true)).thenReturn(expectedAchievements);

        List<Achievement> actualAchievements = achievementService.findVisibleAchievements();

        assertEquals(expectedAchievements, actualAchievements);
        verify(achievementRepository).findByIsVisible(true);
    }

    @Test
    void testCreateAchievement() {
        when(achievementRepository.save(achievement)).thenReturn(achievement);

        Achievement createdAchievement = achievementService.createAchievement(achievement);

        assertEquals(achievement, createdAchievement);
        verify(achievementRepository).save(achievement);
    }

    @Test
    void testFindAchievementById() {
        Achievement foundAchievement = achievementService.findAchievementById(achievement.getId());

        assertEquals(achievement, foundAchievement);
        verify(achievementRepository).findById(achievement.getId());
    }

    @Test
    void testUpdateAchievementProgress() {
        // Setup player achievement for update
        playerAchievement.setProgress(0);
        playerAchievement.setAchieved(false);

        when(playerAchievementRepository.save(any(PlayerAchievement.class))).thenReturn(playerAchievement);

        // Update progress
        PlayerAchievement updatedAchievement = achievementService.updateAchievementProgress(
                player.getPlayerId(), achievement.getId(), 3);

        // Verify progress updated
        assertEquals(3, updatedAchievement.getProgress());
        assertEquals(false, updatedAchievement.getAchieved()); // Shouldn't be achieved yet

        verify(playerAchievementRepository).save(any(PlayerAchievement.class));

        // Reset progress to 0 before the test
        playerAchievement.setProgress(0);

        // Call service with progress 5
        updatedAchievement = achievementService.updateAchievementProgress(
                player.getPlayerId(), achievement.getId(), 5);

        // Now expect exactly 5
        assertEquals(5, updatedAchievement.getProgress());
        assertEquals(true, updatedAchievement.getAchieved());
        assertNotNull(updatedAchievement.getDateEarned());
    }

    @Test
    void testEvaluateAchievementsForGame() {
        // Setup mocks
        List<Achievement> achievements = Collections.singletonList(achievement);
        when(achievementRepository.findAll()).thenReturn(achievements);
        when(gameRepository.findByPlayerId(player.getPlayerId()))
                .thenReturn(Collections.emptyList());
        when(matchRepository.findByPlayerId(player.getPlayerId()))
                .thenReturn(Collections.emptyList());

        // Setup evaluator to return progress
        when(winCountEvaluator.evaluate(eq(player), eq(achievement), any(AchievementEvaluator.EvaluationContext.class)))
                .thenReturn(1);

        // Call method
        achievementService.evaluateAchievementsForGame(game, player);

        // Verify achievement progress was updated
        verify(playerAchievementRepository).save(any(PlayerAchievement.class));
    }

    @Test
    void testInitializePlayerAchievements() {
        // Setup mocks
        List<Achievement> achievements = Collections.singletonList(achievement);
        when(achievementRepository.findAll()).thenReturn(achievements);

        // Initialize achievements
        achievementService.initializePlayerAchievements(player.getPlayerId());

        // Verify playerAchievements were created and saved
        verify(playerAchievementRepository).saveAll(anyList());
    }
}