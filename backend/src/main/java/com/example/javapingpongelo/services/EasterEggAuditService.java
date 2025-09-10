package com.example.javapingpongelo.services;

import com.example.javapingpongelo.events.EasterEggFoundEvent;
import com.example.javapingpongelo.models.EasterEgg;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.repositories.EasterEggFindRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for auditing and logging easter egg related events.
 * Provides security monitoring and business intelligence tracking.
 */
@Service
@Slf4j
public class EasterEggAuditService {

    @Autowired
    private EasterEggFindRepository easterEggFindRepository;

    private static final String AUDIT_LOG_PATTERN = 
        "EASTER_EGG_AUDIT | {} | Player: {} ({}) | Action: {} | EggId: {} | EggType: {} | Points: {} | Page: {} | IP: {} | UserAgent: {} | Details: {}";

    /**
     * Log when an easter egg is successfully found and claimed
     */
    @EventListener
    public void onEasterEggFound(EasterEggFoundEvent event) {
        try {
            Player player = event.getPlayer();
            EasterEgg egg = event.getEasterEgg();
            
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("totalEggsFound", event.getTotalEggsFound());
            auditDetails.put("totalPointsEarned", event.getTotalPointsEarned());
            auditDetails.put("eggSpawnedAt", egg.getSpawnedAt());
            auditDetails.put("timesToFind", System.currentTimeMillis() - egg.getSpawnedAt().getTime());
            
            logAuditEvent(
                "EGG_FOUND",
                player.getPlayerId(),
                player.getFullName(),
                egg.getId(),
                egg.getType().toString(),
                event.getPointsEarned(),
                egg.getPageLocation(),
                null, // IP would come from request context
                null, // User agent would come from request context
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit easter egg found event", e);
        }
    }

    /**
     * Log when an easter egg is spawned
     */
    public void logEggSpawned(EasterEgg egg) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("cssSelector", egg.getCssSelector());
            auditDetails.put("coordinates", egg.getCoordinates());
            auditDetails.put("secretMessage", egg.getSecretMessage());
            
            logAuditEvent(
                "EGG_SPAWNED",
                null, // No specific player for spawning
                "SYSTEM",
                egg.getId(),
                egg.getType().toString(),
                egg.getPointValue(),
                egg.getPageLocation(),
                null,
                null,
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit easter egg spawned event", e);
        }
    }

    /**
     * Log when someone attempts to claim an egg but fails
     */
    public void logFailedClaim(UUID eggId, UUID playerId, String playerName, String reason, String clientInfo) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("failureReason", reason);
            auditDetails.put("attemptTime", LocalDateTime.now().toString());
            
            logAuditEvent(
                "EGG_CLAIM_FAILED",
                playerId,
                playerName,
                eggId,
                "UNKNOWN",
                0,
                "UNKNOWN",
                extractIP(clientInfo),
                extractUserAgent(clientInfo),
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit failed egg claim", e);
        }
    }

    /**
     * Log when eggs are cleaned up/expired
     */
    public void logEggCleanup(int eggCount, String reason) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("cleanupReason", reason);
            auditDetails.put("eggsCleaned", eggCount);
            
            logAuditEvent(
                "EGG_CLEANUP",
                null,
                "SYSTEM",
                null,
                "BATCH",
                0,
                "ALL",
                null,
                null,
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit egg cleanup", e);
        }
    }

    /**
     * Log suspicious activity (e.g., rapid claiming, pattern detection)
     */
    public void logSuspiciousActivity(UUID playerId, String playerName, String activityType, String description) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("activityType", activityType);
            auditDetails.put("description", description);
            auditDetails.put("severity", "MEDIUM");
            auditDetails.put("requiresReview", true);
            
            logAuditEvent(
                "SUSPICIOUS_ACTIVITY",
                playerId,
                playerName,
                null,
                "SECURITY",
                0,
                "SYSTEM",
                null,
                null,
                auditDetails
            );
            
            // Also log as WARNING level for immediate attention
            log.warn("SECURITY_ALERT: Suspicious easter egg activity detected - Player: {} ({}), Type: {}, Details: {}", 
                playerName, playerId, activityType, description);
            
        } catch (Exception e) {
            log.error("Failed to audit suspicious activity", e);
        }
    }

    /**
     * Log when leaderboard is accessed (for privacy compliance)
     */
    public void logLeaderboardAccess(UUID playerId, String playerName, String leaderboardType, int entriesReturned) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("leaderboardType", leaderboardType);
            auditDetails.put("entriesReturned", entriesReturned);
            auditDetails.put("accessTime", LocalDateTime.now().toString());
            
            logAuditEvent(
                "LEADERBOARD_ACCESS",
                playerId,
                playerName,
                null,
                "DATA_ACCESS",
                0,
                "leaderboard",
                null,
                null,
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit leaderboard access", e);
        }
    }

    /**
     * Log administrative actions on easter eggs
     */
    public void logAdminAction(String adminUser, String action, UUID targetEggId, String details) {
        try {
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("adminAction", action);
            auditDetails.put("actionDetails", details);
            auditDetails.put("adminUser", adminUser);
            
            logAuditEvent(
                "ADMIN_ACTION",
                null,
                adminUser,
                targetEggId,
                "ADMIN",
                0,
                "admin",
                null,
                null,
                auditDetails
            );
            
        } catch (Exception e) {
            log.error("Failed to audit admin action", e);
        }
    }

    /**
     * Central audit logging method
     */
    private void logAuditEvent(
            String action,
            UUID playerId,
            String playerName,
            UUID eggId,
            String eggType,
            int points,
            String page,
            String clientIP,
            String userAgent,
            Map<String, Object> details) {
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String playerInfo = playerId != null ? playerId.toString() : "SYSTEM";
        String eggInfo = eggId != null ? eggId.toString() : "N/A";
        String detailsJson = details != null ? details.toString() : "{}";
        
        // Log to dedicated audit logger
        log.info(AUDIT_LOG_PATTERN,
            timestamp,
            playerInfo,
            playerName != null ? playerName : "SYSTEM",
            action,
            eggInfo,
            eggType,
            points,
            page != null ? page : "N/A",
            clientIP != null ? clientIP : "N/A",
            userAgent != null ? userAgent.substring(0, Math.min(100, userAgent.length())) : "N/A",
            detailsJson
        );
    }

    /**
     * Extract IP address from client info string or request context
     */
    private String extractIP(String clientInfo) {
        if (clientInfo == null) return null;
        
        // Try to extract IP from common patterns in client info
        if (clientInfo.contains("X-Forwarded-For:")) {
            String[] parts = clientInfo.split("X-Forwarded-For:");
            if (parts.length > 1) {
                return parts[1].split(",")[0].trim();
            }
        }
        
        if (clientInfo.contains("Remote-Addr:")) {
            String[] parts = clientInfo.split("Remote-Addr:");
            if (parts.length > 1) {
                return parts[1].split(",")[0].trim();
            }
        }
        
        // Return truncated client info as fallback
        return clientInfo.length() > 45 ? clientInfo.substring(0, 45) + "..." : clientInfo;
    }

    /**
     * Extract user agent from client info string
     */
    private String extractUserAgent(String clientInfo) {
        if (clientInfo == null) return null;
        
        // Try to extract User-Agent from client info
        if (clientInfo.contains("User-Agent:")) {
            String[] parts = clientInfo.split("User-Agent:");
            if (parts.length > 1) {
                String userAgent = parts[1].split("\n")[0].trim();
                // Truncate long user agents for security
                return userAgent.length() > 200 ? userAgent.substring(0, 200) + "..." : userAgent;
            }
        }
        
        // Return sanitized client info as fallback
        String sanitized = clientInfo.replaceAll("[<>\"'&]", "_");
        return sanitized.length() > 100 ? sanitized.substring(0, 100) + "..." : sanitized;
    }

    /**
     * Generate audit report for a time period (for compliance)
     * Note: This provides a summary based on database records, not log parsing
     */
    public Map<String, Object> generateAuditReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Report metadata
            report.put("reportPeriod", startTime + " to " + endTime);
            report.put("generatedAt", LocalDateTime.now().toString());
            
            // Get time-based statistics from database
            Date startDate = java.sql.Timestamp.valueOf(startTime);
            Date endDate = java.sql.Timestamp.valueOf(endTime);
            
            // Count total eggs found in period
            long totalEggsFound = easterEggFindRepository.countByFoundAtBetween(startDate, endDate);
            report.put("totalEggsFound", totalEggsFound);
            
            // Count unique players who found eggs
            long uniquePlayers = easterEggFindRepository.countDistinctPlayersByFoundAtBetween(startDate, endDate);
            report.put("uniquePlayersActive", uniquePlayers);
            
            // Count eggs by type in this period  
            Map<String, Long> eggsByType = new HashMap<>();
            for (EasterEgg.EggType type : EasterEgg.EggType.values()) {
                long count = easterEggFindRepository.countByEggTypeAndFoundAtBetween(type, startDate, endDate);
                eggsByType.put(type.name(), count);
            }
            report.put("eggsByType", eggsByType);
            
            // Get most active pages
            List<Object[]> pageActivity = easterEggFindRepository.findMostActivePagesByPeriod(startDate, endDate);
            Map<String, Long> pageStats = new HashMap<>();
            for (Object[] row : pageActivity) {
                pageStats.put((String) row[0], (Long) row[1]);
            }
            report.put("mostActivePages", pageStats);
            
            // Calculate averages
            if (uniquePlayers > 0) {
                report.put("averageEggsPerPlayer", (double) totalEggsFound / uniquePlayers);
            } else {
                report.put("averageEggsPerPlayer", 0.0);
            }
            
            // Security note - suspicious activities would need separate tracking
            report.put("note", "Suspicious activity detection requires log analysis beyond this database report");
            
        } catch (Exception e) {
            log.error("Failed to generate audit report for period {} to {}", startTime, endTime, e);
            report.put("error", "Failed to generate report: " + e.getMessage());
        }
        
        return report;
    }
}