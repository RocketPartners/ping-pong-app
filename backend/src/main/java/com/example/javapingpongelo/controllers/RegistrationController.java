package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.services.IPlayerService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/user")
@Slf4j
public class RegistrationController {

    @Autowired
    private IPlayerService playerService;

    @GetMapping("/registration")
    public String showRegistrationForm(WebRequest request, Model model) {
        log.debug("Showing registration form");
        Player player = new Player();
        model.addAttribute("player", player);
        return "registration";
    }

    @PostMapping("/registration")
    public ModelAndView registerUserAccount(
            @ModelAttribute("player") @Valid Player player,
            Errors errors) {

        log.debug("Processing registration for: {}", player.getUsername());

        ModelAndView mav = new ModelAndView();

        // Check for validation errors
        if (errors.hasErrors()) {
            log.debug("Validation errors in registration form");
            mav.setViewName("registration");
            return mav;
        }

        // Validate password matching
        if (!player.getPassword().equals(player.getMatchingPassword())) {
            log.debug("Passwords don't match");
            mav.addObject("message", "Passwords don't match");
            mav.setViewName("registration");
            return mav;
        }

        try {
            Player registered = playerService.registerNewUserAccount(player);
            log.info("Successfully registered player: {}", registered.getUsername());
            return new ModelAndView("successRegister", "player", registered);
        }
        catch (Exception e) {
            log.error("Error registering new account", e);
            mav.addObject("message", "Registration failed: " + e.getMessage());
            mav.setViewName("registration");
            return mav;
        }
    }

    // REST API endpoint for registration
    @PostMapping("/api/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody Player player) {
        log.debug("API registration request for: {}", player.getUsername());

        // Validate password matching
        if (!player.getPassword().equals(player.getMatchingPassword())) {
            throw new BadRequestException("Passwords don't match");
        }

        Player registered;
        try {
            registered = playerService.registerNewUserAccount(player);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Successfully registered player via API: {}", registered.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "User registered successfully"));
    }
}