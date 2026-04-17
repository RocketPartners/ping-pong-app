package com.example.javapingpongelo.repositories;

import com.example.javapingpongelo.models.KioskDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface KioskDeviceRepository extends JpaRepository<KioskDeviceEntity, UUID> {
    Optional<KioskDeviceEntity> findByJti(String jti);
    boolean existsByJtiAndRevokedTrue(String jti);
}
