package com.example.javapingpongelo.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.javapingpongelo.models.EasterEgg;
import java.util.List;

/**
 * DTO representing the result of attempting to claim an easter egg.
 * Returns success/failure status and relevant information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggClaimResult {
    private boolean success;
    private String message;
    private int pointsEarned;
    private Integer newTotal;        // Player's new total points after this claim
    private EasterEgg.EggType eggType;  // Type of egg found
    private ClaimReason reason;
    private String finderName;       // Who found it if not the requester
    private String secretMessage;    // Special message from the egg
    private List<String> achievementsUnlocked; // Any achievements unlocked from this find
    
    /**
     * Reasons why an egg claim might succeed or fail
     */
    public enum ClaimReason {
        SUCCESS,
        ALREADY_CLAIMED,
        EGG_NOT_FOUND,
        EGG_EXPIRED,
        PLAYER_NOT_FOUND,
        HUNTING_DISABLED,
        SERVER_ERROR
    }
    
    /**
     * Static factory methods for common results
     */
    public static EggClaimResult success(int points, int newTotal, EasterEgg.EggType eggType, String secretMessage) {
        return EggClaimResult.builder()
            .success(true)
            .pointsEarned(points)
            .newTotal(newTotal)
            .eggType(eggType)
            .secretMessage(secretMessage)
            .reason(ClaimReason.SUCCESS)
            .message("Egg successfully found! You earned " + points + " points!")
            .build();
    }
    
    public static EggClaimResult alreadyClaimed(String finderName) {
        return EggClaimResult.builder()
            .success(false)
            .pointsEarned(0)
            .reason(ClaimReason.ALREADY_CLAIMED)
            .finderName(finderName)
            .message(finderName + " beat you to it! Better luck next time.")
            .build();
    }
    
    public static EggClaimResult notFound() {
        return EggClaimResult.builder()
            .success(false)
            .pointsEarned(0)
            .reason(ClaimReason.EGG_NOT_FOUND)
            .message("Egg not found or no longer available.")
            .build();
    }
    
    public static EggClaimResult expired() {
        return EggClaimResult.builder()
            .success(false)
            .pointsEarned(0)
            .reason(ClaimReason.EGG_EXPIRED)
            .message("This egg has expired. A new one will appear soon!")
            .build();
    }
    
    public static EggClaimResult huntingDisabled() {
        return EggClaimResult.builder()
            .success(false)
            .pointsEarned(0)
            .reason(ClaimReason.HUNTING_DISABLED)
            .message("Easter egg hunting is currently disabled.")
            .build();
    }
}