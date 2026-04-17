package com.example.javapingpongelo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived pairing codes that an admin mints and a kiosk redeems.
 *
 * Keeps all state in memory — if the backend restarts, in-flight codes are lost
 * but that's fine because the admin can just mint another one.
 */
@Service
@Slf4j
public class KioskPairingCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.kiosk.pairing-ttl-seconds:300}")
    private long ttlSeconds;

    private final Map<String, PendingPairing> codes = new ConcurrentHashMap<>();

    public PendingPairing mint(String deviceName) {
        purgeExpired();
        String code = generateUniqueCode();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        PendingPairing pending = new PendingPairing(code, deviceName, expiresAt);
        codes.put(code, pending);
        log.info("Minted kiosk pairing code for device '{}' (expires {})", deviceName, expiresAt);
        return pending;
    }

    public Optional<PendingPairing> redeem(String code) {
        purgeExpired();
        PendingPairing pending = codes.remove(code);
        if (pending == null) {
            log.info("Kiosk pairing code {} was not valid or already redeemed", code);
            return Optional.empty();
        }
        if (Instant.now().isAfter(pending.expiresAt())) {
            log.info("Kiosk pairing code {} was expired", code);
            return Optional.empty();
        }
        log.info("Kiosk pairing code {} redeemed for device '{}'", code, pending.deviceName());
        return Optional.of(pending);
    }

    @Scheduled(fixedDelay = 60_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 20; i++) {
            String candidate = String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!codes.containsKey(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique pairing code");
    }

    public record PendingPairing(String code, String deviceName, Instant expiresAt) {}
}
