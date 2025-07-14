package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.Player;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for player statistics that avoids lazy loading issues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStatsDTO {
    private String playerId;

    private String username;

    private String firstName;

    private String lastName;

    private Date created;

    private String profileImage;

    // Rating information
    private int singlesRankedRating;

    private int singlesNormalRating;

    private int doublesRankedRating;

    private int doublesNormalRating;

    // Win/loss counts
    private int totalWins;

    private int totalLosses;

    /**
     * Factory method to create DTO from Player entity
     */
    public static PlayerStatsDTO fromPlayer(Player player) {
        if (player == null) return null;

        int totalWins = player.getSinglesRankedWins() + player.getSinglesNormalWins() +
                player.getDoublesRankedWins() + player.getDoublesNormalWins();

        int totalLosses = player.getSinglesRankedLoses() + player.getSinglesNormalLoses() +
                player.getDoublesRankedLoses() + player.getDoublesNormalLoses();

        return PlayerStatsDTO.builder()
                             .playerId(player.getPlayerId().toString())
                             .username(player.getUsername())
                             .firstName(player.getFirstName())
                             .lastName(player.getLastName())
                             .created(player.getCreated())
                             .profileImage(player.getProfileImage())
                             .singlesRankedRating(player.getSinglesRankedRating())
                             .singlesNormalRating(player.getSinglesNormalRating())
                             .doublesRankedRating(player.getDoublesRankedRating())
                             .doublesNormalRating(player.getDoublesNormalRating())
                             .totalWins(totalWins)
                             .totalLosses(totalLosses)
                             .build();
    }
}