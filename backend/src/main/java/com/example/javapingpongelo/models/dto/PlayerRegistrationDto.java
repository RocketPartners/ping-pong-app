package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.validators.PasswordMatches;
import com.example.javapingpongelo.validators.ValidEmail;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Data Transfer Object for player registration.
 * Separates registration input validation from the Player entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PasswordMatches(message = "Passwords do not match")
public class PlayerRegistrationDto {

    @NotNull(message = "First name is required")
    @NotEmpty(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotNull(message = "Last name is required")
    @NotEmpty(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotNull(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Matching password is required")
    private String matchingPassword;

    @ValidEmail(message = "Email must be valid")
    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    private String email;

    @NotNull(message = "Username is required")
    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Past(message = "Birthday must be in the past")
    private Date birthday;

    /**
     * Style ratings for the player
     */
    @Builder.Default
    private PlayerStyleRatingDto styleRatings = new PlayerStyleRatingDto();
    
    /**
     * Invitation code for registration with restricted domains
     */
    private String invitationCode;
}