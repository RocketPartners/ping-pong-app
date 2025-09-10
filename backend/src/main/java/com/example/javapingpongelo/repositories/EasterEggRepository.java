package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.EasterEgg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing EasterEgg entities.
 * Handles database operations for easter egg hunting system.
 */
@Repository
public interface EasterEggRepository extends JpaRepository<EasterEgg, UUID> {
    
    /**
     * Find an active easter egg by ID
     */
    Optional<EasterEgg> findByIdAndIsActive(UUID id, boolean isActive);
    
    /**
     * Find the current active easter egg (should be only one)
     * Returns the most recently spawned if multiple exist
     */
    @Query("SELECT e FROM EasterEgg e WHERE e.isActive = true ORDER BY e.spawnedAt DESC")
    List<EasterEgg> findAllActiveEggs();
    
    /**
     * Find all active easter eggs on a specific page
     */
    List<EasterEgg> findByPageLocationAndIsActive(String pageLocation, boolean isActive);
    
    /**
     * Find all easter eggs spawned within a time range
     */
    List<EasterEgg> findBySpawnedAtBetween(Date startDate, Date endDate);
    
    /**
     * Count total eggs found since a specific date
     */
    @Query("SELECT COUNT(e) FROM EasterEgg e WHERE e.foundAt >= :since AND e.foundAt IS NOT NULL")
    long countEggsFoundSince(@Param("since") Date since);
    
    /**
     * Count currently active eggs
     */
    long countByIsActiveTrue();
    
    /**
     * Find expired eggs that should be removed (older than specified time)
     */
    @Query("SELECT e FROM EasterEgg e WHERE e.isActive = true AND e.spawnedAt < :cutoffTime")
    List<EasterEgg> findExpiredActiveEggs(@Param("cutoffTime") Date cutoffTime);
    
    /**
     * Find all eggs found by a specific player
     */
    List<EasterEgg> findByFoundByPlayerIdOrderByFoundAtDesc(UUID playerId);
    
    /**
     * Get recent egg activity for analytics
     */
    @Query("SELECT e FROM EasterEgg e WHERE e.foundAt IS NOT NULL ORDER BY e.foundAt DESC")
    List<EasterEgg> findRecentFinds(@Param("limit") int limit);
    
    /**
     * Find old inactive eggs that need to be cleaned up
     */
    @Query("SELECT e FROM EasterEgg e WHERE e.isActive = false AND e.spawnedAt < :cutoffDate")
    List<EasterEgg> findByIsActiveFalseAndSpawnedAtBefore(@Param("cutoffDate") Date cutoffDate);
}