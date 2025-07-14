package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for player ELO history entries, optimized for charting
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerEloHistoryDTO {
    private Date timestamp;

    private int eloRating;

    private int eloChange;
}
