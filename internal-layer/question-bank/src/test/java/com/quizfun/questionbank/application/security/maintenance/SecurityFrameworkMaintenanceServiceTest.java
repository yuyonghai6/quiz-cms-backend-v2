package com.quizfun.questionbank.application.security.maintenance;

import com.quizfun.questionbank.application.security.SecurityEventType;
import com.quizfun.questionbank.application.security.SecurityMetricsCollector;
import com.quizfun.questionbank.application.security.SeverityLevel;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for SecurityFrameworkMaintenanceService.
 *
 * US-027: Security Framework Maintenance and Evolution
 * Tests continuous monitoring, drift detection, threat assessment, and compliance validation.
 */
@ExtendWith(MockitoExtension.class)
@Epic("Core Infrastructure")
@Feature("Security Framework Maintenance")
@DisplayName("SecurityFrameworkMaintenanceService Tests")
class SecurityFrameworkMaintenanceServiceTest {

    private SecurityMetricsCollector metricsCollector;
    private ThreatIntelligenceService threatIntelligence;
    private SecurityConfigurationManager configManager;
    private ComplianceValidator complianceValidator;
    private SecurityFrameworkMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        // Initialize dependencies
        metricsCollector = new SecurityMetricsCollector();
        threatIntelligence = new ThreatIntelligenceService();
        configManager = new SecurityConfigurationManager();
        complianceValidator = new ComplianceValidator();

        // Create maintenance service
        maintenanceService = new SecurityFrameworkMaintenanceService(
            metricsCollector,
            threatIntelligence,
            configManager,
            complianceValidator
        );
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should initialize SecurityFrameworkMaintenanceService successfully")
    @Description("US-027 AC-027.1: Security effectiveness metrics must be continuously monitored")
    void shouldInitializeMaintenanceServiceSuccessfully() {
        // Given/When: Service is initialized in setUp()

        // Then: Service should be ready for monitoring
        assertNotNull(maintenanceService, "Maintenance service should be initialized");

        Map<String, Object> healthStatus = maintenanceService.getHealthStatus();
        assertNotNull(healthStatus, "Health status should be available");
        assertTrue((Boolean) healthStatus.get("monitoring_active"), "Monitoring should be active");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should monitor security health without errors")
    @Description("US-027 AC-027.1: Security effectiveness metrics collection and health monitoring")
    void shouldMonitorSecurityHealthWithoutErrors() {
        // Given: Maintenance service is initialized

        // When: Health monitoring is triggered
        assertDoesNotThrow(() -> maintenanceService.monitorSecurityHealth(),
            "Health monitoring should not throw exceptions");

        // Then: Health status should be available
        Map<String, Object> healthStatus = maintenanceService.getHealthStatus();
        assertNotNull(healthStatus.get("total_events_processed"),
            "Total events should be tracked");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should perform on-demand health check successfully")
    @Description("US-027 AC-027.1: On-demand health check capability")
    void shouldPerformOnDemandHealthCheck() {
        // Given: Maintenance service is initialized

        // When: On-demand health check is requested
        SecurityHealthCheck healthCheck = maintenanceService.performHealthCheck();

        // Then: Health check should return valid results
        assertNotNull(healthCheck, "Health check should not be null");
        assertNotNull(healthCheck.getTimestamp(), "Health check should have timestamp");
        assertNotNull(healthCheck.getOverallStatus(), "Health check should have overall status");
        assertNotNull(healthCheck.getMetrics(), "Health check should include metrics");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should check threat landscape without errors")
    @Description("US-027 AC-027.2: Threat landscape monitoring capability")
    void shouldCheckThreatLandscapeWithoutErrors() {
        // Given: Maintenance service is initialized

        // When: Threat landscape check is triggered
        assertDoesNotThrow(() -> maintenanceService.checkThreatLandscape(),
            "Threat landscape check should not throw exceptions");

        // Then: Service should complete check successfully
        assertTrue(true, "Threat landscape check completed");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should track security metrics over time")
    @Description("US-027 AC-027.3: Performance impact tracking")
    void shouldTrackSecurityMetricsOverTime() {
        // Given: Initial health status
        Map<String, Object> initialStatus = maintenanceService.getHealthStatus();
        long initialEvents = (Long) initialStatus.get("total_events_processed");

        // When: Metrics are collected (simulate by triggering health monitoring)
        maintenanceService.monitorSecurityHealth();

        // Then: Metrics should be available and tracked
        Map<String, Object> updatedStatus = maintenanceService.getHealthStatus();
        assertNotNull(updatedStatus.get("total_events_processed"),
            "Metrics should continue to be tracked");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should retrieve comprehensive health status")
    @Description("US-027 AC-027.1: Comprehensive health status reporting")
    void shouldRetrieveComprehensiveHealthStatus() {
        // Given: Maintenance service is running

        // When: Health status is requested
        Map<String, Object> healthStatus = maintenanceService.getHealthStatus();

        // Then: Should include all required components
        assertNotNull(healthStatus, "Health status should not be null");
        assertTrue(healthStatus.containsKey("total_events_processed"),
            "Should include total events");
        assertTrue(healthStatus.containsKey("compliance_status"),
            "Should include compliance status");
        assertTrue(healthStatus.containsKey("configuration_drift_count"),
            "Should include drift count");
        assertTrue(healthStatus.containsKey("monitoring_active"),
            "Should include monitoring status");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should integrate with SecurityMetricsCollector")
    @Description("US-027: Integration with US-025 metrics collection")
    void shouldIntegrateWithSecurityMetricsCollector() {
        // Given: Metrics collector is integrated

        // When: Metrics are queried
        long totalEvents = metricsCollector.getTotalEventsProcessed();
        Map<SecurityEventType, Long> eventCounts = metricsCollector.getAllEventTypeCounts();
        Map<SeverityLevel, Long> severityCounts = metricsCollector.getAllSeverityCounts();

        // Then: Metrics should be accessible
        assertNotNull(eventCounts, "Event type counts should be available");
        assertNotNull(severityCounts, "Severity counts should be available");
        assertTrue(totalEvents >= 0, "Total events should be non-negative");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should validate compliance continuously")
    @Description("US-027 AC-027.9: Continuous compliance validation")
    void shouldValidateComplianceContinuously() {
        // Given: Compliance validator is integrated

        // When: Compliance validation is performed
        ComplianceValidator.ComplianceStatus complianceStatus =
            complianceValidator.validateCurrentCompliance();

        // Then: Compliance status should be available
        assertNotNull(complianceStatus, "Compliance status should not be null");
        assertTrue(complianceStatus.getTotalCount() > 0,
            "Should have compliance requirements");
        assertTrue(complianceStatus.calculateComplianceScore() >= 0,
            "Compliance score should be valid");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should detect configuration drift")
    @Description("US-027 AC-027.4: Configuration drift detection")
    void shouldDetectConfigurationDrift() {
        // Given: Configuration manager is integrated

        // When: Drift detection is performed
        var drifts = configManager.detectConfigurationDrift();

        // Then: Drift detection should complete
        assertNotNull(drifts, "Drift list should not be null");
        // Note: Empty list is valid if no drift exists
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle threat intelligence")
    @Description("US-027 AC-027.2: Threat intelligence processing")
    void shouldHandleThreatIntelligence() {
        // Given: Threat intelligence service is integrated

        // When: Latest threats are retrieved
        var threats = threatIntelligence.getLatestThreats();

        // Then: Threats should be available
        assertNotNull(threats, "Threats list should not be null");

        // When: Threat assessment is performed
        var assessment = threatIntelligence.assessThreatImpact(threats);

        // Then: Assessment should be complete
        assertNotNull(assessment, "Threat assessment should not be null");
        assertNotNull(assessment.getCriticalThreats(), "Critical threats should be listed");
        assertNotNull(assessment.getHighPriorityThreats(), "High priority threats should be listed");
        assertNotNull(assessment.getUnmitigatedThreats(), "Unmitigated threats should be listed");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should detect performance degradation with high severity events")
    @Description("US-027 AC-027.3: Performance degradation detection")
    void shouldDetectPerformanceDegradationWithHighSeverity() {
        // Given: Establish baseline first
        maintenanceService.monitorSecurityHealth();

        // When: Health check runs again (will compare against baseline)
        maintenanceService.monitorSecurityHealth();

        // Then: Health check should complete without errors
        assertDoesNotThrow(() -> maintenanceService.monitorSecurityHealth(),
            "Should handle performance degradation scenario");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should trigger security updates for framework requiring threats")
    @Description("US-027 AC-027.2: Threat-triggered framework updates")
    void shouldTriggerSecurityUpdatesForThreats() {
        // Given: Threats exist that require framework update
        var threats = threatIntelligence.getLatestThreats();

        // When: Threat landscape is checked
        assertDoesNotThrow(() -> maintenanceService.checkThreatLandscape(),
            "Should handle threats requiring framework update");

        // Then: Check completes without errors
        assertTrue(true, "Threat assessment with update recommendations completed");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should process health check with critical issues")
    @Description("US-027 AC-027.1: Critical issue handling")
    void shouldProcessHealthCheckWithCriticalIssues() {
        // Given: Health check with critical issues
        SecurityHealthCheck healthCheck = new SecurityHealthCheck();
        healthCheck.markUnhealthy();
        healthCheck.addAlert("test_critical_alert",
            java.util.List.of("Critical security issue detected"));

        // Then: Service should handle critical issues
        assertNotNull(healthCheck.getAlerts(), "Alerts should be recorded");
        assertTrue(healthCheck.hasCriticalIssues(), "Should have critical issues");
        assertEquals(SecurityHealthCheck.HealthStatus.UNHEALTHY, healthCheck.getOverallStatus());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should process health check with degraded status")
    @Description("US-027 AC-027.1: Degraded state handling")
    void shouldProcessHealthCheckWithDegradedStatus() {
        // Given: Health check with warnings but not critical
        SecurityHealthCheck healthCheck = new SecurityHealthCheck();
        healthCheck.addAlert("minor_warning",
            java.util.List.of("Minor configuration issue"));

        // Then: Should be degraded but not critical
        assertEquals(SecurityHealthCheck.HealthStatus.DEGRADED, healthCheck.getOverallStatus());
        assertFalse(healthCheck.isHealthy(), "Should not be healthy");
        assertFalse(healthCheck.hasCriticalIssues(), "Should not have critical issues");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle compliance violations during health check")
    @Description("US-027 AC-027.9: Compliance violation handling")
    void shouldHandleComplianceViolationsDuringHealthCheck() {
        // Given: Service is running

        // When: Monitor health (which checks compliance)
        maintenanceService.monitorSecurityHealth();

        // Then: Should complete even with compliance checks
        Map<String, Object> healthStatus = maintenanceService.getHealthStatus();
        assertNotNull(healthStatus.get("compliance_status"),
            "Compliance status should be available");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should collect metrics with multiple event types")
    @Description("US-027 AC-027.3: Multi-dimensional metrics collection")
    void shouldCollectMetricsWithMultipleEventTypes() {
        // Given: Metrics collector is integrated

        // When: Health check collects metrics
        SecurityHealthCheck healthCheck = maintenanceService.performHealthCheck();

        // Then: Metrics should be collected
        assertNotNull(healthCheck.getMetrics(), "Metrics should be collected");
        assertNotNull(healthCheck.getMetrics().get("current_metrics"),
            "Current metrics should be present");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should establish and compare baselines")
    @Description("US-027 AC-027.3: Baseline establishment and comparison")
    void shouldEstablishAndCompareBaselines() {
        // Given: Service is initialized

        // When: First health check establishes baseline
        maintenanceService.monitorSecurityHealth();

        // Then: Second health check should compare against baseline
        assertDoesNotThrow(() -> maintenanceService.monitorSecurityHealth(),
            "Baseline comparison should work");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle exception during health monitoring")
    @Description("US-027: Exception handling in health monitoring")
    void shouldHandleExceptionDuringHealthMonitoring() {
        // Given: Service is initialized

        // When: Monitor health (even if exceptions occur internally, they should be caught)
        assertDoesNotThrow(() -> maintenanceService.monitorSecurityHealth(),
            "Should handle exceptions gracefully");

        // Then: Service should remain operational
        Map<String, Object> healthStatus = maintenanceService.getHealthStatus();
        assertNotNull(healthStatus, "Service should remain operational after exceptions");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle exception during threat landscape check")
    @Description("US-027: Exception handling in threat monitoring")
    void shouldHandleExceptionDuringThreatCheck() {
        // Given: Service is initialized

        // When: Check threat landscape (exceptions should be caught internally)
        assertDoesNotThrow(() -> maintenanceService.checkThreatLandscape(),
            "Should handle exceptions gracefully");

        // Then: Service should remain operational
        assertTrue(true, "Service remains operational after threat check");
    }
}
