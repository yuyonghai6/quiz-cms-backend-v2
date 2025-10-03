package com.quizfun.questionbank.application.security;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Test suite for US-025: Security Monitoring Integration.
 *
 * Tests verify:
 * - Event publishing pipeline functionality
 * - Security metrics collection
 * - Real-time alerting for critical events
 * - Integration with US-021 SecurityAuditLogger
 */
@ExtendWith({AllureJunit5.class, MockitoExtension.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityMonitoringIntegrationTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SecurityEventPublisher securityEventPublisher;
    private SecurityMetricsCollector metricsCollector;
    private SecurityAlertService alertService;

    @BeforeEach
    void setUp() {
        securityEventPublisher = new SecurityEventPublisher(eventPublisher);
        metricsCollector = new SecurityMetricsCollector();
        alertService = new SecurityAlertService();

        // Reset metrics before each test
        metricsCollector.resetMetrics();
    }

    @Test
    @Order(1)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should publish security events asynchronously")
    @Description("US-025 AC-025.1, AC-025.2: Events published asynchronously without blocking")
    void shouldPublishSecurityEventsAsynchronously() {
        // Arrange
        var securityEvent = SecurityEvent.builder()
            .type(SecurityEventType.PATH_PARAMETER_MANIPULATION)
            .userId(1001L)
            .severity(SeverityLevel.CRITICAL)
            .sessionId("session_123")
            .clientIp("192.168.1.100")
            .build();

        // Act
        securityEventPublisher.publishEventAsync(securityEvent);

        // Assert - Event should be published to Spring event system
        // Note: In async, we can't verify immediately, but we can verify the method was called
        // In a real scenario, we'd wait or use @Async testing support
        verify(eventPublisher, timeout(1000)).publishEvent(securityEvent);
    }

    @Test
    @Order(2)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should collect metrics from security events")
    @Description("US-025 AC-025.12: Security metrics collected and reported")
    void shouldCollectMetricsFromSecurityEvents() {
        // Arrange
        var event1 = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);
        var event2 = createSecurityEvent(SecurityEventType.SESSION_HIJACKING_ATTEMPT, SeverityLevel.CRITICAL, 1002L);
        var event3 = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);

        // Act
        metricsCollector.collectMetrics(event1);
        metricsCollector.collectMetrics(event2);
        metricsCollector.collectMetrics(event3);

        // Assert - Metrics should be collected
        assertThat(metricsCollector.getTotalEventsProcessed()).isEqualTo(3);
        assertThat(metricsCollector.getEventTypeCount(SecurityEventType.PATH_PARAMETER_MANIPULATION)).isEqualTo(2);
        assertThat(metricsCollector.getEventTypeCount(SecurityEventType.SESSION_HIJACKING_ATTEMPT)).isEqualTo(1);
        assertThat(metricsCollector.getSeverityCount(SeverityLevel.CRITICAL)).isEqualTo(3);
        assertThat(metricsCollector.getUserViolationCount(1001L)).isEqualTo(2);
        assertThat(metricsCollector.getUserViolationCount(1002L)).isEqualTo(1);
    }

    @Test
    @Order(3)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should track event type counts correctly")
    @Description("US-025 AC-025.10: Trend analysis and pattern recognition")
    void shouldTrackEventTypeCountsCorrectly() {
        // Arrange - Create multiple events of different types
        var pathManipulation = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);
        var sessionHijack = createSecurityEvent(SecurityEventType.SESSION_HIJACKING_ATTEMPT, SeverityLevel.CRITICAL, 1002L);
        var privilegeEscalation = createSecurityEvent(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION, SeverityLevel.CRITICAL, 1003L);

        // Act
        metricsCollector.collectMetrics(pathManipulation);
        metricsCollector.collectMetrics(pathManipulation);
        metricsCollector.collectMetrics(sessionHijack);
        metricsCollector.collectMetrics(privilegeEscalation);

        // Assert
        var allEventCounts = metricsCollector.getAllEventTypeCounts();
        assertThat(allEventCounts.get(SecurityEventType.PATH_PARAMETER_MANIPULATION)).isEqualTo(2);
        assertThat(allEventCounts.get(SecurityEventType.SESSION_HIJACKING_ATTEMPT)).isEqualTo(1);
        assertThat(allEventCounts.get(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION)).isEqualTo(1);
    }

    @Test
    @Order(4)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should track severity level counts correctly")
    @Description("US-025 AC-025.12: Security metrics by severity level")
    void shouldTrackSeverityLevelCountsCorrectly() {
        // Arrange - Create events with different severity levels
        var criticalEvent = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);
        var highEvent = createSecurityEvent(SecurityEventType.UNAUTHORIZED_ACCESS_ATTEMPT, SeverityLevel.HIGH, 1002L);
        var mediumEvent = createSecurityEvent(SecurityEventType.RATE_LIMIT_EXCEEDED, SeverityLevel.MEDIUM, 1003L);

        // Act
        metricsCollector.collectMetrics(criticalEvent);
        metricsCollector.collectMetrics(criticalEvent);
        metricsCollector.collectMetrics(highEvent);
        metricsCollector.collectMetrics(mediumEvent);

        // Assert
        var allSeverityCounts = metricsCollector.getAllSeverityCounts();
        assertThat(allSeverityCounts.get(SeverityLevel.CRITICAL)).isEqualTo(2);
        assertThat(allSeverityCounts.get(SeverityLevel.HIGH)).isEqualTo(1);
        assertThat(allSeverityCounts.get(SeverityLevel.MEDIUM)).isEqualTo(1);
        assertThat(allSeverityCounts.get(SeverityLevel.INFO)).isEqualTo(0);
    }

    @Test
    @Order(5)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should track user-specific violation counts for progressive rate limiting")
    @Description("US-025 AC-025.14: Progressive rate limiting support")
    void shouldTrackUserViolationCountsForProgressiveRateLimiting() {
        // Arrange - User 1001 has multiple violations
        var violation1 = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);
        var violation2 = createSecurityEvent(SecurityEventType.SESSION_HIJACKING_ATTEMPT, SeverityLevel.CRITICAL, 1001L);
        var violation3 = createSecurityEvent(SecurityEventType.TOKEN_PRIVILEGE_ESCALATION, SeverityLevel.CRITICAL, 1001L);

        // Act
        metricsCollector.collectMetrics(violation1);
        metricsCollector.collectMetrics(violation2);
        metricsCollector.collectMetrics(violation3);

        // Assert - Should track repeated violations for same user
        assertThat(metricsCollector.getUserViolationCount(1001L)).isEqualTo(3);
    }

    @Test
    @Order(6)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should reset metrics correctly")
    @Description("US-025: Metrics reset functionality for scheduled resets")
    void shouldResetMetricsCorrectly() {
        // Arrange - Collect some metrics
        var event = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);
        metricsCollector.collectMetrics(event);
        metricsCollector.collectMetrics(event);

        assertThat(metricsCollector.getTotalEventsProcessed()).isEqualTo(2);

        // Act - Reset metrics
        metricsCollector.resetMetrics();

        // Assert - All metrics should be zero
        assertThat(metricsCollector.getTotalEventsProcessed()).isEqualTo(0);
        assertThat(metricsCollector.getEventTypeCount(SecurityEventType.PATH_PARAMETER_MANIPULATION)).isEqualTo(0);
        assertThat(metricsCollector.getUserViolationCount(1001L)).isEqualTo(0);
    }

    @Test
    @Order(7)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should complete event publishing within 5 seconds performance requirement")
    @Description("US-025: Event publishing should complete within 5 seconds")
    void shouldMeetPublishingPerformanceRequirements() {
        // Arrange
        var event = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);

        // Act - Measure publishing time (synchronous for testing)
        var startTime = Instant.now();
        securityEventPublisher.publishEventSync(event);
        var duration = Duration.between(startTime, Instant.now());

        // Assert - Should complete quickly (<5 seconds, typically <5ms)
        assertThat(duration.toMillis()).isLessThan(5000);
        verify(eventPublisher).publishEvent(event);
    }

    @Test
    @Order(8)
    @Epic("Security Breach Protection")
    @Story("025.security-monitoring-integration")
    @DisplayName("Should handle event publishing failures gracefully")
    @Description("US-025 AC-025.4: Circuit breaker pattern for resilience")
    void shouldHandlePublishingFailuresGracefully() {
        // Arrange - Mock failure
        doThrow(new RuntimeException("Event bus failure"))
            .when(eventPublisher).publishEvent(any());

        var event = createSecurityEvent(SecurityEventType.PATH_PARAMETER_MANIPULATION, SeverityLevel.CRITICAL, 1001L);

        // Act - Should not throw exception
        Assertions.assertDoesNotThrow(() -> securityEventPublisher.publishEventSync(event));

        // Assert - Failure logged but not propagated
        verify(eventPublisher).publishEvent(event);
    }

    // Helper method
    private SecurityEvent createSecurityEvent(SecurityEventType type, SeverityLevel severity, Long userId) {
        return SecurityEvent.builder()
            .type(type)
            .severity(severity)
            .userId(userId)
            .sessionId("session_" + userId)
            .clientIp("192.168.1." + userId)
            .details(Map.of("testEvent", true))
            .build();
    }
}
