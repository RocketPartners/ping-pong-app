package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.GameResult;
import com.example.javapingpongelo.models.Match;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.PlayerStyleRating;
import jakarta.validation.constraints.Email;
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
import java.util.stream.Collectors;

/**
 * Data Transfer Object for the Player entity.
 * Used for transferring player data between the API and clients.
 * Excludes sensitive information like passwords.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerDTO {
    private UUID playerId;

    @NotNull(message = "First name is required")
    @NotEmpty(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotNull(message = "Last name is required")
    @NotEmpty(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Username is required")
    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    private Date birthday;

    private String profileImage;

    // Ratings
    private int singlesRankedRating;

    private int doublesRankedRating;

    private int singlesNormalRating;

    private int doublesNormalRating;

    // Game statistics
    private int singlesRankedWins;

    private int singlesRankedLoses;

    private int doublesRankedWins;

    private int doublesRankedLoses;

    private int singlesNormalWins;

    private int singlesNormalLoses;

    private int doublesNormalWins;

    private int doublesNormalLoses;

    // Game histories - store match IDs for DTO simplicity
    private List<UUID> matchHistoryIds;

    private List<GameResult> gameHistory;

    // Player styles
    private List<PlayerStyleRating> playerStyles;

    // Audit timestamps
    private Date created;

    private Date updated;
    private Boolean isAnonymous;

    // Calculated fields
    private String fullName;

    private int overallRating;

    private int totalMatchesPlayed;

    private double singlesRankedWinRate;

    private double doublesRankedWinRate;

    private double singlesNormalWinRate;

    private double doublesNormalWinRate;

    /**
     * Converts a Player entity to a PlayerDTO.
     *
     * @param player The Player entity to convert
     * @return The resulting PlayerDTO
     */
    public static PlayerDTO fromEntity(Player player, List<GameResult> gameHistory) {
        if (player == null) return null;

        List<UUID> matchIds = player.getMatchHistory() != null ?
                player.getMatchHistory().stream()
                      .map(Match::getMatchId)
                      .collect(Collectors.toList()) :
                new ArrayList<>();

        return PlayerDTO.builder()
                        .playerId(player.getPlayerId())
                        .firstName(player.getFirstName())
                        .lastName(player.getLastName())
                        .email(player.getEmail())
                        .username(player.getUsername())
                        .birthday(player.getBirthday())
                        .profileImage(player.getProfileImage())
                        .singlesRankedRating(player.getSinglesRankedRating())
                        .doublesRankedRating(player.getDoublesRankedRating())
                        .singlesNormalRating(player.getSinglesNormalRating())
                        .doublesNormalRating(player.getDoublesNormalRating())
                        .singlesRankedWins(player.getSinglesRankedWins())
                        .singlesRankedLoses(player.getSinglesRankedLoses())
                        .doublesRankedWins(player.getDoublesRankedWins())
                        .doublesRankedLoses(player.getDoublesRankedLoses())
                        .singlesNormalWins(player.getSinglesNormalWins())
                        .singlesNormalLoses(player.getSinglesNormalLoses())
                        .doublesNormalWins(player.getDoublesNormalWins())
                        .doublesNormalLoses(player.getDoublesNormalLoses())
                        .matchHistoryIds(matchIds)
                        .gameHistory(gameHistory != null ? gameHistory : new ArrayList<>())
                        .playerStyles(player.getStyleRatings() != null ? player.getStyleRatings() : new ArrayList<>())
                        .created(player.getCreated())
                        .updated(player.getUpdated())
                        .isAnonymous(player.getIsAnonymous())
                        .fullName(player.getFullName())
                        .overallRating(player.getOverallRating())
                        .totalMatchesPlayed(player.getTotalMatchesPlayed())
                        .singlesRankedWinRate(player.getSinglesRankedWinRate())
                        .doublesRankedWinRate(player.getDoublesRankedWinRate())
                        .singlesNormalWinRate(player.getSinglesNormalWinRate())
                        .doublesNormalWinRate(player.getDoublesNormalWinRate())
                        .build();
    }

}