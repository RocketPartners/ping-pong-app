package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiDataResponse;
import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.InvitationCode;
import com.example.javapingpongelo.services.EmailSender;
import com.example.javapingpongelo.services.EmailService;
import com.example.javapingpongelo.services.InvitationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitations")
@Validated
public class InvitationController {

    private final InvitationService invitationService;
    private final EmailSender emailSender;

    @Autowired
    public InvitationController(InvitationService invitationService, EmailSender emailSender) {
        this.invitationService = invitationService;
        this.emailSender = emailSender;
    }

    @PostMapping("/generate/domain")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<String>> generateDomainInvitation(@RequestBody @Valid DomainInvitationRequest request) {
        String code = invitationService.generateDomainInvitationCode(request.getDomain());
        return ResponseEntity.ok(ApiDataResponse.success("Domain invitation code generated successfully", code));
    }

    @PostMapping("/generate/email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<String>> generateEmailInvitation(@RequestBody @Valid EmailInvitationRequest request) {
        String code = invitationService.generateSpecificEmailInvitationCode(request.getEmail());
        
        // Send invitation email
        emailSender.sendInvitationEmail(request.getEmail(), code);
        
        return ResponseEntity.ok(ApiDataResponse.success("Email invitation code generated and sent successfully", code));
    }

    @PostMapping("/generate/general")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<String>> generateGeneralInvitation() {
        String code = invitationService.generateGeneralInvitationCode();
        return ResponseEntity.ok(ApiDataResponse.success("General invitation code generated successfully", code));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiDataResponse<Boolean>> validateInvitationCode(@RequestBody @Valid ValidateInvitationRequest request) {
        boolean isValid = invitationService.validateInvitationCode(request.getCode(), request.getEmail());
        return ResponseEntity.ok(ApiDataResponse.success("Invitation code validation result", isValid));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiDataResponse<List<InvitationCode>>> getActiveInvitations() {
        List<InvitationCode> activeCodes = invitationService.getAllActiveInvitationCodes();
        return ResponseEntity.ok(ApiDataResponse.success("Active invitation codes retrieved successfully", activeCodes));
    }

    @Data
    public static class DomainInvitationRequest {
        @NotBlank(message = "Domain is required")
        private String domain;
    }

    @Data
    public static class EmailInvitationRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Data
    public static class ValidateInvitationRequest {
        @NotBlank(message = "Invitation code is required")
        private String code;
        
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }
}