package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerEloHistory;
import com.example.javapingpongelo.models.dto.PlayerEloHistoryDTO;
import com.example.javapingpongelo.models.dto.PlayerRankHistoryDTO;
import com.example.javapingpongelo.repositories.PlayerEloHistoryRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing player ELO history and ranking
 */
@Service
@Slf4j
public class PlayerEloHistoryService {

    @Autowired
    private PlayerEloHistoryRepository eloHistoryRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Record ELO change for a player after a game
     */
    @Transactional
    public PlayerEloHistory recordEloChange(UUID playerId, UUID gameId, GameType gameType,
                                            int previousElo, int newElo) {
        // Get the current timestamp
        Date now = new Date();

        // Calculate the player's rank position
        int rankPosition = calculatePlayerRankPosition(playerId, gameType, newElo);
        int totalPlayers = countPlayersWithGameType(gameType);

        // Calculate percentile (higher is better)
        double percentile = 100.0 * (1.0 - ((double) rankPosition / totalPlayers));

        // Log calculations for debugging
        log.debug("ELO change calculation for player {}: rank={}, totalPlayers={}, percentile={}",
                  playerId, rankPosition, totalPlayers, percentile);

        // Create and save the ELO history record
        PlayerEloHistory history = PlayerEloHistory.builder()
                                                   .playerId(playerId)
                                                   .gameId(gameId)
                                                   .timestamp(now)
                                                   .gameType(gameType)
                                                   .previousElo(previousElo)
                                                   .newElo(newElo)
                                                   .eloChange(newElo - previousElo)
                                                   .rankPosition(rankPosition)
                                                   .totalPlayers(totalPlayers)
                                                   .percentile(percentile)
                                                   .build();

        return eloHistoryRepository.save(history);
    }

    /**
     * Calculate a player's current rank position for a game type
     */
    private int calculatePlayerRankPosition(UUID playerId, GameType gameType, int newElo) {
        // Get all players and their current ELO for this game type
        List<Player> allPlayers = playerRepository.findAll();

        // Create a map of player IDs to their current ELO ratings for the specified game type
        Map<UUID, Integer> playerEloMap = new HashMap<>();

        for (Player player : allPlayers) {
            int elo = switch (gameType) {
                case SINGLES_RANKED -> player.getSinglesRankedRating();
                case DOUBLES_RANKED -> player.getDoublesRankedRating();
                case SINGLES_NORMAL -> player.getSinglesNormalRating();
                case DOUBLES_NORMAL -> player.getDoublesNormalRating();
            };

            // Get the appropriate ELO rating based on game type

            // If this is the current player, use the new ELO value
            if (player.getPlayerId().equals(playerId)) {
                playerEloMap.put(player.getPlayerId(), newElo);
            }
            else {
                playerEloMap.put(player.getPlayerId(), elo);
            }
        }

        // Sort players by ELO in descending order
        List<Map.Entry<UUID, Integer>> sortedElo = playerEloMap.entrySet()
                                                               .stream()
                                                               .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                                                               .collect(Collectors.toList());

        // Find the current player's position (1-based index)
        for (int i = 0; i < sortedElo.size(); i++) {
            if (sortedElo.get(i).getKey().equals(playerId)) {
                return i + 1; // 1-based ranking
            }
        }

        // Default to last place if not found
        return allPlayers.size();
    }

    /**
     * Count players who have played games of a specific type
     */
    private int countPlayersWithGameType(GameType gameType) {
        // In a real production system, we would filter based on whether players
        // have actually played games of this type, but for now we'll return
        // the total count to match the behavior of calculatePlayerRankPosition
        return (int) playerRepository.count();
    }

    /**
     * Record ELO change for a player after a match (which may include multiple games)
     */
    @Transactional
    public PlayerEloHistory recordMatchEloChange(UUID playerId, UUID matchId, GameType gameType,
                                                 int previousElo, int newElo) {
        // Get the current timestamp
        Date now = new Date();

        // Calculate the player's rank position
        int rankPosition = calculatePlayerRankPosition(playerId, gameType, newElo);
        int totalPlayers = countPlayersWithGameType(gameType);

        // Create and save the ELO history record
        PlayerEloHistory history = PlayerEloHistory.builder()
                                                   .playerId(playerId)
                                                   .matchId(matchId)
                                                   .timestamp(now)
                                                   .gameType(gameType)
                                                   .previousElo(previousElo)
                                                   .newElo(newElo)
                                                   .rankPosition(rankPosition)
                                                   .totalPlayers(totalPlayers)
                                                   .build();

        return eloHistoryRepository.save(history);
    }

    /**
     * Get ELO history for a player
     */
    @Transactional(readOnly = true)
    public List<PlayerEloHistory> getPlayerEloHistory(UUID playerId) {
        return eloHistoryRepository.findByPlayerId(playerId);
    }

    /**
     * Get ELO history for a player with pagination
     */
    @Transactional(readOnly = true)
    public Page<PlayerEloHistory> getPlayerEloHistory(UUID playerId, Pageable pageable) {
        return eloHistoryRepository.findByPlayerId(playerId, pageable);
    }

    /**
     * Get ELO history for a player and game type
     */
    @Transactional(readOnly = true)
    public List<PlayerEloHistory> getPlayerEloHistoryByGameType(UUID playerId, GameType gameType) {
        return eloHistoryRepository.findByPlayerIdAndGameType(playerId, gameType);
    }

    /**
     * Get ELO history for a player and game type with date range
     */
    @Transactional(readOnly = true)
    public List<PlayerEloHistory> getPlayerEloHistoryByGameTypeAndDateRange(
            UUID playerId, GameType gameType, Date startDate, Date endDate) {
        return eloHistoryRepository.findByPlayerIdAndGameTypeAndTimestampBetweenOrderByTimestampAsc(
                playerId, gameType, startDate, endDate);
    }

    /**
     * Get a player's rank history for a specific game type
     */
    @Transactional(readOnly = true)
    public List<PlayerRankHistoryDTO> getPlayerRankHistory(UUID playerId, GameType gameType) {
        List<Object[]> rankHistory = eloHistoryRepository.findPlayerRankHistoryByGameType(playerId, gameType);

        List<PlayerRankHistoryDTO> result = new ArrayList<>();
        for (Object[] record : rankHistory) {
            PlayerRankHistoryDTO dto = new PlayerRankHistoryDTO();
            dto.setTimestamp((Date) record[0]);
            dto.setRankPosition((Integer) record[1]);
            dto.setTotalPlayers((Integer) record[2]);
            dto.setPercentile((Double) record[3]);
            result.add(dto);
        }

        return result;
    }

    /**
     * Get a player's ELO history for a specific game type, formatted for charting
     */
    @Transactional(readOnly = true)
    public List<PlayerEloHistoryDTO> getPlayerEloHistoryForChart(UUID playerId, GameType gameType) {
        List<PlayerEloHistory> history = eloHistoryRepository.findByPlayerIdAndGameType(playerId, gameType);

        return history.stream()
                      .map(h -> new PlayerEloHistoryDTO(h.getTimestamp(), h.getNewElo(), h.getEloChange()))
                      .collect(Collectors.toList());
    }

    /**
     * Find players with similar ELO trajectories
     */
    @Transactional(readOnly = true)
    public List<UUID> findSimilarPlayers(UUID playerId, GameType gameType) {
        // Look at players with similar trajectory in the last 30 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = cal.getTime();

        return eloHistoryRepository.findSimilarPlayers(playerId, gameType, thirtyDaysAgo);
    }
}