package com.example.javapingpongelo.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tracks kiosk pairing requests awaiting admin approval.
 *
 * A kiosk creates a pending request, shows "waiting for approval", and polls
 * this service via the public status endpoint. An admin sees pending requests
 * in the admin UI and approves or denies them. On approval, the backend stores
 * the minted JWT against the request so the kiosk's next poll picks it up.
 */
@Service
@Slf4j
public class KioskPairingRequestService {

    public enum Status { PENDING, APPROVED, DENIED, EXPIRED }

    @Value("${app.kiosk.pairing-request-ttl-seconds:600}")
    private long pendingTtlSeconds;

    @Value("${app.kiosk.pairing-approved-ttl-seconds:60}")
    private long approvedTtlSeconds;

    private final ConcurrentMap<String, PairingRequest> requests = new ConcurrentHashMap<>();

    public PairingRequest create(String deviceName, String userAgent) {
        purgeExpired();
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        PairingRequest req = new PairingRequest();
        req.id = id;
        req.deviceName = deviceName != null && !deviceName.isBlank() ? deviceName : "Unnamed device";
        req.userAgent = userAgent;
        req.createdAt = now;
        req.expiresAt = now.plusSeconds(pendingTtlSeconds);
        req.status = Status.PENDING;
        requests.put(id, req);
        log.info("New kiosk pairing request {} from {}", id, req.deviceName);
        return req;
    }

    public Optional<PairingRequest> get(String id) {
        PairingRequest req = requests.get(id);
        if (req == null) return Optional.empty();
        // Surface expiry to the poller.
        if (req.status == Status.PENDING && Instant.now().isAfter(req.expiresAt)) {
            req.status = Status.EXPIRED;
        }
        return Optional.of(req);
    }

    public List<PairingRequest> listPending() {
        purgeExpired();
        List<PairingRequest> pending = new ArrayList<>();
        for (PairingRequest r : requests.values()) {
            if (r.status == Status.PENDING) pending.add(r);
        }
        return pending;
    }

    public Optional<PairingRequest> approve(String id, String token, String approvedBy) {
        PairingRequest req = requests.get(id);
        if (req == null || req.status != Status.PENDING) return Optional.empty();
        if (Instant.now().isAfter(req.expiresAt)) {
            req.status = Status.EXPIRED;
            return Optional.empty();
        }
        req.status = Status.APPROVED;
        req.token = token;
        req.approvedBy = approvedBy;
        req.resolvedAt = Instant.now();
        // Kiosk has a short window to claim the token on its next poll before we drop it.
        req.expiresAt = req.resolvedAt.plusSeconds(approvedTtlSeconds);
        log.info("Kiosk pairing {} approved by {}", id, approvedBy);
        return Optional.of(req);
    }

    public Optional<PairingRequest> deny(String id, String deniedBy) {
        PairingRequest req = requests.get(id);
        if (req == null || req.status != Status.PENDING) return Optional.empty();
        req.status = Status.DENIED;
        req.approvedBy = deniedBy;
        req.resolvedAt = Instant.now();
        log.info("Kiosk pairing {} denied by {}", id, deniedBy);
        return Optional.of(req);
    }

    @Scheduled(fixedDelay = 30_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        requests.entrySet().removeIf(entry -> {
            PairingRequest r = entry.getValue();
            if (r.status == Status.APPROVED || r.status == Status.DENIED || r.status == Status.EXPIRED) {
                // Keep resolved requests briefly so the poller can learn the outcome.
                return r.resolvedAt != null && now.isAfter(r.resolvedAt.plusSeconds(60));
            }
            return now.isAfter(r.expiresAt);
        });
    }

    public static class PairingRequest {
        public String id;
        public String deviceName;
        public String userAgent;
        public Status status;
        public Instant createdAt;
        public Instant expiresAt;
        public Instant resolvedAt;
        public String token;
        public String approvedBy;
    }
}
