package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.events.GameCompletedEvent;
import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.services.PlayerStatisticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test for AchievementEventHandler to verify UndeclaredThrowableException fix
 */
@ExtendWith(MockitoExtension.class)
class AchievementEventHandlerTest {

    @Mock
    private SmartAchievementFilterService filterService;

    @Mock
    private AchievementServiceImpl achievementService;

    @Mock
    private PlayerStatisticsService playerStatisticsService;

    @InjectMocks
    private AchievementEventHandler achievementEventHandler;

    private Game testGame;
    private Player testPlayer;
    private List<Player> testPlayers;
    private GameCompletedEvent testEvent;

    @BeforeEach
    void setUp() {
        // Create test game
        testGame = new Game();
        testGame.setGameId(UUID.fromString("91b32155-fd4b-4074-8328-0d068ebfb685"));
        testGame.setChallengerId(UUID.randomUUID());
        testGame.setOpponentId(UUID.randomUUID());
        testGame.setChallengerTeamScore(21);
        testGame.setOpponentTeamScore(19);
        testGame.setSinglesGame(true);
        testGame.setRatedGame(true);

        // Create test player
        testPlayer = new Player();
        testPlayer.setPlayerId(testGame.getChallengerId());
        testPlayer.setFirstName("Test");
        testPlayer.setLastName("Player");
        
        testPlayers = List.of(testPlayer);

        // Create test event
        testEvent = new GameCompletedEvent(this, testGame, testPlayers);
    }

    @Test
    void testHandleGameCompleted_Success() {
        // Arrange
        List<Achievement> mockAchievements = List.of(createMockAchievement());
        PlayerStatistics mockStats = createMockPlayerStatistics();

        when(filterService.getAchievementsForGameCompletion(any(GameType.class)))
                .thenReturn(mockAchievements);
        when(playerStatisticsService.getOrCreatePlayerStatistics(any(UUID.class)))
                .thenReturn(mockStats);

        // Act - this should not throw UndeclaredThrowableException
        achievementEventHandler.handleGameCompleted(testEvent);

        // Assert
        verify(filterService).getAchievementsForGameCompletion(GameType.SINGLES_RANKED);
        verify(playerStatisticsService).getOrCreatePlayerStatistics(testPlayer.getPlayerId());
        verify(achievementService).evaluateSpecificAchievements(eq(testPlayer), eq(mockAchievements), any());
    }

    @Test
    void testHandleGameCompleted_WithNoAchievements() {
        // Arrange - no achievements filtered
        when(filterService.getAchievementsForGameCompletion(any(GameType.class)))
                .thenReturn(List.of());

        // Act
        achievementEventHandler.handleGameCompleted(testEvent);

        // Assert
        verify(filterService).getAchievementsForGameCompletion(GameType.SINGLES_RANKED);
        verifyNoInteractions(playerStatisticsService);
        verifyNoInteractions(achievementService);
    }

    @Test
    void testHandleGameCompleted_WithPlayerStatisticsError() {
        // Arrange
        List<Achievement> mockAchievements = List.of(createMockAchievement());
        
        when(filterService.getAchievementsForGameCompletion(any(GameType.class)))
                .thenReturn(mockAchievements);
        when(playerStatisticsService.getOrCreatePlayerStatistics(any(UUID.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act - should handle error gracefully
        achievementEventHandler.handleGameCompleted(testEvent);

        // Assert - should continue processing despite error
        verify(filterService).getAchievementsForGameCompletion(GameType.SINGLES_RANKED);
        verify(playerStatisticsService).getOrCreatePlayerStatistics(testPlayer.getPlayerId());
        verifyNoInteractions(achievementService);
    }

    @Test
    void testHandleGameCompleted_WithAchievementEvaluationError() {
        // Arrange
        List<Achievement> mockAchievements = List.of(createMockAchievement());
        PlayerStatistics mockStats = createMockPlayerStatistics();

        when(filterService.getAchievementsForGameCompletion(any(GameType.class)))
                .thenReturn(mockAchievements);
        when(playerStatisticsService.getOrCreatePlayerStatistics(any(UUID.class)))
                .thenReturn(mockStats);
        doThrow(new RuntimeException("Achievement evaluation error"))
                .when(achievementService).evaluateSpecificAchievements(any(), any(), any());

        // Act - should handle error gracefully
        achievementEventHandler.handleGameCompleted(testEvent);

        // Assert
        verify(filterService).getAchievementsForGameCompletion(GameType.SINGLES_RANKED);
        verify(playerStatisticsService).getOrCreatePlayerStatistics(testPlayer.getPlayerId());
        verify(achievementService).evaluateSpecificAchievements(eq(testPlayer), eq(mockAchievements), any());
    }

    private Achievement createMockAchievement() {
        Achievement achievement = new Achievement();
        achievement.setId(UUID.randomUUID());
        achievement.setName("Test Achievement");
        achievement.setCriteria("{\"type\":\"GAME_COUNT\",\"threshold\":10}");
        return achievement;
    }

    private PlayerStatistics createMockPlayerStatistics() {
        return PlayerStatistics.builder()
                .playerId(testPlayer.getPlayerId())
                .totalGames(10)
                .totalWins(7)
                .totalLosses(3)
                .build();
    }
}