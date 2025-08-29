package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.PlayerStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PlayerStatistics entities
 */
@Repository
public interface PlayerStatisticsRepository extends JpaRepository<PlayerStatistics, UUID> {
    
    /**
     * Find statistics by player ID
     */
    Optional<PlayerStatistics> findByPlayerId(UUID playerId);

    /**
     * Find players with statistics older than the given date
     */
    @Query("SELECT ps FROM PlayerStatistics ps WHERE ps.lastUpdated < :cutoffDate")
    List<PlayerStatistics> findByLastUpdatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find top players by win rate (minimum games required)
     */
    @Query("SELECT ps FROM PlayerStatistics ps " +
           "WHERE ps.totalGames >= :minGames " +
           "ORDER BY (ps.totalWins * 100.0 / ps.totalGames) DESC")
    List<PlayerStatistics> findTopPlayersByWinRate(@Param("minGames") int minGames);

    /**
     * Find players with the longest current win streaks
     */
    @Query("SELECT ps FROM PlayerStatistics ps " +
           "WHERE ps.currentWinStreak > 0 " +
           "ORDER BY ps.currentWinStreak DESC")
    List<PlayerStatistics> findPlayersByCurrentWinStreak();

    /**
     * Update total games and wins for a player
     */
    @Modifying
    @Query("UPDATE PlayerStatistics ps " +
           "SET ps.totalGames = ps.totalGames + 1, " +
           "    ps.totalWins = ps.totalWins + :winsToAdd, " +
           "    ps.totalLosses = ps.totalLosses + :lossesToAdd, " +
           "    ps.lastUpdated = CURRENT_TIMESTAMP " +
           "WHERE ps.playerId = :playerId")
    void updateGameCounts(@Param("playerId") UUID playerId, 
                         @Param("winsToAdd") int winsToAdd, 
                         @Param("lossesToAdd") int lossesToAdd);

    /**
     * Bulk initialize statistics for players who don't have them
     */
    @Query("SELECT p.playerId FROM Player p " +
           "WHERE p.playerId NOT IN (SELECT ps.playerId FROM PlayerStatistics ps)")
    List<UUID> findPlayersWithoutStatistics();
}