package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiDataResponse;
import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.services.EmailVerificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@Validated
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Autowired
    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    /**
     * Handle email verification link
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        boolean isVerified = emailVerificationService.verifyEmail(token);
        
        if (isVerified) {
            model.addAttribute("message", "Your email has been verified successfully. You can now log in.");
            model.addAttribute("success", true);
        } else {
            model.addAttribute("message", "Invalid or expired verification link. Please request a new one.");
            model.addAttribute("success", false);
        }
        
        return "verification-result";
    }

    /**
     * API endpoint for email verification
     */
    @RestController
    @RequestMapping("/api/verify-email")
    @Validated
    public static class EmailVerificationRestController {
        
        private final EmailVerificationService emailVerificationService;
        
        @Autowired
        public EmailVerificationRestController(EmailVerificationService emailVerificationService) {
            this.emailVerificationService = emailVerificationService;
        }
        
        @PostMapping("/verify")
        public ResponseEntity<ApiDataResponse<Boolean>> verifyEmail(@RequestBody @Valid TokenRequest request) {
            boolean isVerified = emailVerificationService.verifyEmail(request.getToken());
            String message = isVerified 
                ? "Email verified successfully" 
                : "Invalid or expired verification token";
            
            return ResponseEntity.ok(ApiDataResponse.success(message, isVerified));
        }
        
        @PostMapping("/resend")
        public ResponseEntity<ApiDataResponse<Boolean>> resendVerification(@RequestBody @Valid EmailRequest request) {
            boolean sent = emailVerificationService.resendVerificationEmail(request.getEmail());
            String message = sent 
                ? "Verification email sent successfully" 
                : "Failed to send verification email. User may not exist or is already verified.";
            
            return ResponseEntity.ok(ApiDataResponse.success(message, sent));
        }
        
        @Data
        public static class TokenRequest {
            @NotBlank(message = "Token is required")
            private String token;
        }
        
        @Data
        public static class EmailRequest {
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            private String email;
        }
    }
}