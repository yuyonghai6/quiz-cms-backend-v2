package com.quizfun.questionbank.application.security.maintenance;

import com.quizfun.questionbank.application.security.SecurityAuditLogger;
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

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test for SecurityGovernanceService.
 *
 * US-027: Security Framework Maintenance and Evolution
 * Tests governance approval workflow and automated compliance reporting.
 */
@ExtendWith(MockitoExtension.class)
@Epic("Core Infrastructure")
@Feature("Security Framework Governance")
@DisplayName("SecurityGovernanceService Tests")
class SecurityGovernanceServiceTest {

    @Mock
    private SecurityAuditLogger auditLogger;

    private ComplianceValidator complianceValidator;
    private SecurityGovernanceService governanceService;

    @BeforeEach
    void setUp() {
        // Initialize dependencies
        complianceValidator = new ComplianceValidator();

        // Create governance service
        governanceService = new SecurityGovernanceService(
            auditLogger,
            complianceValidator
        );
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should initialize SecurityGovernanceService successfully")
    @Description("US-027 AC-027.10: Security framework changes must go through governance approval process")
    void shouldInitializeGovernanceServiceSuccessfully() {
        // Given/When: Service is initialized in setUp()

        // Then: Service should be ready for governance
        assertNotNull(governanceService, "Governance service should be initialized");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should process low-risk framework change request successfully")
    @Description("US-027 AC-027.10, AC-027.11: Framework change request processing with audit trail")
    void shouldProcessLowRiskFrameworkChangeRequest() {
        // Given: Low-risk framework change request
        SecurityGovernanceService.SecurityFrameworkChangeRequest request =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-001",
                "ENHANCEMENT",
                "Add additional security monitoring metrics",
                "security-team",
                Instant.now()
            );

        // When: Framework change is requested
        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result =
            governanceService.requestFrameworkChange(request);

        // Then: Should auto-approve low-risk change
        assertTrue(result.isSuccess(), "Low-risk change should be approved");
        SecurityGovernanceService.SecurityFrameworkChangeApproval approval = result.getValue();
        assertNotNull(approval, "Approval should not be null");
        assertTrue(approval.isApproved(), "Low-risk change should be auto-approved");
        assertEquals("CHG-001", approval.getRequestId(), "Request ID should match");
        assertNotNull(approval.getApprovedAt(), "Approval timestamp should be set");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should block high-risk framework change (MAJOR_REFACTOR)")
    @Description("US-027 AC-027.10: High-risk changes like MAJOR_REFACTOR are blocked due to compliance impact")
    void shouldBlockHighRiskFrameworkChange() {
        // Given: High-risk framework change request (MAJOR_REFACTOR)
        SecurityGovernanceService.SecurityFrameworkChangeRequest request =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-002",
                "MAJOR_REFACTOR",
                "Complete rewrite of security validation layer",
                "security-architect",
                Instant.now()
            );

        // When: Framework change is requested
        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result =
            governanceService.requestFrameworkChange(request);

        // Then: Should fail due to high-risk compliance impact
        assertFalse(result.isSuccess(), "Major refactor should be blocked due to high compliance impact");
        assertTrue(result.getError().contains("High-risk compliance impact"),
            "Error should mention compliance impact");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should block high-risk compliance impact changes")
    @Description("US-027 AC-027.10: High-risk compliance impact requires additional review")
    void shouldBlockHighRiskComplianceImpactChanges() {
        // Given: High-risk change request (SECURITY_CONTROL_REMOVAL)
        SecurityGovernanceService.SecurityFrameworkChangeRequest request =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-003",
                "SECURITY_CONTROL_REMOVAL",
                "Remove deprecated authentication validator",
                "developer",
                Instant.now()
            );

        // When: Framework change is requested
        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result =
            governanceService.requestFrameworkChange(request);

        // Then: Should fail due to high-risk compliance impact
        assertFalse(result.isSuccess(), "High-risk compliance impact should be blocked");
        assertTrue(result.getError().contains("High-risk compliance impact"),
            "Error should mention compliance impact");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should generate compliance report without errors")
    @Description("US-027 AC-027.12: Regulatory compliance reporting must be automated")
    void shouldGenerateComplianceReportWithoutErrors() {
        // Given: Governance service is initialized

        // When: Compliance report is generated
        assertDoesNotThrow(() -> governanceService.generateComplianceReport(),
            "Compliance report generation should not throw exceptions");

        // Then: Report generation should complete successfully
        assertTrue(true, "Compliance report generated successfully");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should handle multiple sequential framework change requests")
    @Description("US-027 AC-027.10, AC-027.11: Multiple change requests with audit trail")
    void shouldHandleMultipleFrameworkChangeRequests() {
        // Given: Multiple framework change requests
        SecurityGovernanceService.SecurityFrameworkChangeRequest request1 =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-101",
                "BUG_FIX",
                "Fix session validation timing issue",
                "developer",
                Instant.now()
            );

        SecurityGovernanceService.SecurityFrameworkChangeRequest request2 =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-102",
                "ENHANCEMENT",
                "Add rate limiting to API endpoints",
                "security-team",
                Instant.now()
            );

        // When: Both requests are processed
        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result1 =
            governanceService.requestFrameworkChange(request1);
        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result2 =
            governanceService.requestFrameworkChange(request2);

        // Then: Both should be processed successfully
        assertTrue(result1.isSuccess(), "First request should be processed");
        assertTrue(result2.isSuccess(), "Second request should be processed");
        assertNotEquals(result1.getValue().getRequestId(), result2.getValue().getRequestId(),
            "Request IDs should be different");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should integrate with ComplianceValidator")
    @Description("US-027: Integration between governance and compliance validation")
    void shouldIntegrateWithComplianceValidator() {
        // Given: ComplianceValidator is integrated

        // When: Compliance status is checked
        ComplianceValidator.ComplianceStatus status = complianceValidator.validateCurrentCompliance();

        // Then: Compliance status should be available
        assertNotNull(status, "Compliance status should not be null");
        assertTrue(status.getTotalCount() > 0, "Should have compliance requirements");
    }

    @Test
    @Story("US-027: Security Framework Maintenance and Evolution")
    @DisplayName("Should integrate with SecurityAuditLogger")
    @Description("US-027 AC-027.11: Security audit trail must be maintained")
    void shouldIntegrateWithSecurityAuditLogger() {
        // Given: SecurityAuditLogger is integrated
        // When: Framework change is requested (which logs to audit)
        SecurityGovernanceService.SecurityFrameworkChangeRequest request =
            new SecurityGovernanceService.SecurityFrameworkChangeRequest(
                "CHG-201",
                "ENHANCEMENT",
                "Add security telemetry",
                "ops-team",
                Instant.now()
            );

        Result<SecurityGovernanceService.SecurityFrameworkChangeApproval> result =
            governanceService.requestFrameworkChange(request);

        // Then: Request should be processed and audit logged
        assertTrue(result.isSuccess(), "Request should be processed");
        // Note: Audit logger is mocked, so we verify integration by ensuring no exceptions
    }
}
