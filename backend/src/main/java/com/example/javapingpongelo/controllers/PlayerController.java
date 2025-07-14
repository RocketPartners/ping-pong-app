package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.models.dto.PlayerDTO;
import com.example.javapingpongelo.models.dto.PlayerStyleAverageDTO;
import com.example.javapingpongelo.models.dto.PlayerStyleReviewDTO;
import com.example.javapingpongelo.models.dto.PlayerStyleTopDTO;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.GameHistoryService;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.services.PlayerStyleReviewService;
import com.example.javapingpongelo.services.achievements.IAchievementService;
import com.example.javapingpongelo.validators.ValidationGroups;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/players")
@Slf4j
public class PlayerController {

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private IAchievementService achievementService;

    @Autowired
    private GameHistoryService gameHistoryService;

    @Autowired
    private PlayerStyleReviewService playerStyleReviewService;

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayerById(@PathVariable("id") UUID id) {
        log.debug("Request to get player by ID: {}", id);
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }
        return ResponseEntity.ok(player);
    }

    @GetMapping("/name/{firstName}/{lastName}")
    public ResponseEntity<Player> getPlayerByName(
            @PathVariable("firstName") String firstName,
            @PathVariable("lastName") String lastName) {

        log.debug("Request to get player by name: {} {}", firstName, lastName);
        Player player = playerService.findPlayerByName(firstName, lastName);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with name: " + firstName + " " + lastName);
        }
        return ResponseEntity.ok(player);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Player> getPlayerByUsername(@PathVariable("username") String username) {
        log.debug("Request to get player by username: {}", username);
        Player player = playerService.findPlayerByUsername(username);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with username: " + username);
        }
        return ResponseEntity.ok(player);
    }

    @GetMapping
    public ResponseEntity<List<PlayerDTO>> getAllPlayers() {
        log.debug("Request to get all players");
        List<Player> players = playerService.findAllPlayers();
        List<PlayerDTO> playerDTOS = new ArrayList<>();
        for (Player player : players) {
            playerDTOS.add(PlayerDTO.fromEntity(player, gameHistoryService.getGameHistory(player.getPlayerId())));
        }
        return ResponseEntity.ok(playerDTOS);
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<PlayerDTO>> getPlayersByIds(@Valid @RequestBody List<UUID> ids) {
        List<PlayerDTO> playerDTOS = new ArrayList<>();
        log.debug("Request to get players by ids");
        for (UUID id : ids) {
            Player player = playerService.findPlayerById(id);
            if (player == null) {
                throw new ResourceNotFoundException("Player not found with id: " + id);
            }
            playerDTOS.add(PlayerDTO.fromEntity(player, gameHistoryService.getGameHistory(player.getPlayerId())));
        }
        return ResponseEntity.ok(playerDTOS);
    }

    @GetMapping("/usernames")
    public ResponseEntity<List<String>> getPlayerUsernames() {
        log.debug("Request to get all player usernames");
        List<String> usernames = playerService.findAllPlayerUsernames();
        return ResponseEntity.ok(usernames);
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@Validated(ValidationGroups.Create.class) @RequestBody Player player) {
        log.debug("Request to create new player: {}", player.getUsername());
        Player newPlayer = playerService.createPlayer(player);
        return ResponseEntity.status(HttpStatus.CREATED).body(newPlayer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(
            @PathVariable("id") UUID id,
            @Validated(ValidationGroups.Update.class) @RequestBody Player player) {

        log.debug("Request to update player: {}", id);
        // Ensure the player exists
        Player existingPlayer = playerService.findPlayerById(id);
        if (existingPlayer == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Ensure IDs match
        if (!id.equals(player.getPlayerId())) {
            throw new BadRequestException("Player ID in path does not match body");
        }

        Player updatedPlayer = playerService.updatePlayer(player);
        return ResponseEntity.ok(updatedPlayer);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticatePlayer(@Valid @RequestBody AuthRequest request) {
        log.debug("Authentication request for username: {}", request.getUsername());

        Player player = playerService.authenticatePlayer(request.getUsername(), request.getPassword());
        if (player == null) {
            throw new BadRequestException("Invalid username or password");
        }

        // Generate JWT token (this should be replaced with actual JWT token generation)
        String token = "jwt-token-" + System.currentTimeMillis();
        return ResponseEntity.ok(new AuthResponse(player, token));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable("id") UUID id) {
        log.debug("Request to delete player: {}", id);

        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all achievements for a player
     */
    @GetMapping("/{id}/achievements")
    public ResponseEntity<List<Map<String, Object>>> getPlayerAchievements(@PathVariable("id") UUID id) {
        log.debug("Request to get achievements for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Get player achievements
        List<PlayerAchievement> achievements = achievementService.findPlayerAchievements(id);

        // Fetch detailed achievement info for each
        List<Map<String, Object>> detailedAchievements = new ArrayList<>();
        for (PlayerAchievement pa : achievements) {
            Map<String, Object> detail = new HashMap<>();

            // Get the achievement details
            Achievement achievement = achievementService.findAchievementById(pa.getAchievementId());

            // Combine achievement and player achievement data
            detail.put("id", achievement.getId());
            detail.put("name", achievement.getName());
            detail.put("description", achievement.getDescription());
            detail.put("category", achievement.getCategory());
            detail.put("type", achievement.getType());
            detail.put("icon", achievement.getIcon());
            detail.put("points", achievement.getPoints());
            detail.put("achieved", pa.getAchieved());
            detail.put("progress", pa.getProgress());
            detail.put("dateEarned", pa.getDateEarned());

            // Calculate threshold from criteria
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode criteria = objectMapper.readTree(achievement.getCriteria());
                int threshold = criteria.has("threshold") ? criteria.get("threshold").asInt() : 1;
                detail.put("threshold", threshold);
            }
            catch (Exception e) {
                detail.put("threshold", 1);
                log.warn("Error parsing criteria for achievement {}", achievement.getId());
            }

            detailedAchievements.add(detail);
        }

        return ResponseEntity.ok(detailedAchievements);
    }

    /**
     * Get earned achievements for a player
     */
    @GetMapping("/{id}/achievements/earned")
    public ResponseEntity<List<PlayerAchievement>> getPlayerEarnedAchievements(@PathVariable("id") UUID id) {
        log.debug("Request to get earned achievements for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Get player achievements
        List<PlayerAchievement> achievements = achievementService.findPlayerAchievedAchievements(id);

        return ResponseEntity.ok(achievements);
    }

    /**
     * Recalculate all achievements for a player (admin only)
     */
    @PostMapping("/{id}/achievements/recalculate")
    public ResponseEntity<ApiResponse> recalculatePlayerAchievements(
            @PathVariable("id") UUID id,
            Principal principal) {

        log.info("Request to recalculate achievements for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Admin only - implement proper authorization here
        // This is a placeholder check
        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Authentication required"));
        }

        // Recalculate achievements
        achievementService.recalculatePlayerAchievements(id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse(true, "Achievements recalculated successfully"));
    }

    /**
     * Get a player's style ratings
     */
    @GetMapping("/{id}/style-ratings")
    public ResponseEntity<Map<PlayerStyle, Integer>> getPlayerStyleRatings(@PathVariable("id") UUID id) {
        log.debug("Request to get style ratings for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Get player style ratings
        Map<PlayerStyle, Integer> styleRatings = new HashMap<>();
        for (PlayerStyle style : PlayerStyle.values()) {
            styleRatings.put(style, player.getStyleRating(style));
        }

        return ResponseEntity.ok(styleRatings);
    }

    /**
     * Update a player's style ratings
     */
    @PutMapping("/{id}/style-ratings")
    public ResponseEntity<Player> updatePlayerStyleRatings(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Integer> styleRatings) {

        log.debug("Request to update style ratings for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Convert string style names to PlayerStyle enum and update ratings
        Map<PlayerStyle, Integer> ratingMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : styleRatings.entrySet()) {
            try {
                PlayerStyle style = PlayerStyle.valueOf(entry.getKey().toUpperCase());
                ratingMap.put(style, entry.getValue());
            }
            catch (IllegalArgumentException e) {
                log.warn("Invalid style name: {}", entry.getKey());
                // Skip invalid style names
            }
        }

        // Update player style ratings
        Player updatedPlayer = playerService.updatePlayerStyleRatings(id, ratingMap);

        return ResponseEntity.ok(updatedPlayer);
    }

    /**
     * Submit a style review for a player after multiple games
     */
    @PostMapping("/{id}/style-review")
    public ResponseEntity<PlayerStyleReview> submitPlayerStyleReview(
            @PathVariable("id") UUID id,
            @Valid @RequestBody PlayerStyleReviewDTO reviewDTO) {

        log.debug("Request to submit style review for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Ensure the player ID in the path matches the one in the body
        if (!id.equals(reviewDTO.getPlayerId())) {
            throw new BadRequestException("Player ID in path does not match body");
        }

        // Validate reviewer exists (and get their details)
        Player reviewer = playerService.findPlayerById(reviewDTO.getReviewerId());
        if (reviewer == null) {
            throw new ResourceNotFoundException("Reviewer not found with id: " + reviewDTO.getReviewerId());
        }

        // Populate reviewer details
        reviewDTO.setReviewerUsername(reviewer.getUsername());
        reviewDTO.setReviewerFirstName(reviewer.getFirstName());
        reviewDTO.setReviewerLastName(reviewer.getLastName());

        // Create the review
        PlayerStyleReview review = playerStyleReviewService.createReview(reviewDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * Get all reviews for a player
     */
    @GetMapping("/{id}/style-reviews")
    public ResponseEntity<List<PlayerStyleReview>> getPlayerStyleReviews(@PathVariable("id") UUID id) {
        log.debug("Request to get style reviews for player: {}", id);

        // Validate player exists
        Player player = playerService.findPlayerById(id);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + id);
        }

        // Get player reviews
        List<PlayerStyleReview> reviews = playerStyleReviewService.findReviewsByPlayerId(id);

        return ResponseEntity.ok(reviews);
    }

    /**
     * Submit a response review for a player's review
     * If strengths and improvements are both empty, the review is acknowledged without affecting ratings
     */
    @PostMapping("/reviews/{reviewId}/respond")
    public ResponseEntity<PlayerStyleReview> respondToReview(
            @PathVariable("reviewId") UUID reviewId,
            @Valid @RequestBody PlayerStyleReviewDTO responseReviewDTO) {

        log.debug("Request to respond to review: {}", reviewId);

        // Validate the original review exists
        PlayerStyleReview originalReview = playerStyleReviewService.findById(reviewId);
        if (originalReview == null) {
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }

        // Set response properties
        responseReviewDTO.setResponse(true);
        responseReviewDTO.setParentReviewId(reviewId);

        // The player being reviewed is the original reviewer
        responseReviewDTO.setPlayerId(originalReview.getReviewerId());

        // Validate reviewer exists (and get their details)
        Player reviewer = playerService.findPlayerById(responseReviewDTO.getReviewerId());
        if (reviewer == null) {
            throw new ResourceNotFoundException("Reviewer not found with id: " + responseReviewDTO.getReviewerId());
        }

        // Populate reviewer details
        responseReviewDTO.setReviewerUsername(reviewer.getUsername());
        responseReviewDTO.setReviewerFirstName(reviewer.getFirstName());
        responseReviewDTO.setReviewerLastName(reviewer.getLastName());

        // Mark as a dismiss operation if both strengths and improvements are empty
        boolean isDismiss = (responseReviewDTO.getStrengths() == null || responseReviewDTO.getStrengths().isEmpty()) &&
                (responseReviewDTO.getImprovements() == null || responseReviewDTO.getImprovements().isEmpty());

        if (isDismiss) {
            log.debug("Review {} is being dismissed (no strengths/improvements provided)", reviewId);
        }

        // Create the response review
        PlayerStyleReview review = playerStyleReviewService.createReview(responseReviewDTO, isDismiss);

        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    /**
     * Return unacknowledged player reviews for the last 7 days
     */
    @GetMapping("/{playerId}/recent-reviews")
    public ResponseEntity<List<PlayerStyleReview>> getRecentPlayerReviews(
            @PathVariable("playerId") UUID playerId) {

        log.debug("Request to get recent reviews for player: {}", playerId);

        // Validate player exists
        Player player = playerService.findPlayerById(playerId);
        if (player == null) {
            throw new ResourceNotFoundException("Player not found with id: " + playerId);
        }

        List<PlayerStyleReview> reviews = playerStyleReviewService.findRecentUnacknowledgedReviewsForPlayer(playerId, 7);

        return ResponseEntity.ok(reviews);
    }

    /**
     * Get average style ratings across all players
     */
    @GetMapping("/style-ratings/average")
    public ResponseEntity<List<PlayerStyleAverageDTO>> getAverageStyleRatings() {
        log.debug("Request to get average style ratings across all players");
        List<PlayerStyleAverageDTO> averages = playerService.getAverageStyleRatings();
        return ResponseEntity.ok(averages);
    }

    /**
     * Get the highest style ratings among all players
     */
    @GetMapping("/style-ratings/highest")
    public ResponseEntity<List<PlayerStyleTopDTO>> getHighestStyleRatings() {
        log.debug("Request to get highest style ratings among all players");
        List<PlayerStyleTopDTO> highest = playerService.getHighestStyleRatings();
        return ResponseEntity.ok(highest);
    }

}