package com.quizfun.questionbank.application.security.maintenance;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for all data classes and inner classes in US-027.
 * This ensures 100% coverage of data structures.
 */
@Epic("Core Infrastructure")
@Feature("Security Framework Data Classes")
@DisplayName("Security Maintenance Data Classes Tests")
class SecurityMaintenanceDataClassesTest {

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create and use ConfigurationDrift")
    @Description("Test SecurityConfigurationManager.ConfigurationDrift data class")
    void shouldCreateAndUseConfigurationDrift() {
        // Given/When: Create ConfigurationDrift
        Instant detectedAt = Instant.now();
        SecurityConfigurationManager.ConfigurationDrift drift =
            new SecurityConfigurationManager.ConfigurationDrift(
                "security.timeout",
                "expected-value",
                "current-value",
                SecurityConfigurationManager.DriftType.MISMATCH,
                detectedAt
            );

        // Then: All getters should work
        assertEquals("security.timeout", drift.getConfigKey());
        assertEquals(SecurityConfigurationManager.DriftType.MISMATCH, drift.getDriftType());
        assertEquals("expected-value", drift.getExpectedValue());
        assertEquals("current-value", drift.getCurrentValue());
        assertEquals(detectedAt, drift.getDetectedAt());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create and use DriftCorrection with success")
    @Description("Test SecurityConfigurationManager.DriftCorrection success case")
    void shouldCreateAndUseDriftCorrectionSuccess() {
        // Given/When: Create successful DriftCorrection
        Instant detectedAt = Instant.now();
        SecurityConfigurationManager.ConfigurationDrift drift =
            new SecurityConfigurationManager.ConfigurationDrift(
                "security.timeout",
                "expected",
                "current",
                SecurityConfigurationManager.DriftType.MISMATCH,
                detectedAt
            );

        Instant correctedAt = Instant.now();
        SecurityConfigurationManager.DriftCorrection correction =
            new SecurityConfigurationManager.DriftCorrection(
                drift,
                true,
                "Corrected successfully",
                correctedAt
            );

        // Then: All getters should work
        assertEquals(drift, correction.getDrift());
        assertTrue(correction.isSuccess());
        assertEquals("Corrected successfully", correction.getMessage());
        assertEquals(correctedAt, correction.getCorrectedAt());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create and use DriftCorrection with failure")
    @Description("Test SecurityConfigurationManager.DriftCorrection failure case")
    void shouldCreateAndUseDriftCorrectionFailure() {
        // Given/When: Create failed DriftCorrection
        Instant detectedAt = Instant.now();
        SecurityConfigurationManager.ConfigurationDrift drift =
            new SecurityConfigurationManager.ConfigurationDrift(
                "security.feature.x",
                "expected",
                "current",
                SecurityConfigurationManager.DriftType.MISSING,
                detectedAt
            );

        Instant correctedAt = Instant.now();
        SecurityConfigurationManager.DriftCorrection correction =
            new SecurityConfigurationManager.DriftCorrection(
                drift,
                false,
                "Failed to correct: permission denied",
                correctedAt
            );

        // Then: All getters should work
        assertEquals(drift, correction.getDrift());
        assertFalse(correction.isSuccess());
        assertEquals("Failed to correct: permission denied", correction.getMessage());
        assertEquals(correctedAt, correction.getCorrectedAt());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should use all DriftType enum values")
    @Description("Test SecurityConfigurationManager.DriftType enum")
    void shouldUseAllDriftTypeValues() {
        // Given/When/Then: All drift types should be accessible
        assertEquals("MISSING", SecurityConfigurationManager.DriftType.MISSING.toString());
        assertEquals("MISMATCH", SecurityConfigurationManager.DriftType.MISMATCH.toString());
        assertEquals("UNEXPECTED", SecurityConfigurationManager.DriftType.UNEXPECTED.toString());

        // Verify all values
        SecurityConfigurationManager.DriftType[] allTypes = SecurityConfigurationManager.DriftType.values();
        assertEquals(3, allTypes.length);
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create and use SecurityIncidentSummary")
    @Description("Test ComplianceReport.SecurityIncidentSummary data class")
    void shouldCreateAndUseSecurityIncidentSummary() {
        // Given/When: Create SecurityIncidentSummary
        Instant occurredAt = Instant.now();
        ComplianceReport.SecurityIncidentSummary incident =
            new ComplianceReport.SecurityIncidentSummary(
                "INC-001",
                "UNAUTHORIZED_ACCESS",
                "HIGH",
                occurredAt,
                "Access blocked and user notified"
            );

        // Then: All getters should work
        assertEquals("INC-001", incident.getIncidentId());
        assertEquals("UNAUTHORIZED_ACCESS", incident.getIncidentType());
        assertEquals("HIGH", incident.getSeverity());
        assertEquals(occurredAt, incident.getOccurredAt());
        assertEquals("Access blocked and user notified", incident.getResolution());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create and use SecurityAlert")
    @Description("Test SecurityHealthCheck.SecurityAlert data class")
    void shouldCreateAndUseSecurityAlert() {
        // Given/When: Create SecurityAlert
        Instant detectedAt = Instant.now();
        List<String> issues = List.of("Issue 1", "Issue 2");
        SecurityHealthCheck.SecurityAlert alert =
            new SecurityHealthCheck.SecurityAlert("configuration_drift", issues, detectedAt);

        // Then: All getters should work
        assertEquals("configuration_drift", alert.getCategory());
        assertEquals(2, alert.getIssues().size());
        assertEquals("Issue 1", alert.getIssues().get(0));
        assertEquals("Issue 2", alert.getIssues().get(1));
        assertEquals(detectedAt, alert.getDetectedAt());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create ComplianceMetrics with all fields")
    @Description("Test ComplianceReport.ComplianceMetrics data class")
    void shouldCreateComplianceMetrics() {
        // Given/When: Create ComplianceMetrics
        Map<String, Double> byStandard = new HashMap<>();
        byStandard.put("OWASP_ASVS", 95.0);
        byStandard.put("GDPR", 100.0);

        ComplianceReport.ComplianceMetrics metrics =
            new ComplianceReport.ComplianceMetrics(
                97.5,
                10,
                9,
                1,
                byStandard
            );

        // Then: All getters should work
        assertEquals(97.5, metrics.getOverallComplianceScore());
        assertEquals(10, metrics.getTotalRequirements());
        assertEquals(9, metrics.getCompliantRequirements());
        assertEquals(1, metrics.getNonCompliantRequirements());
        assertEquals(2, metrics.getComplianceByStandard().size());
        assertEquals(95.0, metrics.getComplianceByStandard().get("OWASP_ASVS"));
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create FrameworkChange with all fields")
    @Description("Test ComplianceReport.FrameworkChange data class")
    void shouldCreateFrameworkChange() {
        // Given/When: Create FrameworkChange
        Instant implementedAt = Instant.now();
        ComplianceReport.FrameworkChange change =
            new ComplianceReport.FrameworkChange(
                "CHG-001",
                "ENHANCEMENT",
                "Added security monitoring",
                implementedAt,
                "Security Team"
            );

        // Then: All getters should work
        assertEquals("CHG-001", change.getChangeId());
        assertEquals("ENHANCEMENT", change.getChangeType());
        assertEquals("Added security monitoring", change.getDescription());
        assertEquals(implementedAt, change.getImplementedAt());
        assertEquals("Security Team", change.getImplementedBy());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create ReportingPeriod with all fields")
    @Description("Test ComplianceReport.ReportingPeriod data class")
    void shouldCreateReportingPeriod() {
        // Given/When: Create ReportingPeriod
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 7);
        ComplianceReport.ReportingPeriod period =
            new ComplianceReport.ReportingPeriod(
                startDate,
                endDate,
                "Week 1 - October 2025"
            );

        // Then: All getters should work
        assertEquals(startDate, period.getStartDate());
        assertEquals(endDate, period.getEndDate());
        assertEquals("Week 1 - October 2025", period.getPeriodLabel());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should build complete ComplianceReport")
    @Description("Test ComplianceReport.Builder with all fields")
    void shouldBuildCompleteComplianceReport() {
        // Given: All report components
        LocalDate startDate = LocalDate.of(2025, 10, 1);
        LocalDate endDate = LocalDate.of(2025, 10, 7);
        ComplianceReport.ReportingPeriod period =
            new ComplianceReport.ReportingPeriod(startDate, endDate, "Week 1");

        Map<String, Double> byStandard = Map.of("OWASP", 95.0);
        ComplianceReport.ComplianceMetrics metrics =
            new ComplianceReport.ComplianceMetrics(95.0, 10, 9, 1, byStandard);

        List<ComplianceReport.SecurityIncidentSummary> incidents = List.of(
            new ComplianceReport.SecurityIncidentSummary(
                "INC-001", "TEST", "LOW", Instant.now(), "Resolved"
            )
        );

        List<ComplianceReport.FrameworkChange> changes = List.of(
            new ComplianceReport.FrameworkChange(
                "CHG-001", "ENHANCEMENT", "Test", Instant.now(), "Team"
            )
        );

        List<String> recommendations = List.of("Recommendation 1", "Recommendation 2");
        Instant generatedAt = Instant.now();

        // When: Build report
        ComplianceReport report = ComplianceReport.builder()
            .reportId("REPORT-001")
            .reportingPeriod(period)
            .securityFrameworkVersion("1.0.0")
            .complianceMetrics(metrics)
            .securityIncidents(incidents)
            .frameworkChanges(changes)
            .recommendations(recommendations)
            .generatedAt(generatedAt)
            .generatedBy("Test System")
            .build();

        // Then: All fields should be set correctly
        assertEquals("REPORT-001", report.getReportId());
        assertEquals(period, report.getReportingPeriod());
        assertEquals("1.0.0", report.getSecurityFrameworkVersion());
        assertEquals(metrics, report.getComplianceMetrics());
        assertEquals(1, report.getSecurityIncidents().size());
        assertEquals(1, report.getFrameworkChanges().size());
        assertEquals(2, report.getRecommendations().size());
        assertEquals(generatedAt, report.getGeneratedAt());
        assertEquals("Test System", report.getGeneratedBy());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create FrameworkVersion with changelog")
    @Description("Test SecurityFrameworkVersionManager.FrameworkVersion data class")
    void shouldCreateFrameworkVersion() {
        // Given/When: Create FrameworkVersion
        List<String> changelog = List.of("Feature 1", "Bug fix 2");
        Instant deployedAt = Instant.now();

        SecurityFrameworkVersionManager.FrameworkVersion version =
            new SecurityFrameworkVersionManager.FrameworkVersion(
                "1.1.0",
                "1.0.0",
                deployedAt,
                "Deployment System",
                "Major update",
                changelog
            );

        // Then: All getters should work
        assertEquals("1.1.0", version.getVersion());
        assertEquals("1.0.0", version.getPreviousVersion());
        assertEquals(deployedAt, version.getDeployedAt());
        assertEquals("Deployment System", version.getDeployedBy());
        assertEquals("Major update", version.getDescription());
        assertEquals(2, version.getChangelog().size());
        assertEquals("Feature 1", version.getChangelog().get(0));
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should create all SecurityHealthCheck components")
    @Description("Test SecurityHealthCheck with metrics, analysis, compliance, and alerts")
    void shouldCreateCompleteSecurityHealthCheck() {
        // Given: SecurityHealthCheck
        SecurityHealthCheck healthCheck = new SecurityHealthCheck();

        // When: Add various components
        Map<String, Object> metrics = Map.of("cpu", 50, "memory", 75);
        healthCheck.addMetrics("performance", metrics);

        Map<String, Object> analysis = Map.of("trend", "stable");
        healthCheck.addAnalysis("performance_analysis", analysis);

        ComplianceValidator.ComplianceStatus complianceStatus =
            new ComplianceValidator.ComplianceStatus(Instant.now());
        healthCheck.addCompliance("regulatory", complianceStatus);

        List<String> issues = List.of("Issue 1");
        healthCheck.addAlert("test_alert", issues);

        healthCheck.markUnhealthy();

        // Then: All components should be accessible
        assertNotNull(healthCheck.getTimestamp());
        assertEquals(SecurityHealthCheck.HealthStatus.UNHEALTHY, healthCheck.getOverallStatus());
        assertFalse(healthCheck.isHealthy());
        assertTrue(healthCheck.hasCriticalIssues()); // Unhealthy = critical
        assertNotNull(healthCheck.getMetrics());
        assertNotNull(healthCheck.getAnalysis());
        assertNotNull(healthCheck.getCompliance());
        assertNotNull(healthCheck.getAlerts());
        assertEquals(1, healthCheck.getAlerts().size());
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle SecurityHealthCheck degraded state")
    @Description("Test SecurityHealthCheck degraded vs healthy vs unhealthy states")
    void shouldHandleHealthCheckDegradedState() {
        // Given: Three health checks
        SecurityHealthCheck healthy = new SecurityHealthCheck();
        SecurityHealthCheck degraded = new SecurityHealthCheck();
        SecurityHealthCheck unhealthy = new SecurityHealthCheck();

        // When: Set different states
        // healthy remains default (HEALTHY)
        degraded.addAlert("minor_issue", List.of("Minor problem"));
        unhealthy.markUnhealthy();

        // Then: States should be different
        assertEquals(SecurityHealthCheck.HealthStatus.HEALTHY, healthy.getOverallStatus());
        assertTrue(healthy.isHealthy());
        assertFalse(healthy.hasCriticalIssues());

        // Degraded has alerts but not marked unhealthy
        assertEquals(SecurityHealthCheck.HealthStatus.DEGRADED, degraded.getOverallStatus());
        assertFalse(degraded.isHealthy());
        assertFalse(degraded.hasCriticalIssues());

        // Unhealthy is critical
        assertEquals(SecurityHealthCheck.HealthStatus.UNHEALTHY, unhealthy.getOverallStatus());
        assertFalse(unhealthy.isHealthy());
        assertTrue(unhealthy.hasCriticalIssues());
    }
}
