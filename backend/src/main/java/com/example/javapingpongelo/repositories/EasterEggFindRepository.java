package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.EasterEggFind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing EasterEggFind entities.
 * Tracks when players find easter eggs.
 */
@Repository
public interface EasterEggFindRepository extends JpaRepository<EasterEggFind, UUID> {
    
    /**
     * Find all eggs found by a specific player, ordered by most recent first
     */
    List<EasterEggFind> findByPlayerIdOrderByFoundAtDesc(UUID playerId);
    
    /**
     * Get the most recent egg finds across all players
     */
    List<EasterEggFind> findTop10ByOrderByFoundAtDesc();
    
    /**
     * Count how many eggs a player has found in a specific time period
     */
    @Query("SELECT COUNT(f) FROM EasterEggFind f WHERE f.playerId = :playerId AND f.foundAt >= :since")
    long countPlayerFindsInPeriod(@Param("playerId") UUID playerId, @Param("since") Date since);
    
    /**
     * Get player's egg finding streak data
     */
    @Query("SELECT f FROM EasterEggFind f WHERE f.playerId = :playerId ORDER BY f.foundAt DESC")
    List<EasterEggFind> findPlayerFindsForStreak(@Param("playerId") UUID playerId);
    
    /**
     * Count total eggs found by a player
     */
    long countByPlayerId(UUID playerId);
    
    /**
     * Find eggs found by type for a player
     */
    @Query("SELECT COUNT(f) FROM EasterEggFind f WHERE f.playerId = :playerId AND f.eggType = :eggType")
    long countByPlayerIdAndEggType(@Param("playerId") UUID playerId, @Param("eggType") EasterEgg.EggType eggType);
    
    /**
     * Get recent finds for activity feed
     */
    @Query("SELECT f FROM EasterEggFind f ORDER BY f.foundAt DESC")
    List<EasterEggFind> findRecentFinds(Pageable pageable);
    
    /**
     * Find top egg hunters by total eggs found
     */
    @Query("SELECT f.playerId, COUNT(f) as totalFound FROM EasterEggFind f GROUP BY f.playerId ORDER BY totalFound DESC")
    List<Object[]> findTopEggHunters(Pageable pageable);
    
    /**
     * Get page statistics - which pages have most egg finds
     */
    @Query("SELECT f.pageFoundOn, COUNT(f) as findCount FROM EasterEggFind f GROUP BY f.pageFoundOn ORDER BY findCount DESC")
    List<Object[]> getPageFindStatistics();
    
    /**
     * Count eggs found between dates for audit reports
     */
    long countByFoundAtBetween(Date startDate, Date endDate);
    
    /**
     * Count distinct players who found eggs in a time period
     */
    @Query("SELECT COUNT(DISTINCT f.playerId) FROM EasterEggFind f WHERE f.foundAt BETWEEN :startDate AND :endDate")
    long countDistinctPlayersByFoundAtBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * Count eggs by type in a time period
     */
    long countByEggTypeAndFoundAtBetween(EasterEgg.EggType eggType, Date startDate, Date endDate);
    
    /**
     * Get most active pages in a time period for audit reports
     */
    @Query("SELECT f.pageFoundOn, COUNT(f) as findCount FROM EasterEggFind f " +
           "WHERE f.foundAt BETWEEN :startDate AND :endDate " +
           "GROUP BY f.pageFoundOn ORDER BY findCount DESC")
    List<Object[]> findMostActivePagesByPeriod(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    /**
     * Check if player has found a specific egg (validation)
     */
    java.util.Optional<EasterEggFind> findByPlayerIdAndEggId(UUID playerId, UUID eggId);
    
    /**
     * Count recent claims by a player (rate limiting)
     */
    long countByPlayerIdAndFoundAtAfter(UUID playerId, Date since);
}