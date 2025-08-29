package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerStatistics;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.PlayerStatisticsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for managing and updating player statistics
 */
@Service
@Slf4j
public class PlayerStatisticsService {

    @Autowired
    private PlayerStatisticsRepository playerStatisticsRepository;

    @Autowired
    private GameRepository gameRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gets or creates player statistics
     */
    @Cacheable(value = "player-statistics", key = "#playerId")
    public PlayerStatistics getOrCreatePlayerStatistics(UUID playerId) {
        try {
            return playerStatisticsRepository.findByPlayerId(playerId)
                    .orElseGet(() -> {
                        try {
                            return createInitialStatistics(playerId);
                        } catch (Exception e) {
                            log.error("Error creating initial statistics for player {}: {}", playerId, e.getMessage());
                            // Return a basic statistics object to prevent null pointer exceptions
                            return PlayerStatistics.builder()
                                    .playerId(playerId)
                                    .totalGames(0)
                                    .totalWins(0)
                                    .totalLosses(0)
                                    .build();
                        }
                    });
        } catch (Exception e) {
            log.error("Error getting or creating player statistics for player {}: {}", playerId, e.getMessage(), e);
            // Don't throw runtime exception to prevent UndeclaredThrowableException
            // Return a basic statistics object instead
            return PlayerStatistics.builder()
                    .playerId(playerId)
                    .totalGames(0)
                    .totalWins(0)
                    .totalLosses(0)
                    .build();
        }
    }

    /**
     * Creates initial statistics for a player
     */
    @Transactional
    public PlayerStatistics createInitialStatistics(UUID playerId) {
        PlayerStatistics stats = PlayerStatistics.builder()
                .playerId(playerId)
                .build();
        
        // Calculate initial statistics from existing games
        recalculateAllStatistics(stats);
        
        return playerStatisticsRepository.save(stats);
    }

    /**
     * Updates statistics after a game is completed
     */
    @Transactional
    @CacheEvict(value = "player-statistics", allEntries = true)
    public void updateStatisticsAfterGame(Game game, List<Player> players) {
        for (Player player : players) {
            updatePlayerStatisticsForGame(player.getPlayerId(), game);
        }
    }

    /**
     * Updates a single player's statistics for a specific game
     */
    private void updatePlayerStatisticsForGame(UUID playerId, Game game) {
        PlayerStatistics stats = getOrCreatePlayerStatistics(playerId);
        updatePlayerStatisticsForGameInternal(stats, game);
        playerStatisticsRepository.save(stats);
    }

    /**
     * Internal method to update player statistics for a game (avoids recursion during recalculation)
     */
    private void updatePlayerStatisticsForGameInternal(PlayerStatistics stats, Game game) {
        UUID playerId = stats.getPlayerId();
        boolean isWinner = isPlayerWinner(playerId, game);
        boolean isSingles = game.isSinglesGame();
        boolean isRanked = game.isRatedGame();
        
        // Update basic counts
        stats.setTotalGames(stats.getTotalGames() + 1);
        if (isWinner) {
            stats.setTotalWins(stats.getTotalWins() + 1);
        } else {
            stats.setTotalLosses(stats.getTotalLosses() + 1);
        }

        // Update game type specific counts
        if (isSingles) {
            stats.setSinglesGames(stats.getSinglesGames() + 1);
            if (isWinner) {
                stats.setSinglesWins(stats.getSinglesWins() + 1);
            }
        } else {
            stats.setDoublesGames(stats.getDoublesGames() + 1);
            if (isWinner) {
                stats.setDoublesWins(stats.getDoublesWins() + 1);
            }
        }

        if (isRanked) {
            stats.setRankedGames(stats.getRankedGames() + 1);
            if (isWinner) {
                stats.setRankedWins(stats.getRankedWins() + 1);
            }
        } else {
            stats.setNormalGames(stats.getNormalGames() + 1);
            if (isWinner) {
                stats.setNormalWins(stats.getNormalWins() + 1);
            }
        }

        // Update streaks
        updateStreaks(stats, isWinner);

        // Update points
        updatePointsScored(stats, playerId, game);

        // Update opponent counts
        updateOpponentCounts(stats, playerId, game, isWinner);

        // Update special achievement counts
        updateSpecialCounts(stats, playerId, game, isWinner);

        // Set last game date
        stats.setLastGameDate(LocalDateTime.now());
    }

    /**
     * Updates win/loss streaks
     */
    private void updateStreaks(PlayerStatistics stats, boolean isWinner) {
        if (isWinner) {
            stats.setCurrentWinStreak(stats.getCurrentWinStreak() + 1);
            stats.setCurrentLossStreak(0);
            
            if (stats.getCurrentWinStreak() > stats.getMaxWinStreak()) {
                stats.setMaxWinStreak(stats.getCurrentWinStreak());
            }
        } else {
            stats.setCurrentLossStreak(stats.getCurrentLossStreak() + 1);
            stats.setCurrentWinStreak(0);
        }
    }

    /**
     * Updates points scored and conceded
     */
    private void updatePointsScored(PlayerStatistics stats, UUID playerId, Game game) {
        int playerScore = getPlayerScore(playerId, game);
        int opponentScore = getOpponentScore(playerId, game);
        
        stats.setTotalPointsScored(stats.getTotalPointsScored() + playerScore);
        stats.setTotalPointsConceded(stats.getTotalPointsConceded() + opponentScore);
    }

    /**
     * Updates opponent win/loss counts
     */
    private void updateOpponentCounts(PlayerStatistics stats, UUID playerId, Game game, boolean isWinner) {
        UUID opponentId = getOpponentId(playerId, game);
        if (opponentId == null) return; // Skip for complex doubles scenarios
        
        Map<String, Integer> winCounts = parseOpponentCounts(stats.getOpponentWinCounts());
        Map<String, Integer> lossCounts = parseOpponentCounts(stats.getOpponentLossCounts());
        
        String opponentIdStr = opponentId.toString();
        
        if (isWinner) {
            winCounts.put(opponentIdStr, winCounts.getOrDefault(opponentIdStr, 0) + 1);
        } else {
            lossCounts.put(opponentIdStr, lossCounts.getOrDefault(opponentIdStr, 0) + 1);
        }
        
        // Update unique opponents count
        Set<String> allOpponents = new HashSet<>(winCounts.keySet());
        allOpponents.addAll(lossCounts.keySet());
        stats.setUniqueOpponentsPlayed(allOpponents.size());
        
        try {
            stats.setOpponentWinCounts(objectMapper.writeValueAsString(winCounts));
            stats.setOpponentLossCounts(objectMapper.writeValueAsString(lossCounts));
        } catch (JsonProcessingException e) {
            log.error("Error serializing opponent counts", e);
        }
    }

    /**
     * Updates special achievement-related counts
     */
    private void updateSpecialCounts(PlayerStatistics stats, UUID playerId, Game game, boolean isWinner) {
        if (!isWinner) return;
        
        int opponentScore = getOpponentScore(playerId, game);
        int playerScore = getPlayerScore(playerId, game);
        
        // Shutout wins (opponent scored < 5)
        if (opponentScore < 5) {
            stats.setShutoutWins(stats.getShutoutWins() + 1);
        }
        
        // Close wins (2 point difference)
        if (Math.abs(playerScore - opponentScore) == 2) {
            stats.setCloseWins(stats.getCloseWins() + 1);
        }
        
        // Gilyed tracking (singles games with 0 points)
        if (game.isSinglesGame()) {
            if (opponentScore == 0) {
                stats.setGilyedGiven(stats.getGilyedGiven() + 1);
            }
        }
    }

    /**
     * Recalculates all statistics from scratch
     */
    @Transactional
    public void recalculateAllStatistics(PlayerStatistics stats) {
        List<Game> allGames = gameRepository.findByPlayerId(stats.getPlayerId());
        
        // Reset all counts
        resetStatistics(stats);
        
        // Process each game
        for (Game game : allGames) {
            updatePlayerStatisticsForGameInternal(stats, game);
        }
    }

    /**
     * Gets the maximum wins against any single opponent
     */
    public int getMaxWinsAgainstSingleOpponent(UUID playerId) {
        PlayerStatistics stats = getOrCreatePlayerStatistics(playerId);
        Map<String, Integer> winCounts = parseOpponentCounts(stats.getOpponentWinCounts());
        return winCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    // Helper methods
    private boolean isPlayerWinner(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return (game.getChallengerId().equals(playerId) && game.isChallengerWin()) ||
                   (game.getOpponentId().equals(playerId) && game.isOpponentWin());
        } else {
            return (game.getChallengerTeam().contains(playerId) && game.isChallengerTeamWin()) ||
                   (game.getOpponentTeam().contains(playerId) && game.isOpponentTeamWin());
        }
    }

    private int getPlayerScore(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return game.getChallengerId().equals(playerId) ? 
                   game.getChallengerTeamScore() : game.getOpponentTeamScore();
        } else {
            return game.getChallengerTeam().contains(playerId) ? 
                   game.getChallengerTeamScore() : game.getOpponentTeamScore();
        }
    }

    private int getOpponentScore(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return game.getChallengerId().equals(playerId) ? 
                   game.getOpponentTeamScore() : game.getChallengerTeamScore();
        } else {
            return game.getChallengerTeam().contains(playerId) ? 
                   game.getOpponentTeamScore() : game.getChallengerTeamScore();
        }
    }

    private UUID getOpponentId(UUID playerId, Game game) {
        if (game.isSinglesGame()) {
            return game.getChallengerId().equals(playerId) ? 
                   game.getOpponentId() : game.getChallengerId();
        }
        return null; // Complex for doubles, skip for now
    }

    private Map<String, Integer> parseOpponentCounts(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing opponent counts JSON", e);
            return new HashMap<>();
        }
    }

    private void resetStatistics(PlayerStatistics stats) {
        stats.setTotalGames(0);
        stats.setTotalWins(0);
        stats.setTotalLosses(0);
        stats.setSinglesGames(0);
        stats.setSinglesWins(0);
        stats.setDoublesGames(0);
        stats.setDoublesWins(0);
        stats.setRankedGames(0);
        stats.setRankedWins(0);
        stats.setNormalGames(0);
        stats.setNormalWins(0);
        stats.setCurrentWinStreak(0);
        stats.setMaxWinStreak(0);
        stats.setCurrentLossStreak(0);
        stats.setTotalPointsScored(0);
        stats.setTotalPointsConceded(0);
        stats.setUniqueOpponentsPlayed(0);
        stats.setShutoutWins(0);
        stats.setCloseWins(0);
        stats.setGilyedGiven(0);
        stats.setGilyedReceived(0);
        stats.setOpponentWinCounts("{}");
        stats.setOpponentLossCounts("{}");
    }
}