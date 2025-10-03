package com.quizfun.questionbank.application.security;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.validation.ValidationErrorCode;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import io.qameta.allure.junit5.AllureJunit5;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith({AllureJunit5.class, MockitoExtension.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SecurityContextValidatorTest {

    @Mock
    private SecurityAuditLogger auditLogger;

    @Mock
    private RetryHelper retryHelper;

    @Mock
    private ValidationChainMetrics metrics;

    private SecurityContextValidator securityValidator;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();

        // Create SecurityContextValidator with mocked dependencies
        securityValidator = new SecurityContextValidator(null, auditLogger, retryHelper, metrics);
    }

    @AfterEach
    void tearDown() {
        // Clear security context after each test
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should reject request when JWT token user ID differs from path parameter")
    @Description("Validates that path parameter manipulation attacks are detected and blocked immediately")
    void shouldRejectMismatchedUserIdAttack() {
        // RED: This test should fail initially

        // Arrange: Create malicious command with user ID 1001 in path
        var maliciousCommand = createUpsertQuestionCommand(1001L, 2001L);

        // Mock JWT context with different user ID (1002)
        SecurityContextHolder.getContext().setAuthentication(
            createJwtAuthenticationToken("1002")
        );

        // Act: Validate the command
        var result = securityValidator.validate(maliciousCommand);

        // Assert: Should be rejected with security violation
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.UNAUTHORIZED_ACCESS.name());
        assertThat(result.getError()).contains("Access denied");
    }

    @Test
    @Order(2)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should allow request when JWT token user ID matches path parameter")
    @Description("Validates that legitimate users can access their own resources without security blocks")
    void shouldAllowValidUserAccess() {
        // RED: This test should fail initially

        // Arrange: Create valid command with user ID 1001
        var validCommand = createUpsertQuestionCommand(1001L, 2001L);

        // Mock JWT context with matching user ID (1001)
        SecurityContextHolder.getContext().setAuthentication(
            createJwtAuthenticationToken("1001")
        );

        // Act: Validate the command
        var result = securityValidator.validate(validCommand);

        // Assert: Should be allowed to continue
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Order(3)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should reject request when authentication context is missing")
    @Description("Validates that requests without proper authentication are immediately rejected")
    void shouldRejectMissingAuthenticationContext() {
        // RED: This test should fail initially

        // Arrange: Create command but leave security context empty
        var command = createUpsertQuestionCommand(1001L, 2001L);
        // No authentication set in SecurityContextHolder

        // Act: Validate the command
        var result = securityValidator.validate(command);

        // Assert: Should be rejected due to missing authentication
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN.name());
    }

    @Test
    @Order(4)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should reject request with non-JWT authentication token")
    @Description("Validates that only JWT authentication tokens are accepted for security validation")
    void shouldRejectNonJwtAuthenticationToken() {
        // RED: This test should fail initially

        // Arrange: Create command and set non-JWT authentication
        var command = createUpsertQuestionCommand(1001L, 2001L);

        // Set non-JWT authentication (e.g., basic auth)
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("1001", "password")
        );

        // Act: Validate the command
        var result = securityValidator.validate(command);

        // Assert: Should be rejected due to invalid token type
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.INVALID_AUTHENTICATION_TOKEN.name());
    }

    @Test
    @Order(5)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should complete security validation within 20ms performance requirement")
    @Description("Validates that security validation meets the performance requirement of less than 20ms processing time")
    void shouldMeetPerformanceRequirements() {
        // RED: This test should fail initially

        // Arrange: Create valid command and authentication
        var command = createUpsertQuestionCommand(1001L, 2001L);
        SecurityContextHolder.getContext().setAuthentication(
            createJwtAuthenticationToken("1001")
        );

        // Act: Measure validation time
        var startTime = System.nanoTime();
        var result = securityValidator.validate(command);
        var duration = Duration.ofNanos(System.nanoTime() - startTime);

        // Assert: Should complete successfully within time limit
        assertThat(result.isSuccess()).isTrue();
        assertThat(duration).isLessThan(Duration.ofMillis(20));
    }

    @Test
    @Order(6)
    @Epic("Use Case Security Breach Protection Unhappy Path")
    @Story("story-020.security-context-validator-implementation")
    @DisplayName("Should log comprehensive security violation details")
    @Description("Validates that security violations are logged with complete audit information for compliance")
    void shouldLogSecurityViolationDetails() {
        // Arrange: Create malicious command
        var maliciousCommand = createUpsertQuestionCommand(1001L, 2001L);
        SecurityContextHolder.getContext().setAuthentication(
            createJwtAuthenticationToken("1002")
        );

        // Act: Validate the command (should fail and log violation)
        var result = securityValidator.validate(maliciousCommand);

        // Assert: Security violation should be logged and validation should fail
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorCode()).isEqualTo(ValidationErrorCode.UNAUTHORIZED_ACCESS.name());

        // US-022: Verify that security event was logged using new SecurityEvent model
        verify(auditLogger).logSecurityEventAsync(
            org.mockito.ArgumentMatchers.argThat(event ->
                event.getType() == SecurityEventType.PATH_PARAMETER_MANIPULATION &&
                event.getUserId().equals(1002L) &&
                event.getSeverity() == SeverityLevel.CRITICAL &&
                event.getDetails().containsKey("attackPattern") &&
                event.getDetails().get("tokenUserId").equals(1002L) &&
                event.getDetails().get("pathUserId").equals(1001L)
            )
        );
    }

    // Helper methods
    private UpsertQuestionCommand createUpsertQuestionCommand(Long userId, Long questionBankId) {
        var requestDto = new UpsertQuestionRequestDto();
        requestDto.setSourceQuestionId("test-question-123");
        requestDto.setQuestionType("mcq");
        requestDto.setTitle("Test Question");
        requestDto.setContent("Test content");

        return new UpsertQuestionCommand(userId, questionBankId, requestDto);
    }

    private JwtAuthenticationToken createJwtAuthenticationToken(String userId) {
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", userId)
                .claim("iat", Instant.now())
                .claim("exp", Instant.now().plusSeconds(3600))
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}