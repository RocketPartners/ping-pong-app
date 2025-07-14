package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.PlayerStyle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleAverageDTO {
    private PlayerStyle style;

    private double averageRating;
}