package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.EmailVerificationToken;
import com.example.javapingpongelo.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    List<EmailVerificationToken> findByPlayer(Player player);
    void deleteByPlayer(Player player);
}