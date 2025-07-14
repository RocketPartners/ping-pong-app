package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.PlayerGameHistory;
import com.example.javapingpongelo.repositories.PlayerGameHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing player game history.
 */
@Service
@Slf4j
public class PlayerGameHistoryService {

    @Autowired
    PlayerGameHistoryRepository gameHistoryRepository;


    /**
     * Get a player's game history
     */
    @Transactional(readOnly = true)
    public List<PlayerGameHistory> getGameHistory(UUID playerId) {
        return gameHistoryRepository.findByPlayerId(playerId);
    }

    /**
     * Get a player's game history with pagination
     */
    @Transactional(readOnly = true)
    public Page<PlayerGameHistory> getGameHistory(UUID playerId, Pageable pageable) {
        return gameHistoryRepository.findByPlayerId(playerId, pageable);
    }

    /**
     * Get a player's recent game history
     */
    @Transactional(readOnly = true)
    public List<PlayerGameHistory> getRecentGameHistory(UUID playerId) {
        return gameHistoryRepository.findByPlayerIdOrderByGameDateDesc(playerId);
    }

    /**
     * Add a game history entry for a player
     */
    @Transactional
    public void addGameHistoryEntry(UUID playerId, Game game, boolean isWin, GameType gameType) {
        PlayerGameHistory entry = PlayerGameHistory.builder()
                                                   .playerId(playerId)
                                                   .gameDate(game.getDatePlayed())
                                                   .isWin(isWin)
                                                   .gameType(gameType)
                                                   .gameId(game.getGameId())
                                                   .build();

        // Determine opponent ID and scores
        if (game.isSinglesGame()) {
            entry.setOpponentId(game.getChallengerId());
            entry.setPlayerScore(game.getOpponentTeamScore());
            entry.setOpponentScore(game.getChallengerTeamScore());
        }

        gameHistoryRepository.save(entry);
    }

    /**
     * Get the current win streak for a player
     */
    @Transactional(readOnly = true)
    public int getCurrentWinStreak(UUID playerId) {
        return gameHistoryRepository.getCurrentWinStreak(playerId);
    }

    /**
     * Get win percentage for a specific game type
     */
    @Transactional(readOnly = true)
    public double getWinPercentage(UUID playerId, GameType gameType) {
        Double percentage = gameHistoryRepository.getWinPercentageByGameType(playerId, gameType);
        return percentage != null ? percentage : 0.0;
    }

    /**
     * Delete all game history for a player
     */
    @Transactional
    public void deleteGameHistory(UUID playerId) {
        gameHistoryRepository.deleteByPlayerId(playerId);
        log.info("Deleted all game history for player {}", playerId);
    }

    /**
     * Delete game history related to a specific game
     */
    @Transactional
    public void deleteGameHistoryForGame(UUID gameId) {
        List<PlayerGameHistory> entries = gameHistoryRepository.findByGameId(gameId);
        if (!entries.isEmpty()) {
            gameHistoryRepository.deleteAll(entries);
            log.info("Deleted {} game history entries for game {}", entries.size(), gameId);
        }
    }

    /**
     * Find games by date range
     */
    @Transactional(readOnly = true)
    public List<PlayerGameHistory> findByDateRange(UUID playerId, Date startDate, Date endDate) {
        return gameHistoryRepository.findByPlayerIdAndGameDateBetween(playerId, startDate, endDate);
    }
}