package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.AchievementDependency;
import com.example.javapingpongelo.models.PlayerAchievement;
import com.example.javapingpongelo.repositories.AchievementDependencyRepository;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.example.javapingpongelo.repositories.PlayerAchievementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing achievement dependencies and prerequisite checking.
 */
@Service
@Slf4j
public class AchievementDependencyService {

    @Autowired
    private AchievementDependencyRepository dependencyRepository;

    @Autowired
    private PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    /**
     * Checks if a player meets all prerequisites for an achievement
     */
    public boolean meetsPrerequisites(UUID playerId, UUID achievementId) {
        List<AchievementDependency> requiredDeps = dependencyRepository
                .findByAchievementIdAndDependencyType(achievementId, AchievementDependency.DependencyType.REQUIRED);

        if (requiredDeps.isEmpty()) {
            return true; // No prerequisites
        }

        // Get all player's completed achievements
        Set<UUID> completedAchievements = playerAchievementRepository
                .findByPlayerIdAndAchieved(playerId, true)
                .stream()
                .map(PlayerAchievement::getAchievementId)
                .collect(Collectors.toSet());

        // Check if all required prerequisites are met
        for (AchievementDependency dep : requiredDeps) {
            if (!completedAchievements.contains(dep.getPrerequisiteAchievementId())) {
                log.debug("Player {} missing prerequisite {} for achievement {}", 
                         playerId, dep.getPrerequisiteAchievementId(), achievementId);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets all achievements that should be unlocked when a prerequisite is completed
     */
    public List<UUID> getAchievementsUnlockedBy(UUID prerequisiteAchievementId) {
        return dependencyRepository.findAchievementIdsUnlockedBy(
                prerequisiteAchievementId, AchievementDependency.DependencyType.UNLOCKS);
    }

    /**
     * Gets all missing prerequisites for an achievement
     */
    public List<Achievement> getMissingPrerequisites(UUID playerId, UUID achievementId) {
        List<AchievementDependency> requiredDeps = dependencyRepository
                .findByAchievementIdAndDependencyType(achievementId, AchievementDependency.DependencyType.REQUIRED);

        if (requiredDeps.isEmpty()) {
            return List.of(); // No prerequisites
        }

        // Get all player's completed achievements
        Set<UUID> completedAchievements = playerAchievementRepository
                .findByPlayerIdAndAchieved(playerId, true)
                .stream()
                .map(PlayerAchievement::getAchievementId)
                .collect(Collectors.toSet());

        // Find missing prerequisites
        List<UUID> missingPrereqIds = requiredDeps.stream()
                .map(AchievementDependency::getPrerequisiteAchievementId)
                .filter(prereqId -> !completedAchievements.contains(prereqId))
                .collect(Collectors.toList());

        return achievementRepository.findAllById(missingPrereqIds);
    }

    /**
     * Gets all prerequisites for an achievement (for display purposes)
     */
    public List<Achievement> getPrerequisites(UUID achievementId) {
        List<AchievementDependency> deps = dependencyRepository.findByAchievementId(achievementId);
        
        List<UUID> prerequisiteIds = deps.stream()
                .map(AchievementDependency::getPrerequisiteAchievementId)
                .collect(Collectors.toList());

        return achievementRepository.findAllById(prerequisiteIds);
    }

    /**
     * Gets all achievements that depend on a specific achievement
     */
    public List<Achievement> getDependentAchievements(UUID prerequisiteAchievementId) {
        List<AchievementDependency> deps = dependencyRepository
                .findByPrerequisiteAchievementId(prerequisiteAchievementId);
        
        List<UUID> dependentIds = deps.stream()
                .map(AchievementDependency::getAchievementId)
                .collect(Collectors.toList());

        return achievementRepository.findAllById(dependentIds);
    }

    /**
     * Creates a new dependency relationship
     */
    @Transactional
    public AchievementDependency createDependency(UUID achievementId, UUID prerequisiteId, 
                                                 AchievementDependency.DependencyType type) {
        // Validate that achievements exist
        if (!achievementRepository.existsById(achievementId)) {
            throw new IllegalArgumentException("Achievement not found: " + achievementId);
        }
        if (!achievementRepository.existsById(prerequisiteId)) {
            throw new IllegalArgumentException("Prerequisite achievement not found: " + prerequisiteId);
        }

        // Check for circular dependencies
        if (wouldCreateCircularDependency(achievementId, prerequisiteId)) {
            throw new IllegalArgumentException("Creating this dependency would create a circular dependency");
        }

        AchievementDependency dependency = AchievementDependency.builder()
                .achievementId(achievementId)
                .prerequisiteAchievementId(prerequisiteId)
                .dependencyType(type)
                .build();

        return dependencyRepository.save(dependency);
    }

    /**
     * Checks if creating a dependency would create a circular dependency
     */
    private boolean wouldCreateCircularDependency(UUID achievementId, UUID prerequisiteId) {
        // Simple check: if prerequisite depends on achievement, it would be circular
        return dependencyRepository.findByAchievementId(prerequisiteId)
                .stream()
                .anyMatch(dep -> dep.getPrerequisiteAchievementId().equals(achievementId));
    }

    /**
     * Removes a dependency relationship
     */
    @Transactional
    public void removeDependency(UUID achievementId, UUID prerequisiteId) {
        List<AchievementDependency> deps = dependencyRepository.findByAchievementId(achievementId);
        deps.stream()
            .filter(dep -> dep.getPrerequisiteAchievementId().equals(prerequisiteId))
            .forEach(dependencyRepository::delete);
    }

    /**
     * Gets achievements that should be visible to a player (based on prerequisites)
     */
    public List<Achievement> getVisibleAchievements(UUID playerId) {
        List<Achievement> allVisibleAchievements = achievementRepository.findByIsVisible(true);
        
        // Get player's completed achievements
        Set<UUID> completedAchievements = playerAchievementRepository
                .findByPlayerIdAndAchieved(playerId, true)
                .stream()
                .map(PlayerAchievement::getAchievementId)
                .collect(Collectors.toSet());

        return allVisibleAchievements.stream()
                .filter(achievement -> {
                    // Always show achievements with no dependencies
                    if (!dependencyRepository.existsByAchievementId(achievement.getId())) {
                        return true;
                    }

                    // Check UNLOCKS dependencies - only show if prerequisites are met
                    List<AchievementDependency> unlockDeps = dependencyRepository
                            .findByAchievementIdAndDependencyType(achievement.getId(), 
                                                                AchievementDependency.DependencyType.UNLOCKS);
                    
                    if (!unlockDeps.isEmpty()) {
                        // Must have at least one prerequisite completed to show
                        return unlockDeps.stream()
                                .anyMatch(dep -> completedAchievements.contains(dep.getPrerequisiteAchievementId()));
                    }

                    // For REQUIRED dependencies, show the achievement but indicate prerequisites
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Sets up default dependencies for existing achievements
     */
    @Transactional
    public void setupDefaultDependencies() {
        log.info("Setting up default achievement dependencies...");
        
        try {
            // Create some example dependency chains
            
            // Win Count Chain: First Steps -> Century Club -> The Legend
            createDependencyChain("First Steps", "Century Club", AchievementDependency.DependencyType.SUGGESTED);
            createDependencyChain("Century Club", "The Legend", AchievementDependency.DependencyType.SUGGESTED);
            
            // Rating Chain: Rising Star -> Elite Player -> Grandmaster
            createDependencyChain("Rising Star", "Elite Player", AchievementDependency.DependencyType.SUGGESTED);
            createDependencyChain("Elite Player", "Grandmaster", AchievementDependency.DependencyType.SUGGESTED);
            
            // Tournament Chain: Tournament Contender -> Tournament Champion -> Tournament Dynasty
            createDependencyChain("Tournament Contender", "Tournament Champion", AchievementDependency.DependencyType.REQUIRED);
            createDependencyChain("Tournament Champion", "Tournament Dynasty", AchievementDependency.DependencyType.REQUIRED);
            
            // Unlock Chain: First Steps unlocks more advanced achievements
            createDependencyByName("Century Club", "First Steps", AchievementDependency.DependencyType.UNLOCKS);
            
            log.info("Successfully set up default achievement dependencies");
        } catch (Exception e) {
            log.error("Error setting up default dependencies", e);
        }
    }

    private void createDependencyChain(String prerequisiteName, String achievementName, 
                                     AchievementDependency.DependencyType type) {
        try {
            Achievement prerequisite = achievementRepository.findByName(prerequisiteName).orElse(null);
            Achievement achievement = achievementRepository.findByName(achievementName).orElse(null);
            
            if (prerequisite != null && achievement != null) {
                createDependency(achievement.getId(), prerequisite.getId(), type);
                log.debug("Created {} dependency: {} -> {}", type, prerequisiteName, achievementName);
            }
        } catch (Exception e) {
            log.warn("Could not create dependency {} -> {}: {}", prerequisiteName, achievementName, e.getMessage());
        }
    }

    private void createDependencyByName(String achievementName, String prerequisiteName, 
                                      AchievementDependency.DependencyType type) {
        createDependencyChain(prerequisiteName, achievementName, type);
    }
}