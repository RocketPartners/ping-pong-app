package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.GameConfirmation;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.models.exceptions.ResourceNotFoundException;
import com.example.javapingpongelo.services.GameConfirmationService;
import com.example.javapingpongelo.services.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for handling game confirmation operations
 */
@RestController
@RequestMapping("/api/game-confirmations")
@Slf4j
public class GameConfirmationController {

    @Autowired
    private GameConfirmationService gameConfirmationService;

    @Autowired
    private GameService gameService;

    /**
     * Reject a game using the confirmation token
     * This endpoint is accessed directly from the email link
     */
    @GetMapping("/reject")
    public RedirectView rejectGame(@RequestParam("token") String token) {
        log.info("Processing game rejection request with token");

        try {
            // Get the confirmation details first
            GameConfirmation confirmation = gameConfirmationService.getConfirmationByToken(token);
            if (confirmation == null) {
                throw new ResourceNotFoundException("Invalid token");
            }

            // Process the rejection in GameConfirmationService (mark as rejected)
            gameConfirmationService.rejectGame(token);

            // Process the actual game deletion and ELO reversion in GameService
            gameService.handleGameRejection(confirmation.getGameId(), confirmation.getPlayerId());

            return new RedirectView("/game-rejected?success=true");
        }
        catch (Exception e) {
            // For redirect endpoints, we can't rely on the global exception handler
            // So we need to handle exceptions here and redirect with error params
            log.error("Error processing rejection", e);
            String errorMsg = e instanceof ResourceNotFoundException || e instanceof BadRequestException
                    ? e.getMessage()
                    : "An unexpected error occurred";

            return new RedirectView("/game-rejected?success=false&error=" + errorMsg);
        }
    }

    /**
     * API endpoint for rejecting a game (for programmatic use)
     */
    @PostMapping("/reject")
    public ResponseEntity<ApiResponse> rejectGameApi(@RequestParam("token") String token) {
        log.info("Processing API game rejection request");

        // Get the confirmation details first
        GameConfirmation confirmation = gameConfirmationService.getConfirmationByToken(token);
        if (confirmation == null) {
            throw new ResourceNotFoundException("Invalid token");
        }

        // Process the rejection in GameConfirmationService (mark as rejected)
        gameConfirmationService.rejectGame(token);

        // Process the actual game deletion and ELO reversion in GameService
        gameService.handleGameRejection(confirmation.getGameId(), confirmation.getPlayerId());

        return ResponseEntity.ok(new ApiResponse(true, "Game successfully rejected"));
    }
}