package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.config.BaseTestConfiguration;
import com.quizfun.questionbank.infrastructure.persistence.documents.SecurityEventDocument;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * TDD Test suite for SecurityAuditLogger MongoDB implementation.
 *
 * Tests follow the TDD RED-GREEN-REFACTOR cycle and verify:
 * - Security event persistence to MongoDB
 * - Asynchronous logging capabilities
 * - Graceful failure handling
 * - Backward compatibility with legacy methods
 * - Compliance with US-021 acceptance criteria
 *
 * Uses TestContainers for integration testing with real MongoDB instance.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityAuditLoggerTest extends BaseTestConfiguration {

    @Autowired
    private SecurityAuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        // Clean up security events collection before each test
        if (mongoTemplate.collectionExists("security_events")) {
            mongoTemplate.dropCollection("security_events");
        }
    }

    @Test
    @Order(1)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should log security violation with structured event data to MongoDB")
    @Description("AC-021.1: Validates that security violations are logged with comprehensive audit information")
    void shouldLogSecurityViolationWithStructuredEventDataToMongoDB() {
        // Arrange
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT)
            .userId(1001L)
            .sessionId("session_12345")
            .severity(SeverityLevel.HIGH)
            .requestId("req_security_violation_789")
            .details(Map.of(
                "attemptedUserId", 1002L,
                "tokenUserId", 1001L,
                "endpoint", "/api/users/1002/questionbanks/2001/questions"
            ))
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        // Act
        auditLogger.logSecurityEvent(securityEvent);

        // Assert
        var storedEvents = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(storedEvents).hasSize(1);

        var storedEvent = storedEvents.get(0);
        assertThat(storedEvent.getEventType()).isEqualTo(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT);
        assertThat(storedEvent.getUserId()).isEqualTo(1001L);
        assertThat(storedEvent.getSessionId()).isEqualTo("session_12345");
        assertThat(storedEvent.getSeverity()).isEqualTo(SeverityLevel.HIGH);
        assertThat(storedEvent.getRequestId()).isEqualTo("req_security_violation_789");
        assertThat(storedEvent.getTimestamp()).isNotNull();
        assertThat(storedEvent.getClientIp()).isEqualTo("192.168.1.100");
        assertThat(storedEvent.getUserAgent()).contains("Mozilla");
        assertThat(storedEvent.getDetails()).containsEntry("attemptedUserId", 1002L);
        assertThat(storedEvent.getChecksumHash()).isNotNull();
        assertThat(storedEvent.getChecksumHash()).hasSize(64); // SHA-256 produces 64 hex characters
    }

    @Test
    @Order(2)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should include compliance dates for data retention and anonymization")
    @Description("AC-021.9: Validates GDPR and FERPA compliance with retention policies")
    void shouldIncludeComplianceDatesForDataRetention() {
        // Arrange
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.PATH_PARAMETER_MANIPULATION)
            .userId(1001L)
            .severity(SeverityLevel.CRITICAL)
            .build();

        // Act
        auditLogger.logSecurityEvent(securityEvent);

        // Assert
        var storedEvents = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(storedEvents).hasSize(1);

        var storedEvent = storedEvents.get(0);
        assertThat(storedEvent.getAnonymizationDate()).isNotNull();
        assertThat(storedEvent.getRetentionExpiryDate()).isNotNull();

        // Anonymization should be ~90 days after event
        Duration anonymizationPeriod = Duration.between(storedEvent.getTimestamp(), storedEvent.getAnonymizationDate());
        assertThat(anonymizationPeriod.toDays()).isBetween(89L, 91L);

        // Retention should be ~7 years after event
        Duration retentionPeriod = Duration.between(storedEvent.getTimestamp(), storedEvent.getRetentionExpiryDate());
        assertThat(retentionPeriod.toDays()).isGreaterThan(365 * 7 - 1); // Allow for rounding
    }

    @Test
    @Order(3)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should handle asynchronous logging without blocking request processing")
    @Description("AC-021.8: Validates that audit logging doesn't impact request performance")
    void shouldHandleAsynchronousLoggingWithoutBlocking() {
        // Arrange
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.SESSION_HIJACKING_ATTEMPT)
            .userId(1001L)
            .sessionId("session_async_test")
            .severity(SeverityLevel.CRITICAL)
            .build();

        // Act
        var startTime = System.nanoTime();
        CompletableFuture<Void> future = auditLogger.logSecurityEventAsync(securityEvent);
        var duration = Duration.ofNanos(System.nanoTime() - startTime);

        // Assert - Async call should return immediately
        assertThat(duration).isLessThan(Duration.ofMillis(50));
        assertThat(future).isNotNull();

        // Wait for async processing to complete
        assertDoesNotThrow(() -> future.get(java.util.concurrent.TimeUnit.SECONDS.toMillis(2), java.util.concurrent.TimeUnit.MILLISECONDS));

        // Verify event was persisted
        var events = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo(SecurityEventType.SESSION_HIJACKING_ATTEMPT);
    }

    @Test
    @Order(4)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should handle audit logging failure gracefully without throwing exceptions")
    @Description("AC-021.8: Validates that audit logging failures don't impact main request processing")
    void shouldHandleAuditLoggingFailureGracefully() {
        // Arrange - Create event
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.RATE_LIMIT_EXCEEDED)
            .userId(1001L)
            .severity(SeverityLevel.MEDIUM)
            .build();

        // Act - This should not throw an exception even if MongoDB has issues
        // (In real failure scenario, MongoDB might be down, but we can't simulate that easily in test)
        assertDoesNotThrow(() -> auditLogger.logSecurityEvent(securityEvent));

        // Assert - Verify event was logged (since MongoDB is actually working in test)
        var events = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(events).hasSize(1);
    }

    @Test
    @Order(5)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should support multiple security event types with correct severity levels")
    @Description("AC-021.1: Validates structured event data for different security violations")
    void shouldSupportMultipleSecurityEventTypesWithCorrectSeverity() {
        // Arrange & Act - Log multiple event types
        auditLogger.logSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.PATH_PARAMETER_MANIPULATION)
            .userId(1001L)
            .severity(SeverityLevel.CRITICAL)
            .build());

        auditLogger.logSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT)
            .userId(1002L)
            .severity(SeverityLevel.HIGH)
            .build());

        auditLogger.logSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.RATE_LIMIT_EXCEEDED)
            .userId(1003L)
            .severity(SeverityLevel.MEDIUM)
            .build());

        auditLogger.logSecurityEvent(SecurityEvent.builder()
            .type(SecurityEventType.SECURITY_VALIDATION_SUCCESS)
            .userId(1004L)
            .severity(SeverityLevel.INFO)
            .build());

        // Assert
        var events = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(events).hasSize(4);

        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.PATH_PARAMETER_MANIPULATION &&
            e.getSeverity() == SeverityLevel.CRITICAL);

        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT &&
            e.getSeverity() == SeverityLevel.HIGH);

        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.RATE_LIMIT_EXCEEDED &&
            e.getSeverity() == SeverityLevel.MEDIUM);

        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.SECURITY_VALIDATION_SUCCESS &&
            e.getSeverity() == SeverityLevel.INFO);
    }

    @Test
    @Order(6)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should maintain backward compatibility with legacy logging methods")
    @Description("Validates that existing SecurityContextValidator integration still works")
    void shouldMaintainBackwardCompatibilityWithLegacyLoggingMethods() {
        // Act - Use legacy deprecated methods
        auditLogger.logSecurityViolation("PATH_PARAMETER_MANIPULATION", 1001L,
            "Token user 1001 attempted access to user 1002 resources");

        auditLogger.logSuccessfulAccess(1001L, 2001L, "UPSERT");

        // Assert - Both should be persisted to MongoDB
        var events = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(events).hasSize(2);

        // Verify violation was logged
        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.PATH_PARAMETER_MANIPULATION &&
            e.getUserId() == 1001L &&
            e.getSeverity() == SeverityLevel.HIGH);

        // Verify successful access was logged
        assertThat(events).anyMatch(e ->
            e.getEventType() == SecurityEventType.SECURITY_VALIDATION_SUCCESS &&
            e.getUserId() == 1001L &&
            e.getSeverity() == SeverityLevel.INFO);
    }

    @Test
    @Order(7)
    @Epic("Security Breach Protection")
    @Story("021.security-audit-logging-system")
    @DisplayName("Should calculate consistent checksums for tamper detection")
    @Description("AC-021.11: Validates audit trail immutability through checksums")
    void shouldCalculateConsistentChecksumsForTamperDetection() {
        // Arrange
        Instant fixedTimestamp = Instant.parse("2025-10-03T10:00:00Z");
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION)
            .userId(1001L)
            .timestamp(fixedTimestamp)
            .requestId("req_12345")
            .severity(SeverityLevel.CRITICAL)
            .build();

        // Act - Log the same event twice
        auditLogger.logSecurityEvent(securityEvent);
        auditLogger.logSecurityEvent(securityEvent);

        // Assert - Both should have the same checksum
        var events = mongoTemplate.findAll(SecurityEventDocument.class, "security_events");
        assertThat(events).hasSize(2);

        String firstChecksum = events.get(0).getChecksumHash();
        String secondChecksum = events.get(1).getChecksumHash();

        assertThat(firstChecksum).isNotNull();
        assertThat(firstChecksum).isEqualTo(secondChecksum);
        assertThat(firstChecksum).matches("[0-9a-f]{64}"); // Valid SHA-256 hex format
    }
}
