package com.example.javapingpongelo.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time easter egg updates.
 * Enables STOMP messaging over WebSocket for broadcasting egg spawn/claim events.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages back to the client
        // on destinations prefixed with "/topic" and "/queue"
        config.enableSimpleBroker("/topic", "/queue");
        
        // Define prefix for messages that are bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        
        // Define prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/easter-eggs-ws" endpoint, enabling SockJS fallback options
        // This allows clients to connect to the WebSocket server
        registry.addEndpoint("/easter-eggs-ws")
                .setAllowedOriginPatterns("*") // Allow all origins for development
                .setAllowedOrigins("http://localhost:4200") // Specifically allow frontend origin
                .withSockJS(); // Enable SockJS fallback options
    }
}