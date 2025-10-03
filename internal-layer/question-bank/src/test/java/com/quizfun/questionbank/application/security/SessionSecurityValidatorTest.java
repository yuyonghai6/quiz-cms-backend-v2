package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * TDD Test suite for SessionSecurityValidator (US-024).
 *
 * Tests follow the RED-GREEN-REFACTOR cycle and verify:
 * - IP address consistency validation
 * - User-Agent string consistency validation
 * - Session hijacking detection and logging
 * - Integration with US-021 SecurityAuditLogger
 */
@ExtendWith({AllureJunit5.class, MockitoExtension.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionSecurityValidatorTest {

    @Mock
    private SecurityAuditLogger auditLogger;

    private SessionSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SessionSecurityValidator(null, auditLogger);
    }

    @Test
    @Order(1)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should pass validation when IP and User-Agent are consistent")
    @Description("US-024 AC-024.1, AC-024.2: Validates that requests with consistent session context pass validation")
    void shouldPassValidationWhenSessionContextIsConsistent() {
        // RED: This test should fail initially

        // Arrange: Create command with session context
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .sessionCreatedAt(Instant.now().minusSeconds(300))
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: Validate with matching IP and User-Agent
        var result = validator.validate(command, sessionContext,
            "192.168.1.100", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // Assert: Should pass validation
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Order(2)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should detect and block IP address mismatch")
    @Description("US-024 AC-024.1: Validates that IP address changes are detected as session hijacking")
    void shouldDetectAndBlockIPAddressMismatch() {
        // RED: This test should fail initially

        // Arrange: Session created from one IP
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: Request comes from different IP
        var result = validator.validate(command, sessionContext,
            "10.0.0.50", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // Assert: Should fail validation
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.SESSION_SECURITY_VIOLATION.name());
        assertThat(result.getError()).contains("Session security violation");
    }

    @Test
    @Order(3)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should detect and block User-Agent mismatch")
    @Description("US-024 AC-024.2: Validates that User-Agent changes are detected as session hijacking")
    void shouldDetectAndBlockUserAgentMismatch() {
        // RED: This test should fail initially

        // Arrange: Session created with specific User-Agent
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: Request comes with different User-Agent
        var result = validator.validate(command, sessionContext,
            "192.168.1.100", "curl/7.64.1");

        // Assert: Should fail validation
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.SESSION_SECURITY_VIOLATION.name());
    }

    @Test
    @Order(4)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should log session hijacking attempt when IP changes")
    @Description("US-024 AC-024.11: Validates that session hijacking attempts are logged via SecurityAuditLogger")
    void shouldLogSessionHijackingAttemptWhenIPChanges() {
        // Arrange
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: IP mismatch detected
        var result = validator.validate(command, sessionContext,
            "10.0.0.50", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // Assert: Should log security event
        assertThat(result.isFailure()).isTrue();

        verify(auditLogger).logSecurityEventAsync(
            argThat(event ->
                event.getType() == SecurityEventType.SESSION_HIJACKING_ATTEMPT &&
                event.getUserId().equals(1001L) &&
                event.getSessionId().equals("session_12345") &&
                event.getSeverity() == SeverityLevel.CRITICAL &&
                event.getDetails().containsKey("violationType") &&
                event.getDetails().get("violationType").equals("IP_MISMATCH") &&
                event.getDetails().get("sessionIp").equals("192.168.1.100") &&
                event.getDetails().get("requestIp").equals("10.0.0.50")
            )
        );
    }

    @Test
    @Order(5)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should log session hijacking attempt when User-Agent changes")
    @Description("US-024 AC-024.11: Validates that User-Agent changes trigger security logging")
    void shouldLogSessionHijackingAttemptWhenUserAgentChanges() {
        // Arrange
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: User-Agent mismatch detected
        var result = validator.validate(command, sessionContext,
            "192.168.1.100", "curl/7.64.1");

        // Assert: Should log security event
        assertThat(result.isFailure()).isTrue();

        verify(auditLogger).logSecurityEventAsync(
            argThat(event ->
                event.getType() == SecurityEventType.SESSION_HIJACKING_ATTEMPT &&
                event.getSeverity() == SeverityLevel.CRITICAL &&
                event.getDetails().get("violationType").equals("USER_AGENT_MISMATCH")
            )
        );
    }

    @Test
    @Order(6)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should handle null or empty session context gracefully")
    @Description("Validates that missing session context is handled appropriately")
    void shouldHandleNullSessionContextGracefully() {
        // Arrange
        var command = createCommand(1001L, 2001L);

        // Act: Validate with null session context
        var result = validator.validate(command, null,
            "192.168.1.100", "Mozilla/5.0");

        // Assert: Should skip validation if no session context
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Order(7)
    @Epic("Security Breach Protection")
    @Story("024.session-hijacking-detection-system")
    @DisplayName("Should complete validation within 30ms performance requirement")
    @Description("US-024: Session security validation time must be <30ms per request")
    void shouldMeetPerformanceRequirements() {
        // Arrange
        var sessionContext = SessionContext.builder()
            .sessionId("session_12345")
            .userId(1001L)
            .clientIp("192.168.1.100")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build();

        var command = createCommand(1001L, 2001L);

        // Act: Measure validation time
        var startTime = System.nanoTime();
        var result = validator.validate(command, sessionContext,
            "192.168.1.100", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        var durationNanos = System.nanoTime() - startTime;
        var durationMillis = durationNanos / 1_000_000;

        // Assert: Should complete within 30ms
        assertThat(result.isSuccess()).isTrue();
        assertThat(durationMillis).isLessThan(30);
    }

    // Helper method
    private UpsertQuestionCommand createCommand(Long userId, Long questionBankId) {
        var requestDto = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("test-question-123")
            .questionType("mcq")
            .title("Test Question")
            .content("Test content")
            .build();

        return new UpsertQuestionCommand(userId, questionBankId, requestDto);
    }
}
