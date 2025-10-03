package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.infrastructure.persistence.documents.SecurityEventDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB-backed implementation of SecurityAuditLogger for comprehensive audit trails.
 *
 * This implementation provides:
 * - Persistent storage of security events in MongoDB
 * - Synchronous and asynchronous logging capabilities
 * - Tamper detection via SHA-256 checksums
 * - Graceful failure handling (audit failures don't impact requests)
 * - Compliance support (GDPR, FERPA) with data retention policies
 *
 * Follows existing US-005 repository patterns for MongoDB integration.
 *
 * @see SecurityEvent
 * @see SecurityEventDocument
 */
@Component
public class SecurityAuditLoggerImpl implements SecurityAuditLogger {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditLoggerImpl.class);
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_AUDIT");

    private final MongoTemplate mongoTemplate;
    private final SecurityEventPublisher eventPublisher;

    public SecurityAuditLoggerImpl(MongoTemplate mongoTemplate,
                                  SecurityEventPublisher eventPublisher) {
        this.mongoTemplate = mongoTemplate;
        this.eventPublisher = eventPublisher;
        logger.info("SecurityAuditLoggerImpl initialized with MongoDB persistence and event publishing");
    }

    /**
     * Legacy method for backward compatibility with existing SecurityContextValidator.
     */
    @Override
    @Deprecated
    public void logSecurityViolation(String eventType, Long tokenUserId, String details) {
        securityLogger.warn(
            "SECURITY_VIOLATION: eventType={}, tokenUserId={}, details={}, timestamp={}",
            eventType,
            tokenUserId,
            details,
            Instant.now()
        );

        // Convert to new SecurityEvent format and persist
        SecurityEvent event = SecurityEvent.builder()
            .type(mapLegacyEventType(eventType))
            .userId(tokenUserId)
            .severity(SeverityLevel.HIGH)
            .details(java.util.Map.of("legacyDetails", details))
            .build();

        logSecurityEvent(event);
    }

    /**
     * Legacy method for backward compatibility with existing SecurityContextValidator.
     */
    @Override
    @Deprecated
    public void logSuccessfulAccess(Long userId, Long questionBankId, String operation) {
        securityLogger.info(
            "SUCCESSFUL_ACCESS: userId={}, questionBankId={}, operation={}, timestamp={}",
            userId,
            questionBankId,
            operation,
            Instant.now()
        );

        // Convert to new SecurityEvent format and persist
        SecurityEvent event = SecurityEvent.builder()
            .type(SecurityEventType.SECURITY_VALIDATION_SUCCESS)
            .userId(userId)
            .severity(SeverityLevel.INFO)
            .details(java.util.Map.of(
                "questionBankId", questionBankId,
                "operation", operation
            ))
            .build();

        logSecurityEvent(event);
    }

    /**
     * Logs security event synchronously to MongoDB.
     * Failures are logged but do not propagate to prevent impacting request processing.
     */
    @Override
    public void logSecurityEvent(SecurityEvent event) {
        try {
            SecurityEventDocument document = SecurityEventDocument.fromSecurityEvent(event);

            // Calculate checksum for tamper detection
            String checksum = calculateChecksum(event);
            document.setChecksumHash(checksum);

            // Persist to MongoDB
            mongoTemplate.save(document, "security_events");

            logger.debug("Security event persisted to MongoDB: type={}, userId={}, severity={}",
                event.getType(), event.getUserId(), event.getSeverity());

            // Log to SLF4J for immediate visibility
            logToSlf4j(event);

            // US-025: Publish event to monitoring and alerting systems
            if (eventPublisher != null) {
                eventPublisher.publishEventAsync(event);
            }

        } catch (DataAccessException ex) {
            logger.error("MongoDB error while logging security event: type={}, userId={}",
                event.getType(), event.getUserId(), ex);
            // Don't propagate error - audit logging should not impact request processing
        } catch (Exception ex) {
            logger.error("Unexpected error while logging security event: type={}, userId={}",
                event.getType(), event.getUserId(), ex);
            // Don't propagate error
        }
    }

    /**
     * Logs security event asynchronously to MongoDB.
     * Returns immediately to avoid impacting request processing performance.
     */
    @Override
    @Async
    public CompletableFuture<Void> logSecurityEventAsync(SecurityEvent event) {
        return CompletableFuture.runAsync(() -> logSecurityEvent(event));
    }

    /**
     * Calculates SHA-256 checksum for audit trail integrity and tamper detection.
     */
    private String calculateChecksum(SecurityEvent event) {
        try {
            String dataToHash = String.format("%s:%s:%s:%s",
                event.getType(),
                event.getUserId(),
                event.getTimestamp(),
                event.getRequestId());

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException ex) {
            logger.error("SHA-256 algorithm not available for checksum calculation", ex);
            return "CHECKSUM_ERROR";
        }
    }

    /**
     * Logs security event to SLF4J for immediate visibility in application logs.
     */
    private void logToSlf4j(SecurityEvent event) {
        String message = String.format("SECURITY_EVENT: type=%s, userId=%s, severity=%s, timestamp=%s, requestId=%s",
            event.getType(), event.getUserId(), event.getSeverity(),
            event.getTimestamp(), event.getRequestId());

        switch (event.getSeverity()) {
            case CRITICAL, HIGH -> securityLogger.warn(message);
            case MEDIUM -> securityLogger.info(message);
            default -> securityLogger.debug(message);
        }
    }

    /**
     * Maps legacy string event types to SecurityEventType enum.
     */
    private SecurityEventType mapLegacyEventType(String eventType) {
        return switch (eventType.toUpperCase()) {
            case "MISSING_AUTHENTICATION" -> SecurityEventType.MISSING_AUTHENTICATION;
            case "INVALID_TOKEN_TYPE" -> SecurityEventType.INVALID_TOKEN_TYPE;
            case "INVALID_TOKEN_SUBJECT" -> SecurityEventType.INVALID_TOKEN_SUBJECT;
            case "PATH_PARAMETER_MANIPULATION" -> SecurityEventType.PATH_PARAMETER_MANIPULATION;
            case "SECURITY_VALIDATION_ERROR" -> SecurityEventType.SECURITY_VALIDATION_ERROR;
            default -> SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT;
        };
    }
}
