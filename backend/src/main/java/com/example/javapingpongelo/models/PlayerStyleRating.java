package com.example.javapingpongelo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a player's rating in a specific style
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerStyleRating {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStyle styleType;

    @Min(value = 0, message = "Style rating cannot be less than 0")
    @Max(value = 100, message = "Style rating cannot be more than 100")
    @Column(nullable = false)
    private int rating;

    // Method to update rating
    public void updateRating(int newValue) {
        // Ensure value is within range
        this.rating = Math.max(0, Math.min(100, newValue));
    }

    // Increment rating by a value
    public void incrementRating(int increment) {
        this.rating = Math.max(0, Math.min(100, this.rating + increment));
    }
}