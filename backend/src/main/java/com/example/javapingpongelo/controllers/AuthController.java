package com.example.javapingpongelo.controllers;

import com.example.javapingpongelo.models.ApiResponse;
import com.example.javapingpongelo.models.AuthRequest;
import com.example.javapingpongelo.models.AuthResponse;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.models.dto.PlayerRegistrationDto;
import com.example.javapingpongelo.models.exceptions.BadRequestException;
import com.example.javapingpongelo.repositories.PlayerRepository;
import com.example.javapingpongelo.services.IPlayerService;
import com.example.javapingpongelo.services.KioskDeviceService;
import com.example.javapingpongelo.services.KioskPairingCodeService;
import com.example.javapingpongelo.services.KioskPairingRequestService;
import com.example.javapingpongelo.utils.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for authentication operations like login, registration and logout.
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IPlayerService playerService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private KioskPairingCodeService kioskPairingCodeService;

    @Autowired
    private KioskPairingRequestService kioskPairingRequestService;

    @Autowired
    private KioskDeviceService kioskDeviceService;

    @Value("${app.kiosk.username:kiosk-system}")
    private String kioskUsername;

    /**
     * Authenticate a user and generate a JWT token.
     *
     * @param loginRequest  Login credentials
     * @param bindingResult Validation result
     * @return Authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody AuthRequest loginRequest, BindingResult bindingResult) {
        log.debug("Authentication request for: {}", loginRequest.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in login request: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        try {
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);

            // Get player details
            Player player = playerService.findPlayerByUsername(loginRequest.getUsername());

            log.info("User authenticated successfully: {}", loginRequest.getUsername());
            return ResponseEntity.ok(new AuthResponse(player, jwt));
        }
        catch (BadCredentialsException e) {
            log.warn("Authentication failed due to invalid credentials: {}", loginRequest.getUsername());
            throw new BadRequestException("Invalid username or password");
        }
    }

    /**
     * Register a new user account.
     *
     * @param registrationDto Registration information
     * @param bindingResult   Validation result
     * @return Registration result
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody PlayerRegistrationDto registrationDto, BindingResult bindingResult) {
        log.debug("Registration request for: {}", registrationDto.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getAllErrors().stream()
                                         .map(DefaultMessageSourceResolvable::getDefaultMessage)
                                         .collect(Collectors.joining(", "));

            log.warn("Validation errors in registration: {}", errors);
            throw new BadRequestException("Validation errors: " + errors);
        }

        // Check if passwords match
        if (!registrationDto.getPassword().equals(registrationDto.getMatchingPassword())) {
            log.warn("Passwords don't match for registration: {}", registrationDto.getUsername());
            throw new BadRequestException("Passwords don't match");
        }

        // Convert DTO to Player entity and register
        Player player = convertToEntity(registrationDto);
        Player registered = null;
        try {
            registered = playerService.registerNewUserAccount(player);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("User registered successfully: {}", registered.getUsername());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "User registered successfully"));
    }

    /**
     * Convert registration DTO to Player entity.
     */
    private Player convertToEntity(PlayerRegistrationDto dto) {
        Player player = Player.builder()
                              .firstName(dto.getFirstName())
                              .lastName(dto.getLastName())
                              .email(dto.getEmail())
                              .username(dto.getUsername())
                              .password(dto.getPassword())
                              .matchingPassword(dto.getMatchingPassword())
                              .birthday(dto.getBirthday())
                              .invitationCode(dto.getInvitationCode())
                              .build();

        // Initialize style ratings if provided
        if (dto.getStyleRatings() != null) {
            player.initializeStyleRatings(dto.getStyleRatings().toMap());
        }
        else {
            // Initialize with default values
            player.initializeStyleRatings();
        }

        return player;
    }

    /**
     * Logout the current user.
     *
     * @return Logout confirmation
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout() {
        // Clear the security context
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");

        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }

    /**
     * Mint a long-lived JWT for a kiosk device. Admin-only; route protection is enforced
     * in SecurityConfig since /api/auth/** is otherwise permitAll.
     *
     * Preferred for CI/scripted pairing. For interactive pairing, use the 6-digit
     * pairing-code flow below — it avoids pasting a 300-char JWT onto a touchscreen.
     */
    @PostMapping("/kiosk-token")
    public ResponseEntity<Map<String, Object>> mintKioskToken(@RequestBody(required = false) Map<String, String> body) {
        String deviceName = body != null ? body.getOrDefault("deviceName", "unnamed") : "unnamed";
        String approver = currentUsername();
        return ResponseEntity.ok(issueKioskToken(deviceName, approver, null));
    }

    /**
     * Mint a short-lived 6-digit pairing code. Admin-only. The kiosk UI redeems
     * the code via the public redeem endpoint below, receiving the long-lived JWT.
     */
    @PostMapping("/kiosk-pairing-codes")
    public ResponseEntity<Map<String, Object>> mintKioskPairingCode(@RequestBody(required = false) Map<String, String> body) {
        String deviceName = body != null ? body.getOrDefault("deviceName", "unnamed") : "unnamed";
        KioskPairingCodeService.PendingPairing pending = kioskPairingCodeService.mint(deviceName);

        Map<String, Object> response = new HashMap<>();
        response.put("code", pending.code());
        response.put("deviceName", pending.deviceName());
        response.put("expiresAt", pending.expiresAt().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Redeem a 6-digit pairing code for a long-lived kiosk JWT. Public endpoint:
     * security lives in the short TTL, single-use nature of the code. Used by the
     * kiosk pairing screen.
     */
    @PostMapping("/kiosk-pairing-codes/redeem")
    public ResponseEntity<Map<String, Object>> redeemKioskPairingCode(@RequestBody Map<String, String> body) {
        String code = body != null ? body.getOrDefault("code", "").trim() : "";
        if (code.isEmpty()) {
            throw new BadRequestException("Missing pairing code");
        }

        return kioskPairingCodeService.redeem(code)
                .map(pending -> ResponseEntity.ok(issueKioskToken(pending.deviceName(), "pairing-code", null)))
                .orElseThrow(() -> new BadRequestException("Pairing code is invalid or expired"));
    }

    /**
     * Kiosk: heartbeat to keep its lastSeenAt fresh in the device registry.
     * Uses the kiosk's own JWT (attached by the app's jwt.interceptor).
     */
    @PostMapping("/kiosk-devices/heartbeat")
    public ResponseEntity<Map<String, Object>> kioskHeartbeat(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String jti = extractJti(authHeader);
        Map<String, Object> response = new HashMap<>();
        if (jti == null) {
            response.put("ok", false);
            return ResponseEntity.ok(response);
        }
        return kioskDeviceService.heartbeat(jti)
                .map(d -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("ok", true);
                    body.put("deviceName", d.deviceName);
                    body.put("lastSeenAt", d.lastSeenAt.toString());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> {
                    response.put("ok", false);
                    response.put("reason", "device not found or revoked");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                });
    }

    /**
     * Admin: list paired kiosk devices.
     */
    @GetMapping("/kiosk-devices")
    public ResponseEntity<List<Map<String, Object>>> listKioskDevices() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (KioskDeviceService.KioskDevice d : kioskDeviceService.list()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.id);
            m.put("deviceName", d.deviceName);
            m.put("approvedBy", d.approvedBy);
            m.put("userAgent", d.userAgent);
            m.put("approvedAt", d.approvedAt != null ? d.approvedAt.toString() : null);
            m.put("lastSeenAt", d.lastSeenAt != null ? d.lastSeenAt.toString() : null);
            m.put("revoked", d.revoked);
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Admin: revoke a paired kiosk. The next request carrying that JWT will fail
     * at the JwtAuthenticationFilter (via the revoked-jti check).
     */
    @org.springframework.web.bind.annotation.DeleteMapping("/kiosk-devices/{id}")
    public ResponseEntity<Map<String, Object>> revokeKioskDevice(@PathVariable("id") String id) {
        boolean ok = kioskDeviceService.revoke(id);
        Map<String, Object> response = new HashMap<>();
        response.put("ok", ok);
        return ok ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }

    private String extractJti(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        try {
            return tokenProvider.getJtiFromToken(authHeader.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Public: kiosk opens a pairing request and starts polling for admin approval.
     * Returns the request id the kiosk will use to poll.
     */
    @PostMapping("/kiosk-pair-requests")
    public ResponseEntity<Map<String, Object>> createKioskPairRequest(
            @RequestBody(required = false) Map<String, String> body,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String deviceName = body != null ? body.getOrDefault("deviceName", "") : "";
        KioskPairingRequestService.PairingRequest req =
                kioskPairingRequestService.create(deviceName, userAgent);

        Map<String, Object> response = new HashMap<>();
        response.put("id", req.id);
        response.put("deviceName", req.deviceName);
        response.put("expiresAt", req.expiresAt.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Public: kiosk polls this endpoint for the status of its pairing request.
     * If approved, returns the minted JWT so the kiosk can authenticate.
     */
    @GetMapping("/kiosk-pair-requests/{id}")
    public ResponseEntity<Map<String, Object>> getKioskPairRequest(@PathVariable("id") String id) {
        return kioskPairingRequestService.get(id)
                .map(req -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", req.id);
                    response.put("status", req.status.name());
                    response.put("deviceName", req.deviceName);
                    if (req.status == KioskPairingRequestService.Status.APPROVED && req.token != null) {
                        response.put("token", req.token);
                        response.put("username", kioskUsername);
                    }
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Admin: list pending pairing requests for the approval UI.
     */
    @GetMapping("/kiosk-pair-requests")
    public ResponseEntity<List<Map<String, Object>>> listPendingKioskPairRequests() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (KioskPairingRequestService.PairingRequest r : kioskPairingRequestService.listPending()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.id);
            m.put("deviceName", r.deviceName);
            m.put("userAgent", r.userAgent);
            m.put("createdAt", r.createdAt.toString());
            m.put("expiresAt", r.expiresAt.toString());
            out.add(m);
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Admin: approve a pending pairing request — mints the kiosk JWT and attaches
     * it to the request so the kiosk's next poll picks it up.
     */
    @PostMapping("/kiosk-pair-requests/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveKioskPairRequest(@PathVariable("id") String id) {
        String approver = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";

        // Find the request first so we can mint with its deviceName
        KioskPairingRequestService.PairingRequest existing = kioskPairingRequestService.get(id).orElse(null);
        if (existing == null || existing.status != KioskPairingRequestService.Status.PENDING) {
            throw new BadRequestException("Pairing request is not pending");
        }

        Map<String, Object> tokenPayload = issueKioskToken(existing.deviceName, approver, existing.userAgent);
        String token = (String) tokenPayload.get("token");

        return kioskPairingRequestService.approve(id, token, approver)
                .map(req -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", req.id);
                    response.put("status", req.status.name());
                    response.put("approvedBy", approver);
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new BadRequestException("Pairing request could not be approved"));
    }

    /**
     * Admin: deny a pending pairing request.
     */
    @PostMapping("/kiosk-pair-requests/{id}/deny")
    public ResponseEntity<Map<String, Object>> denyKioskPairRequest(@PathVariable("id") String id) {
        String denier = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";

        return kioskPairingRequestService.deny(id, denier)
                .map(req -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", req.id);
                    response.put("status", req.status.name());
                    response.put("deniedBy", denier);
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> new BadRequestException("Pairing request could not be denied"));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "unknown";
    }

    /**
     * Ensures the kiosk-system service-account Player exists and mints a long-lived JWT
     * for it. Shared by the direct /kiosk-token endpoint and the pairing-code redemption.
     * Also registers the device in the KioskDeviceService so admins can see it.
     */
    private Map<String, Object> issueKioskToken(String deviceName, String approvedBy, String userAgent) {
        Player kioskPlayer = playerRepository.findByUsername(kioskUsername);
        if (kioskPlayer == null) {
            log.info("Creating kiosk service-account player: {}", kioskUsername);
            kioskPlayer = Player.builder()
                                .firstName("Kiosk")
                                .lastName("Device")
                                .username(kioskUsername)
                                .email("kiosk+" + UUID.randomUUID() + "@kiosk.local")
                                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .role("USER")
                                .isAnonymous(false)
                                .emailVerified(true)
                                .build();
            kioskPlayer.initializeStyleRatings();
            kioskPlayer = playerRepository.save(kioskPlayer);
        }

        String jti = UUID.randomUUID().toString();
        String token = tokenProvider.generateLongLivedToken(kioskPlayer.getUsername(), jti);
        Instant expiresAt = Instant.now().plus(tokenProvider.getKioskExpirationMillis(), ChronoUnit.MILLIS);

        kioskDeviceService.register(jti, deviceName, approvedBy, userAgent);

        log.info("Minted kiosk token for device '{}' (username={}) expiring at {}",
                deviceName, kioskPlayer.getUsername(), expiresAt);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", kioskPlayer.getUsername());
        response.put("expiresAt", expiresAt.toString());
        response.put("deviceName", deviceName);
        return response;
    }
}