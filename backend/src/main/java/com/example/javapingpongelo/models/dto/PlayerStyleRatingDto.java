package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.PlayerStyle;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object for player style ratings during registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleRatingDto {

    @Min(value = 0, message = "SPIN rating cannot be less than 0")
    @Max(value = 100, message = "SPIN rating cannot be more than 100")
    @Builder.Default
    private int spin = 50;

    @Min(value = 0, message = "POWER rating cannot be less than 0")
    @Max(value = 100, message = "POWER rating cannot be more than 100")
    @Builder.Default
    private int power = 50;

    @Min(value = 0, message = "CREATIVE rating cannot be less than 0")
    @Max(value = 100, message = "CREATIVE rating cannot be more than 100")
    @Builder.Default
    private int creative = 50;

    @Min(value = 0, message = "AGGRESSIVE rating cannot be less than 0")
    @Max(value = 100, message = "AGGRESSIVE rating cannot be more than 100")
    @Builder.Default
    private int aggressive = 50;

    @Min(value = 0, message = "RESILIENT rating cannot be less than 0")
    @Max(value = 100, message = "RESILIENT rating cannot be more than 100")
    @Builder.Default
    private int resilient = 50;

    @Min(value = 0, message = "ACE_MASTER rating cannot be less than 0")
    @Max(value = 100, message = "ACE_MASTER rating cannot be more than 100")
    @Builder.Default
    private int aceMaster = 50;

    @Min(value = 0, message = "RALLY_KING rating cannot be less than 0")
    @Max(value = 100, message = "RALLY_KING rating cannot be more than 100")
    @Builder.Default
    private int rallyKing = 50;

    @Min(value = 0, message = "TACTICIAN rating cannot be less than 0")
    @Max(value = 100, message = "TACTICIAN rating cannot be more than 100")
    @Builder.Default
    private int tactician = 50;

    /**
     * Convert DTO to a map of PlayerStyle to rating values
     */
    public Map<PlayerStyle, Integer> toMap() {
        Map<PlayerStyle, Integer> map = new HashMap<>();
        map.put(PlayerStyle.SPIN, spin);
        map.put(PlayerStyle.POWER, power);
        map.put(PlayerStyle.CREATIVE, creative);
        map.put(PlayerStyle.AGGRESSIVE, aggressive);
        map.put(PlayerStyle.RESILIENT, resilient);
        map.put(PlayerStyle.ACE_MASTER, aceMaster);
        map.put(PlayerStyle.RALLY_KING, rallyKing);
        map.put(PlayerStyle.TACTICIAN, tactician);
        return map;
    }
}