package com.example.javapingpongelo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent record of a paired kiosk device. Registered when an admin approves
 * a pairing request (or a kiosk token is minted directly). The JWT's jti claim
 * is the lookup key used during heartbeats and revocation checks.
 */
@Entity
@Table(name = "kiosk_device", indexes = {
        @Index(name = "idx_kiosk_device_jti", columnList = "jti", unique = true),
        @Index(name = "idx_kiosk_device_revoked", columnList = "revoked")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KioskDeviceEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String jti;

    @Column(nullable = false, length = 100)
    private String deviceName;

    @Column(length = 100)
    private String approvedBy;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private Instant approvedAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean revoked;

    private Instant revokedAt;
}
