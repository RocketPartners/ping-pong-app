package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.EasterEgg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * WebSocket message for easter egg events (spawned, claimed, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EasterEggEvent {
    
    public enum EventType {
        EGG_SPAWNED,
        EGG_CLAIMED,
        EGG_EXPIRED
    }
    
    private EventType eventType;
    private UUID eggId;
    private String pageLocation;
    private String cssSelector;
    private String coordinates;
    private EasterEgg.EggType eggType;
    private int pointValue;
    private String claimedByPlayer; // Only set for EGG_CLAIMED events
    private long timestamp;
    
    /**
     * Create an EGG_SPAWNED event from an EasterEgg
     */
    public static EasterEggEvent spawned(EasterEgg egg) {
        return EasterEggEvent.builder()
                .eventType(EventType.EGG_SPAWNED)
                .eggId(egg.getId())
                .pageLocation(egg.getPageLocation())
                .cssSelector(egg.getCssSelector())
                .coordinates(egg.getCoordinates())
                .eggType(egg.getType())
                .pointValue(egg.getPointValue())
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Create an EGG_CLAIMED event
     */
    public static EasterEggEvent claimed(UUID eggId, String playerName) {
        return EasterEggEvent.builder()
                .eventType(EventType.EGG_CLAIMED)
                .eggId(eggId)
                .claimedByPlayer(playerName)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * Create an EGG_EXPIRED event
     */
    public static EasterEggEvent expired(UUID eggId) {
        return EasterEggEvent.builder()
                .eventType(EventType.EGG_EXPIRED)
                .eggId(eggId)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}