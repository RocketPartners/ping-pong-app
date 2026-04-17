package com.example.javapingpongelo.services;

import com.example.javapingpongelo.models.KioskDeviceEntity;
import com.example.javapingpongelo.repositories.KioskDeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tracks which kiosk devices have active long-lived JWTs.
 *
 * Persisted in the kiosk_device table so paired devices survive backend restarts.
 * Revocation is authoritative — the JwtAuthenticationFilter consults this service
 * and denies any request whose token's jti matches a revoked device record.
 */
@Service
@Slf4j
@Transactional
public class KioskDeviceService {

    @Autowired
    private KioskDeviceRepository repository;

    public KioskDevice register(String jti, String deviceName, String approvedBy, String userAgent) {
        Instant now = Instant.now();
        KioskDeviceEntity entity = KioskDeviceEntity.builder()
                .id(UUID.randomUUID())
                .jti(jti)
                .deviceName(deviceName != null && !deviceName.isBlank() ? deviceName : "Unnamed")
                .approvedBy(approvedBy)
                .userAgent(userAgent)
                .approvedAt(now)
                .lastSeenAt(now)
                .revoked(false)
                .build();
        KioskDeviceEntity saved = repository.save(entity);
        log.info("Registered kiosk device {} ({}) approved by {}", saved.getId(), saved.getDeviceName(), approvedBy);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<KioskDevice> findByJti(String jti) {
        if (jti == null) return Optional.empty();
        return repository.findByJti(jti)
                .filter(e -> !e.isRevoked())
                .map(this::toDto);
    }

    public Optional<KioskDevice> heartbeat(String jti) {
        if (jti == null) return Optional.empty();
        return repository.findByJti(jti)
                .filter(e -> !e.isRevoked())
                .map(e -> {
                    e.setLastSeenAt(Instant.now());
                    repository.save(e);
                    return toDto(e);
                });
    }

    @Transactional(readOnly = true)
    public List<KioskDevice> list() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public boolean revoke(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return repository.findById(uuid).map(entity -> {
            entity.setRevoked(true);
            entity.setRevokedAt(Instant.now());
            repository.save(entity);
            log.info("Revoked kiosk device {} ({})", entity.getId(), entity.getDeviceName());
            return true;
        }).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isJtiRevoked(String jti) {
        if (jti == null) return false;
        return repository.existsByJtiAndRevokedTrue(jti);
    }

    private KioskDevice toDto(KioskDeviceEntity e) {
        KioskDevice d = new KioskDevice();
        d.id = e.getId() != null ? e.getId().toString() : null;
        d.jti = e.getJti();
        d.deviceName = e.getDeviceName();
        d.approvedBy = e.getApprovedBy();
        d.userAgent = e.getUserAgent();
        d.approvedAt = e.getApprovedAt();
        d.lastSeenAt = e.getLastSeenAt();
        d.revoked = e.isRevoked();
        d.revokedAt = e.getRevokedAt();
        return d;
    }

    public static class KioskDevice {
        public String id;
        public String jti;
        public String deviceName;
        public String approvedBy;
        public String userAgent;
        public Instant approvedAt;
        public Instant lastSeenAt;
        public boolean revoked;
        public Instant revokedAt;
    }
}
