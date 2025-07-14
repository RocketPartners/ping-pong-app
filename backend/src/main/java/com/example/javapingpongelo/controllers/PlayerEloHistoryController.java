package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.GameType;
import com.example.javapingpongelo.models.PlayerEloHistory;
import com.example.javapingpongelo.models.dto.PlayerEloHistoryDTO;
import com.example.javapingpongelo.models.dto.PlayerRankHistoryDTO;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.services.PlayerEloHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Controller for retrieving player ELO and rank history
 */
@RestController
@RequestMapping("/api/players")
@Slf4j
public class PlayerEloHistoryController {

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private PlayerEloHistoryService eloHistoryService;

    /**
     * Get a player's complete ELO history
     */
    @GetMapping("/{id}/elo-history")
    public ResponseEntity<List<PlayerEloHistory>> getPlayerEloHistory(@PathVariable("id") UUID id) {
        log.debug("Request to get ELO history for player: {}", id);

        // Validate player exists
        if (playerService.findPlayerById(id) == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Get the player's ELO history
        List<PlayerEloHistory> eloHistory = eloHistoryService.getPlayerEloHistory(id);

        return ResponseEntity.ok(eloHistory);
    }

    /**
     * Get a player's ELO history for a specific game type
     */
    @GetMapping("/{id}/elo-history/{gameType}")
    public ResponseEntity<List<PlayerEloHistory>> getPlayerEloHistoryByGameType(
            @PathVariable("id") UUID id,
            @PathVariable("gameType") String gameTypeStr) {

        log.debug("Request to get ELO history for player: {} and game type: {}", id, gameTypeStr);

        // Validate player exists
        if (playerService.findPlayerById(id) == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Convert string to enum (throws IllegalArgumentException if invalid)
        GameType gameType = GameType.valueOf(gameTypeStr.toUpperCase());

        // Get the player's ELO history for the specified game type
        List<PlayerEloHistory> eloHistory = eloHistoryService.getPlayerEloHistoryByGameType(id, gameType);

        return ResponseEntity.ok(eloHistory);
    }

    /**
     * Get a player's ELO history for a specific game type within a date range
     */
    @GetMapping("/{id}/elo-history/{gameType}/range")
    public ResponseEntity<List<PlayerEloHistory>> getPlayerEloHistoryByGameTypeAndDateRange(
            @PathVariable("id") UUID id,
            @PathVariable("gameType") String gameTypeStr,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {

        log.debug("Request to get ELO history for player: {} and game type: {} between {} and {}",
                  id, gameTypeStr, startDate, endDate);

        // Validate player exists
        if (playerService.findPlayerById(id) == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Convert string to enum (throws IllegalArgumentException if invalid)
        GameType gameType = GameType.valueOf(gameTypeStr.toUpperCase());

        // Get the player's ELO history for the specified game type and date range
        List<PlayerEloHistory> eloHistory = eloHistoryService.getPlayerEloHistoryByGameTypeAndDateRange(
                id, gameType, startDate, endDate);

        return ResponseEntity.ok(eloHistory);
    }

    /**
     * Get a player's ELO history optimized for charting
     */
    @GetMapping("/{id}/elo-chart/{gameType}")
    public ResponseEntity<List<PlayerEloHistoryDTO>> getPlayerEloHistoryForChart(
            @PathVariable("id") UUID id,
            @PathVariable("gameType") String gameTypeStr) {

        log.debug("Request to get ELO chart data for player: {} and game type: {}", id, gameTypeStr);

        // Validate player exists
        if (playerService.findPlayerById(id) == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Convert string to enum (throws IllegalArgumentException if invalid)
        GameType gameType = GameType.valueOf(gameTypeStr.toUpperCase());

        // Get the player's ELO history for charting
        List<PlayerEloHistoryDTO> chartData = eloHistoryService.getPlayerEloHistoryForChart(id, gameType);

        return ResponseEntity.ok(chartData);
    }

    /**
     * Get a player's rank history for a specific game type
     */
    @GetMapping("/{id}/rank-history/{gameType}")
    public ResponseEntity<List<PlayerRankHistoryDTO>> getPlayerRankHistory(
            @PathVariable("id") UUID id,
            @PathVariable("gameType") String gameTypeStr) {

        log.debug("Request to get rank history for player: {} and game type: {}", id, gameTypeStr);

        // Validate player exists
        if (playerService.findPlayerById(id) == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Convert string to enum (throws IllegalArgumentException if invalid)
        GameType gameType = GameType.valueOf(gameTypeStr.toUpperCase());

        // Get the player's rank history
        List<PlayerRankHistoryDTO> rankHistory = eloHistoryService.getPlayerRankHistory(id, gameType);

        return ResponseEntity.ok(rankHistory);
    }
}