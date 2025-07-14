package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.services.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-reset")
@Slf4j
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        log.debug("Password reset request for email: {}", request.getEmail());

        // Note: For security reasons, we always return a success response regardless of
        // whether an account with the email actually exists
        try {
            passwordResetService.createPasswordResetTokenForEmail(request.getEmail());
        }
        catch (Exception e) {
            log.error("Error processing password reset request", e);
        }

        return ResponseEntity.ok(new ApiResponse(true,
                                                 "If an account with that email exists, we have sent a password reset link"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody PasswordResetCompleteRequest request) {
        log.debug("Processing password reset with token");

        if (!passwordResetService.validatePasswordResetToken(request.getToken())) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        passwordResetService.resetPassword(request.getToken(), request.getPassword());

        return ResponseEntity.ok(new ApiResponse(true, "Password has been reset successfully"));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse> validateToken(@RequestParam("token") String token) {
        boolean valid = passwordResetService.validatePasswordResetToken(token);
        return ResponseEntity.ok(new ApiResponse(valid,
                                                 valid ? "Token is valid" : "Token is invalid or expired"));
    }

    @Data
    public static class PasswordResetRequest {
        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class PasswordResetCompleteRequest {
        @NotEmpty(message = "Token is required")
        private String token;

        @NotEmpty(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotEmpty(message = "Confirm password is required")
        private String confirmPassword;
    }
}