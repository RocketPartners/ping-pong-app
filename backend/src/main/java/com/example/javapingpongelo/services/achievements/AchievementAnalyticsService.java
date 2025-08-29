package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for calculating and managing achievement analytics.
 * Provides insights into achievement performance, difficulty, and player engagement.
 */
@Service
@Slf4j
public class AchievementAnalyticsService {

    @Autowired
    private AchievementAnalyticsRepository analyticsRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Calculates analytics for all achievements (scheduled task)
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    @CacheEvict(value = {"achievement-analytics", "achievement-summary"}, allEntries = true)
    public void calculateAllAchievementAnalytics() {
        log.info("Starting daily achievement analytics calculation...");
        
        List<Achievement> allAchievements = achievementRepository.findAll();
        int processed = 0;
        int errors = 0;

        for (Achievement achievement : allAchievements) {
            try {
                calculateAnalyticsForAchievement(achievement.getId());
                processed++;
            } catch (Exception e) {
                log.error("Error calculating analytics for achievement {}: {}", 
                         achievement.getId(), e.getMessage());
                errors++;
            }
        }

        log.info("Completed analytics calculation: {} processed, {} errors", processed, errors);
    }

    /**
     * Calculates analytics for a specific achievement
     */
    @Transactional
    public AchievementAnalytics calculateAnalyticsForAchievement(UUID achievementId) {
        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + achievementId));

        log.debug("Calculating analytics for achievement: {}", achievement.getName());

        // Get or create analytics record
        AchievementAnalytics analytics = analyticsRepository.findByAchievementId(achievementId)
                .orElse(AchievementAnalytics.builder()
                        .achievementId(achievementId)
                        .build());

        // Calculate completion metrics
        calculateCompletionMetrics(analytics, achievementId);

        // Calculate timing metrics
        calculateTimingMetrics(analytics, achievementId);

        // Calculate engagement metrics
        calculateEngagementMetrics(analytics, achievementId);

        // Calculate trend metrics
        calculateTrendMetrics(analytics, achievementId);

        // Set achievement reference for calculations
        analytics.setAchievement(achievement);

        return analyticsRepository.save(analytics);
    }

    /**
     * Calculates completion-related metrics
     */
    private void calculateCompletionMetrics(AchievementAnalytics analytics, UUID achievementId) {
        // Get all player achievements for this achievement
        List<PlayerAchievement> allPlayerAchievements = playerAchievementRepository
                .findByAchievementId(achievementId);

        // Total players who have attempted this achievement
        int totalAttempted = allPlayerAchievements.size();
        
        // Total completions
        long completions = allPlayerAchievements.stream()
                .filter(PlayerAchievement::getAchieved)
                .count();

        // If no one has attempted it, use total active players as potential attempts
        if (totalAttempted == 0) {
            totalAttempted = Math.max(1, (int) playerRepository.count());
        }

        analytics.setTotalCompletions((int) completions);
        analytics.setTotalPlayersAttempted(totalAttempted);

        // Calculate completion rate
        BigDecimal completionRate = BigDecimal.valueOf(completions * 100.0 / totalAttempted)
                .setScale(2, RoundingMode.HALF_UP);
        analytics.setCompletionRate(completionRate);

        log.debug("Completion metrics: {}/{} ({}%) for achievement {}", 
                 completions, totalAttempted, completionRate, achievementId);
    }

    /**
     * Calculates timing-related metrics
     */
    private void calculateTimingMetrics(AchievementAnalytics analytics, UUID achievementId) {
        List<PlayerAchievement> completedAchievements = playerAchievementRepository
                .findByAchievementId(achievementId)
                .stream()
                .filter(PlayerAchievement::getAchieved)
                .filter(pa -> pa.getDateEarned() != null)
                .collect(Collectors.toList());

        if (completedAchievements.isEmpty()) {
            return;
        }

        // For timing, we'll estimate based on when players first started playing
        // This is a simplified calculation - in reality you'd track when they first attempted
        List<Long> completionTimes = new ArrayList<>();
        LocalDateTime firstCompletion = null;
        LocalDateTime latestCompletion = null;

        for (PlayerAchievement pa : completedAchievements) {
            LocalDateTime earnedDate = pa.getDateEarned().toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();

            if (firstCompletion == null || earnedDate.isBefore(firstCompletion)) {
                firstCompletion = earnedDate;
            }
            if (latestCompletion == null || earnedDate.isAfter(latestCompletion)) {
                latestCompletion = earnedDate;
            }

            // Estimate completion time (simplified - assume they started playing 30 days before earning)
            long estimatedHours = 24 * 30; // Default 30 days
            completionTimes.add(estimatedHours);
        }

        if (!completionTimes.isEmpty()) {
            long avgTime = (long) completionTimes.stream().mapToLong(Long::longValue).average().orElse(0);
            long minTime = completionTimes.stream().mapToLong(Long::longValue).min().orElse(0);
            long maxTime = completionTimes.stream().mapToLong(Long::longValue).max().orElse(0);

            analytics.setAverageCompletionTimeHours((int) avgTime);
            analytics.setFastestCompletionTimeHours((int) minTime);
            analytics.setSlowestCompletionTimeHours((int) maxTime);
        }

        analytics.setFirstCompletionDate(firstCompletion);
        analytics.setLatestCompletionDate(latestCompletion);
    }

    /**
     * Calculates engagement-related metrics
     */
    private void calculateEngagementMetrics(AchievementAnalytics analytics, UUID achievementId) {
        List<PlayerAchievement> allPlayerAchievements = playerAchievementRepository
                .findByAchievementId(achievementId);

        // Players with any progress (not completed)
        long playersWithProgress = allPlayerAchievements.stream()
                .filter(pa -> !pa.getAchieved() && pa.getProgress() > 0)
                .count();

        // Active players pursuing (have made progress recently or have high progress)
        long activePursuing = allPlayerAchievements.stream()
                .filter(pa -> !pa.getAchieved() && pa.getProgress() > 50) // >50% progress
                .count();

        // Average progress percentage for incomplete achievements
        OptionalDouble avgProgress = allPlayerAchievements.stream()
                .filter(pa -> !pa.getAchieved())
                .mapToInt(PlayerAchievement::getProgress)
                .average();

        analytics.setPlayersWithProgress((int) playersWithProgress);
        analytics.setActivePlayersPursuing((int) activePursuing);
        
        if (avgProgress.isPresent()) {
            analytics.setAverageProgressPercentage(
                    BigDecimal.valueOf(avgProgress.getAsDouble()).setScale(2, RoundingMode.HALF_UP));
        }
    }

    /**
     * Calculates trend-related metrics
     */
    private void calculateTrendMetrics(AchievementAnalytics analytics, UUID achievementId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime monthAgo = now.minus(30, ChronoUnit.DAYS);

        // Count completions in time periods
        List<PlayerAchievement> recentCompletions = playerAchievementRepository
                .findByAchievementId(achievementId)
                .stream()
                .filter(PlayerAchievement::getAchieved)
                .filter(pa -> pa.getDateEarned() != null)
                .collect(Collectors.toList());

        long completionsLast7Days = recentCompletions.stream()
                .filter(pa -> {
                    LocalDateTime earnedDate = pa.getDateEarned().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                    return earnedDate.isAfter(weekAgo);
                })
                .count();

        long completionsLast30Days = recentCompletions.stream()
                .filter(pa -> {
                    LocalDateTime earnedDate = pa.getDateEarned().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                    return earnedDate.isAfter(monthAgo);
                })
                .count();

        analytics.setCompletionsLast7Days((int) completionsLast7Days);
        analytics.setCompletionsLast30Days((int) completionsLast30Days);
        analytics.updateCompletionTrend();
    }

    /**
     * Gets analytics summary for all achievements
     */
    @Cacheable(value = "achievement-summary", key = "'summary'")
    public Map<String, Object> getAnalyticsSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Basic statistics
        Object[] basicStats = analyticsRepository.getAnalyticsSummary(5); // Min 5 attempts
        if (basicStats != null && basicStats.length >= 4) {
            summary.put("totalAchievements", basicStats[0]);
            summary.put("averageCompletionRate", 
                       basicStats[1] != null ? ((Double) basicStats[1]).doubleValue() : 0.0);
            summary.put("averageDifficultyScore", 
                       basicStats[2] != null ? ((Double) basicStats[2]).doubleValue() : 0.0);
            summary.put("averageCompletionTimeHours", 
                       basicStats[3] != null ? ((Double) basicStats[3]).intValue() : 0);
        }

        // Completion rate distribution
        List<Object[]> distribution = analyticsRepository.getCompletionRateDistribution(5);
        Map<String, Integer> distributionMap = new HashMap<>();
        for (Object[] row : distribution) {
            distributionMap.put((String) row[0], ((Number) row[1]).intValue());
        }
        summary.put("completionRateDistribution", distributionMap);

        // Difficulty distribution
        Map<AchievementAnalytics.CalculatedDifficulty, Long> difficultyDistribution = 
                Arrays.stream(AchievementAnalytics.CalculatedDifficulty.values())
                      .collect(Collectors.toMap(
                              diff -> diff,
                              diff -> (long) analyticsRepository.findByCalculatedDifficulty(diff).size()
                      ));
        summary.put("difficultyDistribution", difficultyDistribution);

        // Recent trends
        LocalDateTime weekAgo = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        List<Object[]> trends = analyticsRepository.getCompletionTrends(weekAgo);
        Map<String, Integer> trendMap = new HashMap<>();
        for (Object[] row : trends) {
            trendMap.put((String) row[0], ((Number) row[1]).intValue());
        }
        summary.put("recentTrends", trendMap);

        // Problem achievements
        List<AchievementAnalytics> needingBalance = analyticsRepository.findAchievementsNeedingBalance(10);
        summary.put("achievementsNeedingBalance", needingBalance.size());

        return summary;
    }

    /**
     * Gets detailed analytics for a specific achievement
     */
    public Map<String, Object> getAchievementDetailedAnalytics(UUID achievementId) {
        AchievementAnalytics analytics = analyticsRepository.findByAchievementId(achievementId)
                .orElse(null);

        if (analytics == null) {
            // Calculate if doesn't exist
            analytics = calculateAnalyticsForAchievement(achievementId);
        }

        Map<String, Object> details = new HashMap<>();
        details.put("analytics", analytics);

        // Add recommendations
        List<String> recommendations = generateRecommendations(analytics);
        details.put("recommendations", recommendations);

        // Add comparison to similar achievements
        Map<String, Object> comparison = compareToSimilarAchievements(analytics);
        details.put("comparison", comparison);

        return details;
    }

    /**
     * Generates recommendations for achievement improvement
     */
    private List<String> generateRecommendations(AchievementAnalytics analytics) {
        List<String> recommendations = new ArrayList<>();

        if (analytics.isTooEasy()) {
            recommendations.add("Consider increasing difficulty - completion rate is very high");
            recommendations.add("Reduce point rewards or increase requirements");
        }

        if (analytics.isUnderperforming()) {
            recommendations.add("Achievement may be too difficult - very low completion rate");
            recommendations.add("Consider reducing requirements or adding progression steps");
            recommendations.add("Add hints or guidance for players");
        }

        if (analytics.getActivePlayersPursuing() < 5) {
            recommendations.add("Low player engagement - consider making more visible");
            recommendations.add("Add better description or preview of rewards");
        }

        if ("DECREASING".equals(analytics.getCompletionTrend())) {
            recommendations.add("Completion trend is decreasing - investigate recent changes");
            recommendations.add("Consider refreshing the achievement or adding bonus rewards");
        }

        if (analytics.getAverageCompletionTimeHours() != null && 
            analytics.getAverageCompletionTimeHours() > 720) { // >30 days
            recommendations.add("Very long completion time - consider breaking into smaller steps");
            recommendations.add("Add intermediate rewards or milestones");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Achievement is performing well - no immediate action needed");
        }

        return recommendations;
    }

    /**
     * Compares achievement to similar ones
     */
    private Map<String, Object> compareToSimilarAchievements(AchievementAnalytics analytics) {
        Map<String, Object> comparison = new HashMap<>();

        if (analytics.getAchievement() == null) {
            return comparison;
        }

        Achievement.AchievementCategory category = analytics.getAchievement().getCategory();
        
        // Find similar achievements by category
        List<AchievementAnalytics> similarAchievements = analyticsRepository.findAll()
                .stream()
                .filter(aa -> aa.getAchievement() != null && 
                             aa.getAchievement().getCategory() == category &&
                             !aa.getAchievementId().equals(analytics.getAchievementId()))
                .collect(Collectors.toList());

        if (!similarAchievements.isEmpty()) {
            double avgCompletionRate = similarAchievements.stream()
                    .filter(aa -> aa.getCompletionRate() != null)
                    .mapToDouble(aa -> aa.getCompletionRate().doubleValue())
                    .average().orElse(0.0);

            double avgDifficultyScore = similarAchievements.stream()
                    .filter(aa -> aa.getDifficultyScore() != null)
                    .mapToDouble(aa -> aa.getDifficultyScore().doubleValue())
                    .average().orElse(0.0);

            comparison.put("categoryAverageCompletionRate", avgCompletionRate);
            comparison.put("categoryAverageDifficultyScore", avgDifficultyScore);
            comparison.put("similarAchievementsCount", similarAchievements.size());

            // Performance ranking
            long betterThan = similarAchievements.stream()
                    .filter(aa -> aa.getCompletionRate() != null &&
                                 analytics.getCompletionRate() != null &&
                                 aa.getCompletionRate().compareTo(analytics.getCompletionRate()) < 0)
                    .count();

            comparison.put("performanceBetterThanCount", betterThan);
            comparison.put("performancePercentile", 
                          similarAchievements.size() > 0 ? 
                          (betterThan * 100.0 / similarAchievements.size()) : 0.0);
        }

        return comparison;
    }

    /**
     * Gets list of achievements needing attention
     */
    public List<AchievementAnalytics> getAchievementsNeedingAttention() {
        List<AchievementAnalytics> needingAttention = new ArrayList<>();

        // Too easy achievements
        needingAttention.addAll(analyticsRepository.findByCompletionRateGreaterThan(BigDecimal.valueOf(90)));

        // Too hard achievements
        needingAttention.addAll(analyticsRepository.findByCompletionRateLessThan(BigDecimal.valueOf(5)));

        // Decreasing trend achievements
        needingAttention.addAll(analyticsRepository.findByCompletionTrend("DECREASING"));

        return needingAttention.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Forces recalculation of stale analytics
     */
    @Transactional
    public void recalculateStaleAnalytics() {
        LocalDateTime cutoff = LocalDateTime.now().minus(24, ChronoUnit.HOURS);
        List<AchievementAnalytics> staleAnalytics = analyticsRepository.findStaleAnalytics(cutoff);

        log.info("Recalculating {} stale analytics", staleAnalytics.size());

        for (AchievementAnalytics analytics : staleAnalytics) {
            try {
                calculateAnalyticsForAchievement(analytics.getAchievementId());
            } catch (Exception e) {
                log.error("Error recalculating analytics for achievement {}: {}", 
                         analytics.getAchievementId(), e.getMessage());
            }
        }
    }
}