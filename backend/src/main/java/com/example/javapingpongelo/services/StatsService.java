package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerAchievement;
import com.example.javapingpongelo.models.dto.PlayerStatsDTO;
import com.example.javapingpongelo.models.dto.SystemStatsDTO;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating system-wide statistics
 */
@Service
@Slf4j
public class StatsService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private com.example.javapingpongelo.repositories.PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    private com.example.javapingpongelo.repositories.AchievementRepository achievementRepository;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Force refresh of cached statistics
     */
    @CacheEvict(value = "systemStats", allEntries = true)
    public void refreshStats() {
        log.info("Forced refresh of system statistics");

        // Get a proxy reference to this service through the ApplicationContext
        // This ensures that the @Cacheable annotation is processed
        StatsService statsService = applicationContext.getBean(StatsService.class);

        // Call the method through the proxy
        statsService.getSystemStats();
    }

    /**
     * Get comprehensive system statistics with caching
     */
    @Cacheable(value = "systemStats", key = "'stats'")
    @Transactional(readOnly = true)
    public SystemStatsDTO getSystemStats() {
        // Log that we're generating fresh stats (will only happen on cache misses)
        log.info("Generating fresh system-wide statistics");

        try {
            // Calculate all statistics
            List<PlayerStatsDTO> recentPlayers = getRecentPlayers(5);
            int totalPlayers = getTotalPlayers();
            Map<String, PlayerStatsDTO> topRatedPlayers = getTopRatedPlayers();

            int totalGames = getTotalGames();
            Map<String, Integer> gamesByType = getGamesByType();
            int totalPointsScored = getTotalPointsScored();
            double averageScore = getAverageScore();

            int totalAchievementsEarned = getTotalAchievementsEarned();
            int cumulativeAchievementScore = getCumulativeAchievementScore();
            Map<String, Integer> topAchievements = getTopAchievements(5);

            double avgGamesPerPlayer = totalPlayers > 0 ? (double) totalGames / totalPlayers : 0;
            double avgAchievementsPerPlayer = totalPlayers > 0 ? (double) totalAchievementsEarned / totalPlayers : 0;

            Map<String, Integer> winDistribution = getWinDistribution();

            // Build the stats DTO
            return SystemStatsDTO.builder()
                                 .recentPlayers(recentPlayers)
                                 .totalPlayers(totalPlayers)
                                 .topRatedPlayers(topRatedPlayers)
                                 .totalGames(totalGames)
                                 .gamesByType(gamesByType)
                                 .totalPointsScored(totalPointsScored)
                                 .averageScore(averageScore)
                                 .totalAchievementsEarned(totalAchievementsEarned)
                                 .cumulativeAchievementScore(cumulativeAchievementScore)
                                 .topAchievements(topAchievements)
                                 .averageGamesPerPlayer(avgGamesPerPlayer)
                                 .averageAchievementsPerPlayer(avgAchievementsPerPlayer)
                                 .winDistribution(winDistribution)
                                 .build();
        }
        catch (Exception e) {
            log.error("Error generating system statistics", e);
            throw new RuntimeException("Failed to generate system statistics", e);
        }
    }

    // Remaining methods stay the same

    /**
     * Get the most recently registered players
     */
    private List<PlayerStatsDTO> getRecentPlayers(int count) {
        try {
            // Sort by created date in descending order and limit to count
            List<Player> recentPlayers = playerRepository.findAll(
                    PageRequest.of(0, count, Sort.by(Sort.Direction.DESC, "created"))
            ).getContent();

            // Convert to DTOs to avoid lazy loading issues
            return recentPlayers.stream()
                                .map(PlayerStatsDTO::fromPlayer)
                                .collect(Collectors.toList());
        }
        catch (Exception e) {
            log.error("Error retrieving recent players", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get total number of registered players
     */
    private int getTotalPlayers() {
        return (int) playerRepository.count();
    }

    /**
     * Get top-rated player for each game type
     */
    private Map<String, PlayerStatsDTO> getTopRatedPlayers() {
        try {
            List<Player> allPlayers = playerRepository.findAll();
            Map<String, PlayerStatsDTO> topPlayers = new HashMap<>();

            if (allPlayers.isEmpty()) {
                return topPlayers;
            }

            // Find top player for each rating type
            Player topSinglesRanked = allPlayers.stream()
                                                .max(Comparator.comparingInt(Player::getSinglesRankedRating))
                                                .orElse(null);

            Player topSinglesNormal = allPlayers.stream()
                                                .max(Comparator.comparingInt(Player::getSinglesNormalRating))
                                                .orElse(null);

            Player topDoublesRanked = allPlayers.stream()
                                                .max(Comparator.comparingInt(Player::getDoublesRankedRating))
                                                .orElse(null);

            Player topDoublesNormal = allPlayers.stream()
                                                .max(Comparator.comparingInt(Player::getDoublesNormalRating))
                                                .orElse(null);

            // Convert to DTOs to avoid lazy loading issues
            topPlayers.put("Singles Ranked", PlayerStatsDTO.fromPlayer(topSinglesRanked));
            topPlayers.put("Singles Normal", PlayerStatsDTO.fromPlayer(topSinglesNormal));
            topPlayers.put("Doubles Ranked", PlayerStatsDTO.fromPlayer(topDoublesRanked));
            topPlayers.put("Doubles Normal", PlayerStatsDTO.fromPlayer(topDoublesNormal));

            return topPlayers;
        }
        catch (Exception e) {
            log.error("Error retrieving top rated players", e);
            return new HashMap<>();
        }
    }

    /**
     * Get total number of games played - using optimized repository method
     */
    private int getTotalGames() {
        return (int) gameRepository.countTotalGames();
    }

    /**
     * Get breakdown of games by type - using optimized repository methods
     */
    private Map<String, Integer> getGamesByType() {
        try {
            Map<String, Integer> gamesByType = new HashMap<>();

            // Use optimized repository methods instead of fetching all games
            int singlesRanked = (int) gameRepository.countSinglesRankedGames();
            int singlesNormal = (int) gameRepository.countSinglesNormalGames();
            int doublesRanked = (int) gameRepository.countDoublesRankedGames();
            int doublesNormal = (int) gameRepository.countDoublesNormalGames();

            gamesByType.put("Singles Ranked", singlesRanked);
            gamesByType.put("Singles Normal", singlesNormal);
            gamesByType.put("Doubles Ranked", doublesRanked);
            gamesByType.put("Doubles Normal", doublesNormal);

            return gamesByType;
        }
        catch (Exception e) {
            log.error("Error retrieving games by type", e);
            return new HashMap<>();
        }
    }

    /**
     * Get total points scored across all games - using optimized repository method
     */
    private int getTotalPointsScored() {
        try {
            Integer totalPoints = gameRepository.sumTotalPoints();
            return totalPoints != null ? totalPoints : 0;
        }
        catch (Exception e) {
            log.error("Error calculating total points scored", e);
            return 0;
        }
    }

    /**
     * Get average score per game - using optimized repository method
     */
    private double getAverageScore() {
        try {
            Double average = gameRepository.averageTotalScore();
            // The repository returns the total per game, but we want per team
            return average != null ? average / 2.0 : 0.0;
        }
        catch (Exception e) {
            log.error("Error calculating average score", e);
            return 0;
        }
    }

    /**
     * Get total number of achievements earned by all players
     */
    private int getTotalAchievementsEarned() {
        try {
            return (int) playerAchievementRepository.findAll().stream()
                                                    .filter(PlayerAchievement::getAchieved)
                                                    .count();
        }
        catch (Exception e) {
            log.error("Error counting total achievements earned", e);
            return 0;
        }
    }

    /**
     * Get cumulative achievement score across all players
     */
    private int getCumulativeAchievementScore() {
        try {
            List<PlayerAchievement> earnedAchievements = playerAchievementRepository.findAll().stream()
                                                                                    .filter(PlayerAchievement::getAchieved)
                                                                                    .toList();

            int totalScore = 0;

            for (PlayerAchievement pa : earnedAchievements) {
                try {
                    Achievement achievement = achievementRepository.findById(pa.getAchievementId()).orElse(null);
                    if (achievement != null) {
                        totalScore += achievement.getPoints();
                    }
                }
                catch (Exception e) {
                    log.warn("Error calculating achievement score for {}", pa.getAchievementId(), e);
                }
            }

            return totalScore;
        }
        catch (Exception e) {
            log.error("Error calculating cumulative achievement score", e);
            return 0;
        }
    }

    /**
     * Get top N most earned achievements
     */
    private Map<String, Integer> getTopAchievements(int count) {
        try {
            List<Achievement> allAchievements = achievementRepository.findAll();
            Map<String, Integer> achievementCounts = new HashMap<>();

            for (Achievement achievement : allAchievements) {
                int earnedCount = playerAchievementRepository.findByAchievementIdAndAchievedTrue(achievement.getId()).size();
                achievementCounts.put(achievement.getName(), earnedCount);
            }

            // Sort by count (descending) and limit to top N
            return achievementCounts.entrySet().stream()
                                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                                    .limit(count)
                                    .collect(Collectors.toMap(
                                            Map.Entry::getKey,
                                            Map.Entry::getValue,
                                            (e1, e2) -> e1,
                                            LinkedHashMap::new
                                    ));
        }
        catch (Exception e) {
            log.error("Error retrieving top achievements", e);
            return new HashMap<>();
        }
    }

    /**
     * Get distribution of wins among players
     */
    private Map<String, Integer> getWinDistribution() {
        try {
            List<Player> allPlayers = playerRepository.findAll();
            Map<String, Integer> distribution = new HashMap<>();

            // Count players with different win ranges
            int zeroWins = 0;
            int oneToFiveWins = 0;
            int sixToTenWins = 0;
            int elevenToTwentyWins = 0;
            int twentyPlusWins = 0;

            for (Player player : allPlayers) {
                int totalWins = player.getSinglesRankedWins() + player.getSinglesNormalWins() +
                        player.getDoublesRankedWins() + player.getDoublesNormalWins();

                if (totalWins == 0) zeroWins++;
                else if (totalWins >= 1 && totalWins <= 5) oneToFiveWins++;
                else if (totalWins >= 6 && totalWins <= 10) sixToTenWins++;
                else if (totalWins >= 11 && totalWins <= 20) elevenToTwentyWins++;
                else twentyPlusWins++;
            }

            distribution.put("0 wins", zeroWins);
            distribution.put("1-5 wins", oneToFiveWins);
            distribution.put("6-10 wins", sixToTenWins);
            distribution.put("11-20 wins", elevenToTwentyWins);
            distribution.put("20+ wins", twentyPlusWins);

            return distribution;
        }
        catch (Exception e) {
            log.error("Error calculating win distribution", e);
            return new HashMap<>();
        }
    }
}