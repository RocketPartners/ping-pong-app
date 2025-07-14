package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * DTO for player ELO summary data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEloSummaryDTO {
    private UUID playerId;

    private String playerName;

    private int currentRank;

    private int highestRank;

    private Date highestRankDate;

    private int currentElo;

    private int highestElo;

    private Date highestEloDate;

    private int eloGain30Days;

    private int rankGain30Days;
}
