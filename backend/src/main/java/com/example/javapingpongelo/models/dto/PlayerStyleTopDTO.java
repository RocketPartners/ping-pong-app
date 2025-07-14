package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.PlayerStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleTopDTO {
    private PlayerStyle style;

    private int rating;

    private UUID playerId;

    private String playerUsername;

    private String playerFullName;
}