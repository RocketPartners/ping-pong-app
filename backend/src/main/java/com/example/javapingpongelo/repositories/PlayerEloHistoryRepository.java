package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.PlayerEloHistory;
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
 * Repository for tracking player ELO rating history
 */
@Repository
public interface PlayerEloHistoryRepository extends JpaRepository<PlayerEloHistory, UUID> {

    /**
     * Find all ELO history entries for a specific player
     */
    List<PlayerEloHistory> findByPlayerId(UUID playerId);

    /**
     * Find all ELO history entries for a specific player with pagination
     */
    Page<PlayerEloHistory> findByPlayerId(UUID playerId, Pageable pageable);

    /**
     * Find all ELO history entries for a specific player and game type
     */
    List<PlayerEloHistory> findByPlayerIdAndGameType(UUID playerId, GameType gameType);

    /**
     * Find all ELO history entries for a specific player and game type with pagination
     */
    Page<PlayerEloHistory> findByPlayerIdAndGameType(UUID playerId, GameType gameType, Pageable pageable);

    /**
     * Find all ELO history entries for a specific player within a date range
     */
    List<PlayerEloHistory> findByPlayerIdAndTimestampBetweenOrderByTimestampAsc(
            UUID playerId, Date startDate, Date endDate);

    /**
     * Find all ELO history entries for a specific player and game type within a date range
     */
    List<PlayerEloHistory> findByPlayerIdAndGameTypeAndTimestampBetweenOrderByTimestampAsc(
            UUID playerId, GameType gameType, Date startDate, Date endDate);

    /**
     * Find the most recent ELO history entry for a player and game type
     */
    PlayerEloHistory findFirstByPlayerIdAndGameTypeOrderByTimestampDesc(UUID playerId, GameType gameType);

    /**
     * Get the ELO history for a specific game
     */
    List<PlayerEloHistory> findByGameId(UUID gameId);

    /**
     * Get the ELO history for a specific match
     */
    List<PlayerEloHistory> findByMatchId(UUID matchId);

    /**
     * Get a player's rank history (just the rank positions) for a specific game type
     */
    @Query("SELECT timestamp, rankPosition, totalPlayers, percentile FROM PlayerEloHistory " +
            "WHERE playerId = :playerId AND gameType = :gameType " +
            "ORDER BY timestamp ASC")
    List<Object[]> findPlayerRankHistoryByGameType(
            @Param("playerId") UUID playerId,
            @Param("gameType") GameType gameType);

    /**
     * Find players with similar ELO trajectory (for recommendations)
     */
    @Query(value = "SELECT DISTINCT peh.player_id FROM player_elo_history peh " +
            "WHERE peh.game_type = :gameType " +
            "AND peh.player_id != :playerId " +
            "AND peh.timestamp > :since " +
            "AND EXISTS (SELECT 1 FROM player_elo_history ref WHERE " +
            "   ref.player_id = :playerId AND " +
            "   ref.game_type = :gameType AND " +
            "   ref.new_elo BETWEEN peh.new_elo - 100 AND peh.new_elo + 100) " +
            "ORDER BY peh.timestamp DESC LIMIT 5",
            nativeQuery = true)
    List<UUID> findSimilarPlayers(
            @Param("playerId") UUID playerId,
            @Param("gameType") GameType gameType,
            @Param("since") Date since);
}