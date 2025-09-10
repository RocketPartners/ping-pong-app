package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.EasterEggStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing EasterEggStats entities.
 * Handles aggregated easter egg statistics for players.
 */
@Repository
public interface EasterEggStatsRepository extends JpaRepository<EasterEggStats, UUID> {
    
    /**
     * Find stats for a specific player
     */
    Optional<EasterEggStats> findByPlayerId(UUID playerId);
    
    /**
     * Get leaderboard with custom limit
     */
    @Query("SELECT s FROM EasterEggStats s ORDER BY s.totalEggsFound DESC, s.totalPointsEarned DESC")
    List<EasterEggStats> findTopEggHunters(Pageable pageable);
    
    /**
     * Get players who have found at least one egg (for secret leaderboard access)
     */
    List<EasterEggStats> findByTotalEggsFoundGreaterThan(int minEggs);
    
    /**
     * Count total active egg hunters (players who have found at least one egg)
     */
    @Query("SELECT COUNT(s) FROM EasterEggStats s WHERE s.totalEggsFound > 0")
    long countActiveEggHunters();
    
    /**
     * Get aggregate statistics
     */
    @Query("SELECT SUM(s.totalEggsFound), SUM(s.totalPointsEarned), COUNT(s) FROM EasterEggStats s WHERE s.totalEggsFound > 0")
    Object[] getGlobalEggHuntingStats();
    
    /**
     * Find players who specialize in rare egg types
     */
    @Query("SELECT s FROM EasterEggStats s WHERE s.rareEggsFound > 0 OR s.epicEggsFound > 0 OR s.legendaryEggsFound > 0 OR s.mythicalEggsFound > 0 ORDER BY (COALESCE(s.rareEggsFound, 0) + COALESCE(s.epicEggsFound, 0) * 2 + COALESCE(s.legendaryEggsFound, 0) * 3 + COALESCE(s.mythicalEggsFound, 0) * 10) DESC")
    List<EasterEggStats> findRareEggSpecialists(Pageable pageable);
    
    /**
     * Top rare egg finders
     */
    @Query("SELECT s FROM EasterEggStats s WHERE s.rareEggsFound > 0 ORDER BY s.rareEggsFound DESC")
    List<EasterEggStats> findTopRareEggFinders(Pageable pageable);
    
    /**
     * Top epic egg finders
     */
    @Query("SELECT s FROM EasterEggStats s WHERE s.epicEggsFound > 0 ORDER BY s.epicEggsFound DESC")
    List<EasterEggStats> findTopEpicEggFinders(Pageable pageable);
    
    /**
     * Top legendary egg finders
     */
    @Query("SELECT s FROM EasterEggStats s WHERE s.legendaryEggsFound > 0 ORDER BY s.legendaryEggsFound DESC")
    List<EasterEggStats> findTopLegendaryEggFinders(Pageable pageable);
    
    /**
     * Top mythical egg finders
     */
    @Query("SELECT s FROM EasterEggStats s WHERE s.mythicalEggsFound > 0 ORDER BY s.mythicalEggsFound DESC")
    List<EasterEggStats> findTopMythicalEggFinders(Pageable pageable);
    
    /**
     * Alternative method for getting top hunters - Spring Data naming convention
     */
    List<EasterEggStats> findTop50ByOrderByTotalEggsFoundDescTotalPointsEarnedDesc(Pageable pageable);
}