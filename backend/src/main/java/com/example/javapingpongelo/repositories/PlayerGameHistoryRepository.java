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
 * Repository for the PlayerGameHistory entity.
 */
@Repository
public interface PlayerGameHistoryRepository extends JpaRepository<PlayerGameHistory, UUID> {

    /**
     * Find all game history entries for a specific player
     */
    List<PlayerGameHistory> findByPlayerId(UUID playerId);

    /**
     * Find all game history entries for a specific player, paginated
     */
    Page<PlayerGameHistory> findByPlayerId(UUID playerId, Pageable pageable);

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
    long countByPlayerIdAndGameTypeAndIsWinTrue(UUID playerId, GameType gameType);

    /**
     * Get win percentage by game type
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN gh.isWin = true THEN 1 ELSE 0 END) * 1.0 / COUNT(gh), 0) " +
            "FROM PlayerGameHistory gh WHERE gh.playerId = :playerId AND gh.gameType = :gameType")
    Double getWinPercentageByGameType(@Param("playerId") UUID playerId, @Param("gameType") GameType gameType);

    /**
     * Get recent game history entries ordered by date
     */
    List<PlayerGameHistory> findByPlayerIdOrderByGameDateDesc(UUID playerId);

    /**
     * Count consecutive wins (win streak)
     */
    @Query(value = "WITH ranked_games AS (" +
            "    SELECT *, " +
            "           ROW_NUMBER() OVER (ORDER BY game_date DESC) AS rn " +
            "    FROM player_game_history " +
            "    WHERE player_id = :playerId " +
            "    ORDER BY game_date DESC" +
            ") " +
            "SELECT COUNT(*) FROM (" +
            "    SELECT * FROM ranked_games " +
            "    WHERE is_win = true " +
            "    AND rn <= (SELECT MIN(rn) FROM ranked_games WHERE is_win = false)" +
            ") AS current_streak",
            nativeQuery = true)
    int getCurrentWinStreak(@Param("playerId") UUID playerId);

    /**
     * Delete all game history for a player
     */
    void deleteByPlayerId(UUID playerId);

    /**
     * Find all game history entries for a specific game
     */
    List<PlayerGameHistory> findByGameId(UUID gameId);
}