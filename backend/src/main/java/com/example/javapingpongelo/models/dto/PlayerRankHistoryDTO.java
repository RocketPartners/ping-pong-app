package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for player rank history entries
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRankHistoryDTO {
    private Date timestamp;

    private int rankPosition;

    private int totalPlayers;

    private double percentile;
}
