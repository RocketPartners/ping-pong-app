package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.PlayerGameHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Repository for querying player game history data from the player_game_history table.
 */
@Repository
public interface GameHistoryRepository extends JpaRepository<PlayerGameHistory, UUID> {

    /**
     * Find all game history entries for a specific player
     */
    List<PlayerGameHistory> findByPlayerId(UUID playerId);

    /**
     * Find all wins for a specific player
     */
    List<PlayerGameHistory> findByPlayerIdAndIsWinTrue(UUID playerId);

    /**
     * Find all losses for a specific player
     */
    List<PlayerGameHistory> findByPlayerIdAndIsWinFalse(UUID playerId);

    /**
     * Find game history entries by game type
     */
    List<PlayerGameHistory> findByPlayerIdAndGameType(UUID playerId, GameType gameType);

    /**
     * Find game history entries within a date range
     */
    List<PlayerGameHistory> findByPlayerIdAndGameDateBetween(UUID playerId, Date startDate, Date endDate);

    /**
     * Count wins by game type
     */
    @Query("SELECT COUNT(gh) FROM PlayerGameHistory gh WHERE gh.playerId = :playerId AND gh.gameType = :gameType AND gh.isWin = true")
    Long countWinsByGameType(@Param("playerId") UUID playerId, @Param("gameType") GameType gameType);

    /**
     * Get win percentage by game type
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN gh.isWin = true THEN 1 ELSE 0 END) * 1.0 / COUNT(gh), 0) " +
            "FROM PlayerGameHistory gh WHERE gh.playerId = :playerId AND gh.gameType = :gameType")
    Double getWinPercentageByGameType(@Param("playerId") UUID playerId, @Param("gameType") GameType gameType);

    /**
     * Get recent game results
     */
    List<PlayerGameHistory> findByPlayerIdOrderByGameDateDesc(UUID playerId);

    /**
     * Get recent game results with pagination
     */
    Page<PlayerGameHistory> findByPlayerId(UUID playerId, Pageable pageable);

    /**
     * Find all player IDs with a specific game type
     */
    @Query("SELECT DISTINCT gh.playerId FROM PlayerGameHistory gh WHERE gh.gameType = :gameType")
    List<UUID> findPlayerIdsByGameType(@Param("gameType") GameType gameType);
}