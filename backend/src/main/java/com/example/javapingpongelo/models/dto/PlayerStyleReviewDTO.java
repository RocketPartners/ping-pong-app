package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.PlayerStyle;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for player style reviews
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleReviewDTO {
    private UUID id;

    @NotNull(message = "Reviewer ID is required")
    private UUID reviewerId;

    @NotNull(message = "Player ID is required")
    private UUID playerId;

    @NotNull(message = "Game IDs are required")
    private List<UUID> gameIds;

    @NotNull(message = "Strengths list is required")
    private List<PlayerStyle> strengths;

    @NotNull(message = "Improvements list is required")
    private List<PlayerStyle> improvements;

    @Builder.Default
    private boolean acknowledged = false;

    private Date acknowledgedDate;

    @Builder.Default
    private boolean response = false;

    private UUID parentReviewId;

    private Date reviewDate;

    // New fields for reviewer information
    private String reviewerUsername;

    private String reviewerFirstName;

    private String reviewerLastName;
}