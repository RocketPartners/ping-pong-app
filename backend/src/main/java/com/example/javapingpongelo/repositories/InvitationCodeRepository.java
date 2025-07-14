package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.InvitationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationCodeRepository extends JpaRepository<InvitationCode, UUID> {
    Optional<InvitationCode> findByCode(String code);
    List<InvitationCode> findByAllowedDomain(String domain);
    List<InvitationCode> findBySpecificEmail(String email);
    List<InvitationCode> findByUsedFalseAndExpiryDateAfter(LocalDateTime now);
}