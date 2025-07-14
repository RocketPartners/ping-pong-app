package com.example.javapingpongelo.models;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Builder
@Getter
@Setter
public class AuthResponse {
    private Player player;

    private String token;

    // Additional constructor that sets the token on the player object
    public AuthResponse(Player player, String token) {
        this.player = player;
        this.token = token;

        // Set token on player for frontend convenience
        if (player != null) {
            player.setToken(token);
        }
    }
}