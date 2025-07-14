package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.Tournament;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * DTO for tournament creation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRequestDTO {

    @NotNull(message = "Tournament name is required")
    @NotEmpty(message = "Tournament name cannot be empty")
    @Size(min = 3, max = 100, message = "Tournament name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Tournament type is required")
    private Tournament.TournamentType tournamentType;

    @NotNull(message = "Game type is required")
    private Tournament.GameType gameType;

    @NotNull(message = "Seeding method is required")
    private Tournament.SeedingMethod seedingMethod;

    @NotNull(message = "Organizer ID is required")
    private UUID organizerId;

    @NotNull(message = "Player IDs are required")
    @Size(min = 2, message = "At least 2 players are required")
    @Builder.Default
    private List<UUID> playerIds = new ArrayList<>();

    // For doubles tournaments, maps player IDs to their partners
    @Builder.Default
    private List<TeamPairDTO> teamPairs = new ArrayList<>();

    @NotNull(message = "Start date is required")
    private Date startDate;

    // Optional end date
    private Date endDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeamPairDTO {
        private UUID player1Id;

        private UUID player2Id;
    }
}