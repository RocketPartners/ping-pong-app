package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.PlayerAchievement;
import com.example.javapingpongelo.models.dto.AchievementDTO;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.achievements.IAchievementService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for achievement-related operations
 */
@RestController
@RequestMapping("/api/achievements")
@Slf4j
public class AchievementController {

    @Autowired
    IAchievementService achievementService;

    /**
     * Get all visible achievements
     */
    @GetMapping
    public ResponseEntity<List<Achievement>> getAllVisibleAchievements() {
        log.debug("Request to get all visible achievements");
        List<Achievement> achievements = achievementService.findVisibleAchievements();
        return ResponseEntity.ok(achievements);
    }

    /**
     * Get all achievements (including hidden ones)
     * Restricted to admin users
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Achievement>> getAllAchievements() {
        log.debug("Admin request to get all achievements including hidden ones");
        List<Achievement> achievements = achievementService.findAllAchievements();
        return ResponseEntity.ok(achievements);
    }

    /**
     * Get an achievement by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Achievement> getAchievementById(@PathVariable("id") UUID id) {
        log.debug("Request to get achievement: {}", id);
        Achievement achievement = achievementService.findAchievementById(id);
        if (achievement == null) {
            throw new ResourceNotFoundException("Achievement not found with id: " + id);
        }
        return ResponseEntity.ok(achievement);
    }

    /**
     * Create a new achievement
     * Restricted to admin users
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAchievement(@Valid @RequestBody Achievement achievement,
                                               BindingResult bindingResult) {
        log.debug("Request to create achievement: {}", achievement.getName());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in achievement creation: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        Achievement createdAchievement = achievementService.createAchievement(achievement);
        log.info("Achievement created successfully: {}", createdAchievement.getId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdAchievement);
    }

    /**
     * Get a player's achievements
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<PlayerAchievement>> getPlayerAchievements(@PathVariable("playerId") UUID playerId) {
        log.debug("Request to get achievements for player: {}", playerId);
        List<PlayerAchievement> achievements = achievementService.findPlayerAchievements(playerId);
        return ResponseEntity.ok(achievements);
    }

    /**
     * Get the current user's achievements
     */
    @GetMapping("/myAchievements")
    public ResponseEntity<List<PlayerAchievement>> getCurrentUserAchievements(Principal principal) {
        if (principal == null) {
            throw new BadRequestException("User not authenticated");
        }

        log.debug("Request to get achievements for current user: {}", principal.getName());

        // Get player by username
        UUID playerId = getPlayerIdFromUsername(principal.getName());
        List<PlayerAchievement> achievements = achievementService.findPlayerAchievements(playerId);

        return ResponseEntity.ok(achievements);
    }

    /**
     * Helper method to get player ID from username
     */
    private UUID getPlayerIdFromUsername(String username) {
        try {
            return UUID.fromString(username);
        }
        catch (Exception e) {
            throw new ResourceNotFoundException("Player not found with username: " + username);
        }
    }

    /**
     * Get a player's achieved achievements
     */
    @GetMapping("/player/{playerId}/achieved")
    public ResponseEntity<List<AchievementDTO>> getPlayerAchievedAchievements(@PathVariable("playerId") UUID playerId) {
        log.debug("Request to get achieved achievements for player: {}", playerId);
        List<PlayerAchievement> achievements = achievementService.findPlayerAchievedAchievements(playerId);
        List<AchievementDTO> achievementDTOList = new ArrayList<>();
        for (PlayerAchievement achievement : achievements) {
            AchievementDTO achievementDTO = AchievementDTO.fromEntities(achievementService.findAchievementById(achievement.getAchievementId()), achievement);
            achievementDTOList.add(achievementDTO);
        }
        return ResponseEntity.ok(achievementDTOList);
    }

    /**
     * Recalculate all achievements for a player
     * Restricted to admin users
     */
    @PostMapping("/player/{playerId}/recalculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> recalculatePlayerAchievements(@PathVariable("playerId") UUID playerId) {
        log.info("Admin request to recalculate achievements for player: {}", playerId);

        achievementService.recalculatePlayerAchievements(playerId);
        return ResponseEntity.ok(new ApiResponse(true, "Achievements recalculated successfully"));
    }
}