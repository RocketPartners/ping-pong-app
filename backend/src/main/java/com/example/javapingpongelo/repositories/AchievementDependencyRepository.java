package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.AchievementDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for AchievementDependency entities
 */
@Repository
public interface AchievementDependencyRepository extends JpaRepository<AchievementDependency, UUID> {
    
    /**
     * Find all prerequisites for a specific achievement
     */
    List<AchievementDependency> findByAchievementId(UUID achievementId);

    /**
     * Find all achievements that depend on a specific prerequisite
     */
    List<AchievementDependency> findByPrerequisiteAchievementId(UUID prerequisiteAchievementId);

    /**
     * Find required prerequisites for an achievement
     */
    List<AchievementDependency> findByAchievementIdAndDependencyType(
            UUID achievementId, AchievementDependency.DependencyType dependencyType);

    /**
     * Check if an achievement has any prerequisites
     */
    boolean existsByAchievementId(UUID achievementId);

    /**
     * Get all achievement IDs that should be unlocked when a prerequisite is completed
     */
    @Query("SELECT ad.achievementId FROM AchievementDependency ad " +
           "WHERE ad.prerequisiteAchievementId = :prerequisiteId " +
           "AND ad.dependencyType = :dependencyType")
    List<UUID> findAchievementIdsUnlockedBy(@Param("prerequisiteId") UUID prerequisiteId,
                                           @Param("dependencyType") AchievementDependency.DependencyType dependencyType);

    /**
     * Get achievements with their dependency counts for sorting/display
     */
    @Query("SELECT ad.achievementId, COUNT(ad) as depCount FROM AchievementDependency ad " +
           "WHERE ad.dependencyType = 'REQUIRED' " +
           "GROUP BY ad.achievementId " +
           "ORDER BY depCount ASC")
    List<Object[]> findAchievementsWithDependencyCounts();

    /**
     * Find circular dependencies (for validation)
     */
    @Query("SELECT ad FROM AchievementDependency ad " +
           "WHERE ad.achievementId = :achievementId " +
           "AND ad.prerequisiteAchievementId IN " +
           "(SELECT ad2.achievementId FROM AchievementDependency ad2 " +
           " WHERE ad2.prerequisiteAchievementId = :achievementId)")
    List<AchievementDependency> findCircularDependencies(@Param("achievementId") UUID achievementId);
}