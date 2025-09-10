package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.EasterEggFind;
import com.example.javapingpongelo.models.EasterEggStats;
import com.example.javapingpongelo.models.dto.EggHunterLeaderboardDto;
import com.example.javapingpongelo.models.dto.RecentEggFindDto;
import com.example.javapingpongelo.repositories.EasterEggFindRepository;
import com.example.javapingpongelo.repositories.EasterEggStatsRepository;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.services.DTOMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service focused on easter egg statistics and leaderboards.
 * Handles player stats, achievements tracking, and leaderboard generation.
 */
@Service
@Slf4j
public class EasterEggStatsService {

    @Autowired
    private EasterEggStatsRepository easterEggStatsRepository;

    @Autowired
    private EasterEggFindRepository easterEggFindRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private DTOMappingService dtoMappingService;

    /**
     * Get or create player statistics
     */
    @Transactional(readOnly = true)
    public Optional<EasterEggStats> getPlayerStats(UUID playerId) {
        return easterEggStatsRepository.findByPlayerId(playerId);
    }

    /**
     * Update player statistics when an egg is found
     */
    @Transactional
    public EasterEggStats updatePlayerStats(UUID playerId, EasterEgg egg, int pointsAwarded) {
        EasterEggStats stats = easterEggStatsRepository.findByPlayerId(playerId)
            .orElse(EasterEggStats.builder()
                .playerId(playerId)
                .build());

        // Record the egg find
        stats.recordEggFound(egg.getType(), pointsAwarded, egg.getPageLocation());
        
        // Save and return updated stats
        EasterEggStats savedStats = easterEggStatsRepository.save(stats);
        
        log.debug("Updated stats for player {}: {} eggs found, {} total points", 
            playerId, savedStats.getTotalEggsFound(), savedStats.getTotalPointsEarned());
            
        return savedStats;
    }

    /**
     * Get the secret leaderboard of top egg hunters
     */
    @Transactional(readOnly = true)
    public List<EggHunterLeaderboardDto> getSecretLeaderboard(int limit) {
        List<EasterEggStats> topHunters = easterEggStatsRepository
            .findTop50ByOrderByTotalEggsFoundDescTotalPointsEarnedDesc(PageRequest.of(0, limit));

        return topHunters.stream()
            .map(dtoMappingService::toLeaderboardDto)
            .collect(Collectors.toList());
    }

    /**
     * Get recent egg finds for activity display
     */
    @Transactional(readOnly = true)
    public List<RecentEggFindDto> getRecentFinds(int limit) {
        List<EasterEggFind> recentFinds = easterEggFindRepository
            .findRecentFinds(PageRequest.of(0, limit));

        return recentFinds.stream()
            .map(dtoMappingService::toRecentFindDto)
            .collect(Collectors.toList());
    }

    /**
     * Calculate achievement progress for a player
     */
    @Transactional(readOnly = true)
    public Map<String, Object> calculateAchievementProgress(UUID playerId) {
        Optional<EasterEggStats> statsOpt = getPlayerStats(playerId);
        if (statsOpt.isEmpty()) {
            return Collections.emptyMap();
        }

        EasterEggStats stats = statsOpt.get();
        Map<String, Object> progress = new HashMap<>();

        // Basic counts
        progress.put("totalEggsFound", stats.getTotalEggsFound());
        progress.put("totalPointsEarned", stats.getTotalPointsEarned());
        progress.put("currentStreak", stats.getCurrentStreak());
        progress.put("longestStreak", stats.getLongestStreak());

        // Rarity counts
        progress.put("commonEggsFound", stats.getCommonEggsFound());
        progress.put("uncommonEggsFound", stats.getUncommonEggsFound() != null ? stats.getUncommonEggsFound() : 0);
        progress.put("rareEggsFound", stats.getRareEggsFound() != null ? stats.getRareEggsFound() : 0);
        progress.put("epicEggsFound", stats.getEpicEggsFound() != null ? stats.getEpicEggsFound() : 0);
        progress.put("legendaryEggsFound", stats.getLegendaryEggsFound() != null ? stats.getLegendaryEggsFound() : 0);
        progress.put("mythicalEggsFound", stats.getMythicalEggsFound() != null ? stats.getMythicalEggsFound() : 0);

        return progress;
    }

    /**
     * Get top performers for each rarity type
     */
    @Transactional(readOnly = true)
    public Map<String, List<EggHunterLeaderboardDto>> getRarityLeaderboards() {
        Map<String, List<EggHunterLeaderboardDto>> leaderboards = new HashMap<>();

        // Get top players for each rarity (limit to top 5 for each)
        leaderboards.put("rare", getTopPlayersForRarity("rare", 5));
        leaderboards.put("epic", getTopPlayersForRarity("epic", 5));
        leaderboards.put("legendary", getTopPlayersForRarity("legendary", 5));
        leaderboards.put("mythical", getTopPlayersForRarity("mythical", 5));

        return leaderboards;
    }

    /**
     * Get statistics summary for admin dashboard
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long totalPlayers = easterEggStatsRepository.count();
        long totalEggs = easterEggFindRepository.count();
        
        stats.put("totalActivePlayers", totalPlayers);
        stats.put("totalEggsFound", totalEggs);
        
        if (totalPlayers > 0) {
            stats.put("averageEggsPerPlayer", (double) totalEggs / totalPlayers);
        }
        
        // Get counts by rarity
        stats.put("eggsByRarity", getEggCountsByRarity());
        
        return stats;
    }

    // Note: Conversion methods moved to DTOMappingService for centralization and reuse

    /**
     * Get top players for a specific rarity type
     */
    private List<EggHunterLeaderboardDto> getTopPlayersForRarity(String rarity, int limit) {
        try {
            PageRequest pageRequest = PageRequest.of(0, limit);
            List<EasterEggStats> topPlayers;
            
            switch (rarity.toLowerCase()) {
                case "rare":
                    topPlayers = easterEggStatsRepository.findTopRareEggFinders(pageRequest);
                    break;
                case "epic":
                    topPlayers = easterEggStatsRepository.findTopEpicEggFinders(pageRequest);
                    break;
                case "legendary":
                    topPlayers = easterEggStatsRepository.findTopLegendaryEggFinders(pageRequest);
                    break;
                case "mythical":
                    topPlayers = easterEggStatsRepository.findTopMythicalEggFinders(pageRequest);
                    break;
                default:
                    log.warn("Unknown rarity type requested: {}", rarity);
                    return Collections.emptyList();
            }
            
            List<EggHunterLeaderboardDto> result = new ArrayList<>();
            int rank = 1;
            
            for (EasterEggStats stats : topPlayers) {
                EggHunterLeaderboardDto dto = dtoMappingService.toLeaderboardDto(stats);
                dto.setRank(rank++);
                result.add(dto);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Error getting top players for rarity: {}", rarity, e);
            return Collections.emptyList();
        }
    }

    /**
     * Get egg counts grouped by rarity type (optimized single-pass calculation)
     */
    private Map<String, Long> getEggCountsByRarity() {
        Map<String, Long> counts = new HashMap<>();
        
        // Get all stats in one query and calculate totals in a single pass
        List<EasterEggStats> allStats = easterEggStatsRepository.findAll();
        
        // Use single stream with collectors for better performance
        long common = 0, uncommon = 0, rare = 0, epic = 0, legendary = 0, mythical = 0;
        
        for (EasterEggStats stats : allStats) {
            common += stats.getCommonEggsFound();
            uncommon += (stats.getUncommonEggsFound() != null ? stats.getUncommonEggsFound() : 0);
            rare += (stats.getRareEggsFound() != null ? stats.getRareEggsFound() : 0);
            epic += (stats.getEpicEggsFound() != null ? stats.getEpicEggsFound() : 0);
            legendary += (stats.getLegendaryEggsFound() != null ? stats.getLegendaryEggsFound() : 0);
            mythical += (stats.getMythicalEggsFound() != null ? stats.getMythicalEggsFound() : 0);
        }
        
        counts.put("COMMON", common);
        counts.put("UNCOMMON", uncommon);
        counts.put("RARE", rare);
        counts.put("EPIC", epic);
        counts.put("LEGENDARY", legendary);
        counts.put("MYTHICAL", mythical);
        
        return counts;
    }
}