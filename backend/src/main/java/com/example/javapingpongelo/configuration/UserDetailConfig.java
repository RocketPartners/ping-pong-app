package com.example.javapingpongelo.configuration;

import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.PlayerRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
public class UserDetailConfig {
    @Bean
    public UserDetailsService userDetailsService(PlayerRepository playerRepository) {
        return username -> {
            Player player = playerRepository.findByUsername(username);
            if (player == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }

            return User
                    .withUsername(username)
                    .password(player.getPassword())
                    .authorities("USER") // Add roles if needed
                    .build();
        };
    }
}
