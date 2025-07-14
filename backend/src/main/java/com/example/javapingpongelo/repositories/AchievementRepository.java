package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    /**
     * Find achievements by category
     */
    List<Achievement> findByCategory(Achievement.AchievementCategory category);

    /**
     * Find achievements by visibility
     */
    List<Achievement> findByIsVisible(boolean isVisible);

    /**
     * Find achievements by type
     */
    List<Achievement> findByType(Achievement.AchievementType type);
}