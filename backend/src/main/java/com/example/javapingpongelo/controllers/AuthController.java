package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.AuthRequest;
import com.example.javapingpongelo.models.AuthResponse;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.dto.PlayerRegistrationDto;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.utils.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * Controller for authentication operations like login, registration and logout.
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /**
     * Authenticate a user and generate a JWT token.
     *
     * @param loginRequest  Login credentials
     * @param bindingResult Validation result
     * @return Authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody AuthRequest loginRequest, BindingResult bindingResult) {
        log.debug("Authentication request for: {}", loginRequest.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in login request: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Get player details
            Player player = playerService.findPlayerByUsername(loginRequest.getUsername());

            log.info("User authenticated successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(new AuthResponse(player, jwt));
        }
        catch (BadCredentialsException e) {
            log.warn("Authentication failed due to invalid credentials: {}", loginRequest.getUsername());
            throw new BadRequestException("Invalid username or password");
        }
    }

    /**
     * Register a new user account.
     *
     * @param registrationDto Registration information
     * @param bindingResult   Validation result
     * @return Registration result
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody PlayerRegistrationDto registrationDto, BindingResult bindingResult) {
        log.debug("Registration request for: {}", registrationDto.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in registration: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        // Check if passwords match
        if (!registrationDto.getPassword().equals(registrationDto.getMatchingPassword())) {
            log.warn("Passwords don't match for registration: {}", registrationDto.getUsername());
            throw new BadRequestException("Passwords don't match");
        }

        // Convert DTO to Player entity and register
        Player player = convertToEntity(registrationDto);
        Player registered = null;
        try {
            registered = playerService.registerNewUserAccount(player);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("User registered successfully: {}", registered.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "User registered successfully"));
    }

    /**
     * Convert registration DTO to Player entity.
     */
    private Player convertToEntity(PlayerRegistrationDto dto) {
        Player player = Player.builder()
                              .firstName(dto.getFirstName())
                              .lastName(dto.getLastName())
                              .email(dto.getEmail())
                              .username(dto.getUsername())
                              .password(dto.getPassword())
                              .matchingPassword(dto.getMatchingPassword())
                              .birthday(dto.getBirthday())
                              .invitationCode(dto.getInvitationCode())
                              .build();

        // Initialize style ratings if provided
        if (dto.getStyleRatings() != null) {
            player.initializeStyleRatings(dto.getStyleRatings().toMap());
        }
        else {
            // Initialize with default values
            player.initializeStyleRatings();
        }

        return player;
    }

    /**
     * Logout the current user.
     *
     * @return Logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        // Clear the security context
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");

        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }
}