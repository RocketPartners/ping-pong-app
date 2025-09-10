package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.dto.EasterEggEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting easter egg events via WebSocket
 */
@Service
@Slf4j
public class EasterEggWebSocketService {
    
    private static final String EASTER_EGG_TOPIC = "/topic/easter-eggs";
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Broadcast an easter egg event to all connected clients
     */
    public void broadcastEasterEggEvent(EasterEggEvent event) {
        try {
            log.info("Broadcasting easter egg event: {} for egg {}", 
                    event.getEventType(), event.getEggId());
            
            messagingTemplate.convertAndSend(EASTER_EGG_TOPIC, event);
            
        } catch (Exception e) {
            log.error("Failed to broadcast easter egg event: {}", event, e);
        }
    }
    
    /**
     * Send easter egg event to a specific user
     */
    public void sendEasterEggEventToUser(String username, EasterEggEvent event) {
        try {
            log.info("Sending easter egg event to user {}: {} for egg {}", 
                    username, event.getEventType(), event.getEggId());
            
            messagingTemplate.convertAndSendToUser(username, "/queue/easter-eggs", event);
            
        } catch (Exception e) {
            log.error("Failed to send easter egg event to user {}: {}", username, event, e);
        }
    }
}