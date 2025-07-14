package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.Game;
import com.example.javapingpongelo.models.dto.PagedResponse;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.GameService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller for game-related operations.
 */
@RestController
@RequestMapping("/api/games")
@Slf4j
public class GameController {

    @Autowired
    private GameService gameService;

    /**
     * Save one or more games
     *
     * @param games         List of games to save
     * @param bindingResult Validation results
     * @return ResponseEntity with saved games
     */
    @PostMapping
    public ResponseEntity<List<Game>> saveGames(@Valid @RequestBody List<Game> games, BindingResult bindingResult) {
        log.info("Saving {} games", games.size());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in game submission: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        List<Game> savedGames = gameService.saveAndReturn(games);
        log.info("Successfully saved {} games", savedGames.size());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedGames);
    }

    /**
     * Get a game by its ID
     *
     * @param id UUID of the game
     * @return The game if found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Game> getGameById(@PathVariable("id") UUID id) {
        log.debug("Request to get game: {}", id);

        Game game = gameService.findById(id);
        if (game == null) {
            log.warn("Game not found with ID: {}", id);
            throw new ResourceNotFoundException("Game not found with id: " + id);
        }

        return ResponseEntity.ok(game);
    }

    /**
     * Get all games with optional pagination and sorting
     *
     * @param limit Optional limit parameter (default: all games)
     * @param page  Optional page number (default: 0)
     * @return List of games, sorted by date played (most recent first)
     */
    @GetMapping
    public ResponseEntity<Object> getAllGames(
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "0") Integer page) {
        log.debug("Request to get games with limit: {}, page: {}", limit, page);

        // If pagination is requested
        if (limit != null && limit > 0) {
            // Create pageable with sorting by date played in descending order
            Pageable pageable = PageRequest.of(
                    page,
                    limit,
                    Sort.by(Sort.Direction.DESC, "datePlayed")
            );

            Page<Game> gamesPage = gameService.findAllPaged(pageable);

            // Convert Page to our custom DTO
            PagedResponse<Game> response = PagedResponse.from(gamesPage);

            return ResponseEntity.ok(response);
        }
        else {
            // No pagination, return all games sorted by date
            List<Game> games = gameService.findAllSorted(Sort.by(Sort.Direction.DESC, "datePlayed"));
            return ResponseEntity.ok(games);
        }
    }

    /**
     * Get all games for a specific player with optional pagination
     *
     * @param playerId ID of the player
     * @param limit    Optional limit parameter (default: all games)
     * @param page     Optional page number (default: 0)
     * @return List of games played by the player
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<Object> getGamesByPlayerId(
            @PathVariable("playerId") UUID playerId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(defaultValue = "0") Integer page) {
        log.debug("Request to get games by player ID: {} with limit: {}, page: {}", playerId, limit, page);

        // If pagination is requested
        if (limit != null && limit > 0) {
            // Create pageable with sorting by date played in descending order
            Pageable pageable = PageRequest.of(
                    page,
                    limit,
                    Sort.by(Sort.Direction.DESC, "datePlayed")
            );

            Page<Game> gamesPage = gameService.findByPlayerIdPaged(playerId, pageable);

            // Convert Page to our custom DTO
            PagedResponse<Game> response = PagedResponse.from(gamesPage);

            return ResponseEntity.ok(response);
        }
        else {
            // No pagination, return all games for player sorted by date
            List<Game> games = gameService.findByPlayerId(playerId);
            games.sort((g1, g2) -> g2.getDatePlayed().compareTo(g1.getDatePlayed()));

            return ResponseEntity.ok(games);
        }
    }

    /**
     * Reset all player ratings to their initial values
     *
     * @return Success/error information
     */
    @PatchMapping("/reset")
    public ResponseEntity<ApiResponse> resetAllPlayers() {
        log.info("Request to reset all players' ratings");

        gameService.resetAllPlayers();
        log.info("Successfully reset all player ratings");

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse(true, "All player ratings have been reset"));
    }

    /**
     * Delete a game by its ID
     *
     * @param id UUID of the game to delete
     * @return Empty response with appropriate status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteGame(@PathVariable("id") UUID id) {
        log.debug("Request to delete game: {}", id);

        Game game = gameService.findById(id);
        if (game == null) {
            log.warn("Game not found with ID: {}", id);
            throw new ResourceNotFoundException("Game not found with id: " + id);
        }

        gameService.deleteGame(id);
        log.info("Game deleted successfully: {}", id);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ApiResponse(true, "Game deleted successfully"));
    }
}