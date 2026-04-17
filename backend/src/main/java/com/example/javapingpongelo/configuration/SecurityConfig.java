package com.example.javapingpongelo.configuration;

import com.example.javapingpongelo.utils.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CorsFilter corsFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults()) // Enable CORS
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Kiosk token/code minting must be admin-only; listed before /api/auth/** permitAll.
                        // Redeem endpoint stays public — security is the 5-minute single-use pairing code.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/kiosk-pairing-codes/redeem").permitAll()
                        .requestMatchers("/api/auth/kiosk-token").hasRole("ADMIN")
                        .requestMatchers("/api/auth/kiosk-pairing-codes").hasRole("ADMIN")
                        // Request-based pairing: kiosk can create + poll its own request; admins manage them.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/kiosk-pair-requests").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/kiosk-pair-requests/*").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/kiosk-pair-requests").hasRole("ADMIN")
                        .requestMatchers("/api/auth/kiosk-pair-requests/*/approve").hasRole("ADMIN")
                        .requestMatchers("/api/auth/kiosk-pair-requests/*/deny").hasRole("ADMIN")
                        // Heartbeat is any authenticated kiosk; list/delete are admin-only.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/kiosk-devices/heartbeat").authenticated()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/auth/kiosk-devices").hasRole("ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/auth/kiosk-devices/*").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/password-reset/**").permitAll()
                        .requestMatchers("/api/verify-email/**").permitAll()
                        .requestMatchers("/verify-email").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/api/actuator/health").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Slack integration endpoints - no authentication required
                        .requestMatchers("/api/slack/**").permitAll()
                        // Restrict test endpoints to dev environments only
                        .requestMatchers("/test/**", "/api/test/**").hasRole("ADMIN")
                        .requestMatchers("/api/invitations/validate").permitAll()
                        .requestMatchers("/api/invitations/**").hasRole("ADMIN")
                        .requestMatchers("/api/players/bootstrap/**").permitAll()
                        // WebSocket endpoints - allow unauthenticated connections
                        .requestMatchers("/easter-eggs-ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}