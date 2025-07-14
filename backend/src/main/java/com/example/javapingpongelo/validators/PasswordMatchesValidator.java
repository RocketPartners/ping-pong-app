package com.example.javapingpongelo.validators;

import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.dto.PlayerRegistrationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        if (obj instanceof Player player) {
            return player.getPassword() != null && player.getPassword().equals(player.getMatchingPassword());
        }
        else if (obj instanceof PlayerRegistrationDto dto) {
            return dto.getPassword() != null && dto.getPassword().equals(dto.getMatchingPassword());
        }

        // If the object is neither a Player nor a PlayerRegistrationDto,
        // we can't validate it, so return false
        return false;
    }
}