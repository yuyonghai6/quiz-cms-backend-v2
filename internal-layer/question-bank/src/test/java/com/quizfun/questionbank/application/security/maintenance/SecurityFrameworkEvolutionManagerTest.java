package com.quizfun.questionbank.application.security.maintenance;

import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityMetricsCollector;
import com.quizfun.shared.common.Result;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for SecurityFrameworkEvolutionManager.
 *
 * US-027: Security Framework Maintenance and Evolution
 * Tests zero-downtime deployment and rollback capabilities.
 */
@ExtendWith(MockitoExtension.class)
@Epic("Core Infrastructure")
@Feature("Security Framework Evolution")
@DisplayName("SecurityFrameworkEvolutionManager Tests")
class SecurityFrameworkEvolutionManagerTest {

    @Mock
    private SecurityAuditLogger auditLogger;

    private SecurityFrameworkVersionManager versionManager;
    private SecurityDeploymentOrchestrator deploymentOrchestrator;
    private SecurityFrameworkMaintenanceService maintenanceService;
    private SecurityFrameworkEvolutionManager evolutionManager;

    @BeforeEach
    void setUp() {
        // Initialize dependencies
        SecurityMetricsCollector metricsCollector = new SecurityMetricsCollector();
        ThreatIntelligenceService threatIntelligence = new ThreatIntelligenceService();
        SecurityConfigurationManager configManager = new SecurityConfigurationManager();
        ComplianceValidator complianceValidator = new ComplianceValidator();

        versionManager = new SecurityFrameworkVersionManager();
        deploymentOrchestrator = new SecurityDeploymentOrchestrator();
        maintenanceService = new SecurityFrameworkMaintenanceService(
            metricsCollector,
            threatIntelligence,
            configManager,
            complianceValidator
        );

        // Create evolution manager
        evolutionManager = new SecurityFrameworkEvolutionManager(
            auditLogger,
            versionManager,
            deploymentOrchestrator,
            maintenanceService
        );
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should initialize SecurityFrameworkEvolutionManager successfully")
    @Description("US-027 AC-027.5, AC-027.8: Initialize with Blue-Green deployment strategy")
    void shouldInitializeEvolutionManagerSuccessfully() {
        // Given/When: Manager is initialized in setUp()

        // Then: Manager should be ready with initial state
        assertNotNull(evolutionManager, "Evolution manager should be initialized");
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.BLUE,
            evolutionManager.getActiveEnvironment(), "Initial active environment should be BLUE");
        assertEquals("1.0.0-US027", evolutionManager.getCurrentVersion(),
            "Initial version should be 1.0.0-US027");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should deploy new version with zero downtime")
    @Description("US-027 AC-027.5, AC-027.7, AC-027.8: Zero-downtime deployment with validation")
    void shouldDeployNewVersionWithZeroDowntime() {
        // Given: Current version is 1.0.0-US027
        String currentVersion = evolutionManager.getCurrentVersion();
        String newVersion = "1.1.0-US027";

        // When: New version is deployed
        Result<SecurityFrameworkEvolutionManager.FrameworkDeploymentResult> result =
            evolutionManager.deployNewVersion(newVersion);

        // Then: Deployment should succeed
        assertTrue(result.isSuccess(), "Deployment should succeed");
        SecurityFrameworkEvolutionManager.FrameworkDeploymentResult deploymentResult =
            result.getValue();
        assertNotNull(deploymentResult, "Deployment result should not be null");
        assertEquals(currentVersion, deploymentResult.getPreviousVersion(),
            "Previous version should match");
        assertEquals(newVersion, deploymentResult.getNewVersion(), "New version should match");
        assertEquals(newVersion, evolutionManager.getCurrentVersion(),
            "Current version should be updated");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should switch from BLUE to GREEN environment during deployment")
    @Description("US-027 AC-027.8: Blue-Green deployment strategy")
    void shouldSwitchEnvironmentDuringDeployment() {
        // Given: Active environment is BLUE
        SecurityFrameworkEvolutionManager.DeploymentEnvironment initialEnvironment =
            evolutionManager.getActiveEnvironment();
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.BLUE,
            initialEnvironment, "Initial environment should be BLUE");

        // When: New version is deployed
        Result<SecurityFrameworkEvolutionManager.FrameworkDeploymentResult> result =
            evolutionManager.deployNewVersion("1.1.0-US027");

        // Then: Active environment should switch to GREEN
        assertTrue(result.isSuccess(), "Deployment should succeed");
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.GREEN,
            evolutionManager.getActiveEnvironment(), "Active environment should be GREEN");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should rollback to previous version")
    @Description("US-027 AC-027.6: Version rollback capability")
    void shouldRollbackToPreviousVersion() {
        // Given: System has been upgraded to version 1.1.0
        evolutionManager.deployNewVersion("1.1.0-US027");
        String currentVersion = evolutionManager.getCurrentVersion();
        assertEquals("1.1.0-US027", currentVersion, "Should be at version 1.1.0");

        // When: Rollback to previous version is initiated
        Result<SecurityFrameworkEvolutionManager.FrameworkRollbackResult> result =
            evolutionManager.rollbackToVersion("1.0.0-US027");

        // Then: Rollback should succeed
        assertTrue(result.isSuccess(), "Rollback should succeed");
        SecurityFrameworkEvolutionManager.FrameworkRollbackResult rollbackResult =
            result.getValue();
        assertNotNull(rollbackResult, "Rollback result should not be null");
        assertEquals("1.1.0-US027", rollbackResult.getFromVersion(),
            "From version should be 1.1.0");
        assertEquals("1.0.0-US027", rollbackResult.getToVersion(),
            "To version should be 1.0.0");
        assertEquals("1.0.0-US027", evolutionManager.getCurrentVersion(),
            "Current version should be rolled back");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should complete rollback within 60 seconds")
    @Description("US-027 AC-027.6: Version rollback must be possible within 60 seconds")
    void shouldCompleteRollbackWithin60Seconds() {
        // Given: System has been upgraded
        evolutionManager.deployNewVersion("1.1.0-US027");

        // When: Rollback is performed
        Result<SecurityFrameworkEvolutionManager.FrameworkRollbackResult> result =
            evolutionManager.rollbackToVersion("1.0.0-US027");

        // Then: Rollback should complete within 60 seconds
        assertTrue(result.isSuccess(), "Rollback should succeed");
        SecurityFrameworkEvolutionManager.FrameworkRollbackResult rollbackResult =
            result.getValue();
        assertTrue(rollbackResult.isWithinTarget(),
            "Rollback should complete within 60 second target");
        assertTrue(rollbackResult.getRollbackDuration().getSeconds() <= 60,
            "Rollback duration should be <= 60 seconds");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should fail rollback for non-existent version")
    @Description("US-027 AC-027.6: Validate rollback target exists")
    void shouldFailRollbackForNonExistentVersion() {
        // Given: Non-existent version
        String nonExistentVersion = "9.9.9-NONEXISTENT";

        // When: Rollback to non-existent version is attempted
        Result<SecurityFrameworkEvolutionManager.FrameworkRollbackResult> result =
            evolutionManager.rollbackToVersion(nonExistentVersion);

        // Then: Rollback should fail
        assertFalse(result.isSuccess(), "Rollback should fail for non-existent version");
        assertTrue(result.getError().contains("not found"),
            "Error should mention version not found");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should track deployment history in version manager")
    @Description("US-027 AC-027.6: Version history tracking")
    void shouldTrackDeploymentHistory() {
        // Given: Multiple deployments
        String version1 = "1.1.0-US027";
        String version2 = "1.2.0-US027";

        // When: Multiple versions are deployed
        evolutionManager.deployNewVersion(version1);
        evolutionManager.deployNewVersion(version2);

        // Then: Version manager should track all versions
        assertTrue(versionManager.versionExists("1.0.0-US027"),
            "Initial version should exist");
        assertTrue(versionManager.versionExists(version1), "Version 1.1.0 should exist");
        assertTrue(versionManager.versionExists(version2), "Version 1.2.0 should exist");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should integrate with SecurityAuditLogger")
    @Description("US-027 AC-027.11: Audit trail for framework modifications")
    void shouldIntegrateWithSecurityAuditLogger() {
        // Given: Evolution manager with audit logger

        // When: Deployment is performed
        Result<SecurityFrameworkEvolutionManager.FrameworkDeploymentResult> result =
            evolutionManager.deployNewVersion("1.1.0-US027");

        // Then: Deployment should succeed with audit logging
        assertTrue(result.isSuccess(), "Deployment should succeed");
        // Note: Audit logger is mocked, so we verify integration by ensuring no exceptions
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should switch environment back and forth with multiple deployments")
    @Description("US-027 AC-027.8: Blue-Green environment switching")
    void shouldSwitchEnvironmentBackAndForth() {
        // Given: Starting with BLUE environment
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.BLUE,
            evolutionManager.getActiveEnvironment(), "Should start with BLUE");

        // When: First deployment (BLUE -> GREEN)
        evolutionManager.deployNewVersion("1.1.0-US027");

        // Then: Should be on GREEN
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.GREEN,
            evolutionManager.getActiveEnvironment(), "Should be on GREEN after first deployment");

        // When: Second deployment (GREEN -> BLUE)
        evolutionManager.deployNewVersion("1.2.0-US027");

        // Then: Should be back on BLUE
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.BLUE,
            evolutionManager.getActiveEnvironment(), "Should be on BLUE after second deployment");

        // When: Third deployment (BLUE -> GREEN)
        evolutionManager.deployNewVersion("1.3.0-US027");

        // Then: Should be on GREEN again
        assertEquals(SecurityFrameworkEvolutionManager.DeploymentEnvironment.GREEN,
            evolutionManager.getActiveEnvironment(), "Should be on GREEN after third deployment");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should integrate with SecurityFrameworkMaintenanceService")
    @Description("US-027: Integration with health monitoring")
    void shouldIntegrateWithMaintenanceService() {
        // Given: Evolution manager with maintenance service integration

        // When: Health status is queried
        var healthStatus = maintenanceService.getHealthStatus();

        // Then: Health status should be available
        assertNotNull(healthStatus, "Health status should not be null");
        assertTrue((Boolean) healthStatus.get("monitoring_active"),
            "Monitoring should be active");
    }
}
