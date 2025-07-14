package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for system-wide statistics to display on home page
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Cacheable(value = "systemStats", key = "'stats'")
@Transactional(readOnly = true)
public class SystemStatsDTO {

    private List<PlayerStatsDTO> recentPlayers;

    private int totalPlayers;

    private Map<String, PlayerStatsDTO> topRatedPlayers;

    private int totalGames;

    private Map<String, Integer> gamesByType;

    private int totalPointsScored;

    private double averageScore;

    private int totalAchievementsEarned;

    private int cumulativeAchievementScore;

    private Map<String, Integer> topAchievements;

    private double averageGamesPerPlayer;

    private double averageAchievementsPerPlayer;

    private Map<String, Integer> winDistribution;
}