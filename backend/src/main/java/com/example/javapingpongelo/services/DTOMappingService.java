package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.models.dto.*;
import com.example.javapingpongelo.repositories.PlayerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Centralized service for converting between entities and DTOs.
 * Provides consistent null-safe conversions and eliminates code duplication.
 */
@Service
@Slf4j
public class DTOMappingService {

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Convert EasterEggStats to EggHunterLeaderboardDto
     */
    public EggHunterLeaderboardDto toLeaderboardDto(EasterEggStats stats) {
        if (stats == null) {
            return null;
        }

        // Get player details efficiently with null safety
        PlayerInfo playerInfo = getPlayerInfo(stats.getPlayerId());

        return EggHunterLeaderboardDto.builder()
            .playerName(playerInfo.fullName)
            .username(playerInfo.username)
            .totalEggsFound(stats.getTotalEggsFound())
            .totalPoints(stats.getTotalPointsEarned())
            .rank(0) // Will be set by caller based on position in list
            .commonEggsFound(stats.getCommonEggsFound())
            .uncommonEggsFound(safeInteger(stats.getUncommonEggsFound()))
            .rareEggsFound(safeInteger(stats.getRareEggsFound()))
            .epicEggsFound(safeInteger(stats.getEpicEggsFound()))
            .legendaryEggsFound(safeInteger(stats.getLegendaryEggsFound()))
            .mythicalEggsFound(safeInteger(stats.getMythicalEggsFound()))
            .longestStreak(stats.getLongestStreak())
            .favoriteHuntingPage(stats.getFavoriteHuntingPage())
            .firstEggFound(stats.getFirstEggFound())
            .lastEggFound(stats.getLastEggFound())
            .build();
    }

    /**
     * Convert EasterEggFind to RecentEggFindDto
     */
    public RecentEggFindDto toRecentFindDto(EasterEggFind find) {
        if (find == null) {
            return null;
        }

        PlayerInfo playerInfo = getPlayerInfo(find.getPlayerId());

        RecentEggFindDto dto = RecentEggFindDto.builder()
            .finderName(playerInfo.fullName)
            .finderUsername(playerInfo.username)
            .eggType(find.getEggType())
            .pointsEarned(find.getPointsAwarded())
            .foundAt(find.getFoundAt())
            .pageFoundOn(find.getPageFoundOn())
            .build();

        // Calculate relative time if needed
        if (dto instanceof RecentEggFindDto) {
            try {
                ((RecentEggFindDto) dto).calculateRelativeTime();
            } catch (Exception e) {
                log.debug("Could not calculate relative time for find: {}", e.getMessage());
            }
        }

        return dto;
    }

    /**
     * Convert Player to a simple player info DTO for leaderboards
     */
    public PlayerInfo toPlayerInfo(Player player) {
        if (player == null) {
            return new PlayerInfo("Unknown Player", "unknown");
        }

        return new PlayerInfo(
            safeString(player.getFullName(), "Unknown Player"),
            safeString(player.getUsername(), "unknown")
        );
    }

    // Helper methods for null-safe conversions

    /**
     * Get player information with caching and null safety
     */
    private PlayerInfo getPlayerInfo(UUID playerId) {
        if (playerId == null) {
            return new PlayerInfo("Unknown Player", "unknown");
        }

        try {
            Optional<Player> playerOpt = playerRepository.findById(playerId);
            if (playerOpt.isPresent()) {
                Player player = playerOpt.get();
                return new PlayerInfo(
                    safeString(player.getFullName(), "Unknown Player"),
                    safeString(player.getUsername(), "unknown")
                );
            }
        } catch (Exception e) {
            log.warn("Error fetching player {}: {}", playerId, e.getMessage());
        }

        return new PlayerInfo("Unknown Player", "unknown");
    }

    /**
     * Null-safe integer conversion
     */
    private int safeInteger(Integer value) {
        return value != null ? value : 0;
    }

    /**
     * Null-safe string conversion with default
     */
    private String safeString(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Simple data class for player information
     */
    public static class PlayerInfo {
        public final String fullName;
        public final String username;

        public PlayerInfo(String fullName, String username) {
            this.fullName = fullName;
            this.username = username;
        }
    }
}