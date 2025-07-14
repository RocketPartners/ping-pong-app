package com.example.javapingpongelo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for displaying game rejection result page
 */
@Controller
public class GameRejectedController {

    /**
     * Display the game rejected confirmation page
     */
    @GetMapping("/game-rejected")
    public String gameRejected() {
        return "game-rejected";
    }
}