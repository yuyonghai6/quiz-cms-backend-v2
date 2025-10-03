package com.quizfun.questionbank.application.validation;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionBankOwnershipValidatorTest {

    @Mock
    private QuestionBanksPerUserRepository repository;

    @Mock
    private RetryHelper retryHelper;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    private QuestionBankOwnershipValidator validator;
    private UpsertQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        validator = new QuestionBankOwnershipValidator(repository, retryHelper, securityAuditLogger);
        validCommand = new UpsertQuestionCommand(
            1001L, 2002L, createValidRequest()
        );
    }

    @Nested
    @DisplayName("Successful Validation Tests")
    class SuccessfulValidationTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should pass validation when user owns active question bank")
        @Description("Verifies that validation succeeds when user owns the question bank and it is active")
        void shouldPassValidationWhenUserOwnsActiveQuestionBank() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(true));
            when(repository.isQuestionBankActive(1001L, 2002L))
                .thenReturn(Result.success(true));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should skip validation for non-UpsertQuestionCommand")
        @Description("Verifies that validator skips validation for commands that are not UpsertQuestionCommand")
        void shouldSkipValidationForNonUpsertQuestionCommand() {
            // Arrange
            var otherCommand = new Object();

            // Act
            var result = validator.validate(otherCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verifyNoInteractions(repository, retryHelper);
        }
    }

    @Nested
    @DisplayName("Ownership Validation Failure Tests")
    class OwnershipValidationFailureTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should fail validation when user doesn't own question bank")
        @Description("Verifies that validation fails with UNAUTHORIZED_ACCESS when user doesn't own the question bank")
        void shouldFailValidationWhenUserDoesntOwnQuestionBank() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(false));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");
            assertThat(result.getError()).contains("User 1001 doesn't own question bank 2002");
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            // Should not check active status if ownership fails
            verify(retryHelper, never()).executeWithRetry(any(), eq("isQuestionBankActive"));
        }

        @Test
        @Epic("Security Breach Protection")
        @Story("023.token-privilege-escalation-prevention")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should log privilege escalation when ownership validation fails")
        @Description("US-023: Verifies that privilege escalation attempts are logged when user doesn't own question bank")
        void shouldLogPrivilegeEscalationWhenOwnershipValidationFails() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(false));

            // Act
            var result = validator.validate(validCommand);

            // Assert - Validation should fail
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");

            // US-023: Verify privilege escalation event was logged
            verify(securityAuditLogger).logSecurityEventAsync(
                org.mockito.ArgumentMatchers.argThat(event ->
                    event.getType() == com.quizfun.questionbank.application.security.SecurityEventType.TOKEN_PRIVILEGE_ESCALATION &&
                    event.getUserId().equals(1001L) &&
                    event.getSeverity() == com.quizfun.questionbank.application.security.SeverityLevel.CRITICAL &&
                    event.getDetails().containsKey("violationType") &&
                    event.getDetails().get("violationType").equals("OWNERSHIP_VIOLATION") &&
                    event.getDetails().get("questionBankId").equals(2002L)
                )
            );
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should fail validation when question bank is inactive")
        @Description("Verifies that validation fails when user owns the question bank but it is inactive")
        void shouldFailValidationWhenQuestionBankIsInactive() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(true));
            when(repository.isQuestionBankActive(1001L, 2002L))
                .thenReturn(Result.success(false));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");
            assertThat(result.getError()).contains("Question bank 2002 is not active for user 1001");
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
        }

        @Test
        @Epic("Security Breach Protection")
        @Story("023.token-privilege-escalation-prevention")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should log privilege escalation when accessing inactive question bank")
        @Description("US-023: Verifies that privilege escalation attempts are logged when user tries to access inactive question bank")
        void shouldLogPrivilegeEscalationWhenAccessingInactiveQuestionBank() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(true));
            when(repository.isQuestionBankActive(1001L, 2002L))
                .thenReturn(Result.success(false));

            // Act
            var result = validator.validate(validCommand);

            // Assert - Validation should fail
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");

            // US-023: Verify privilege escalation event was logged for inactive resource access
            verify(securityAuditLogger).logSecurityEventAsync(
                org.mockito.ArgumentMatchers.argThat(event ->
                    event.getType() == com.quizfun.questionbank.application.security.SecurityEventType.TOKEN_PRIVILEGE_ESCALATION &&
                    event.getUserId().equals(1001L) &&
                    event.getSeverity() == com.quizfun.questionbank.application.security.SeverityLevel.HIGH &&
                    event.getDetails().containsKey("violationType") &&
                    event.getDetails().get("violationType").equals("INACTIVE_RESOURCE_ACCESS") &&
                    event.getDetails().get("questionBankId").equals(2002L)
                )
            );
        }
    }

    @Nested
    @DisplayName("Repository Error Handling Tests")
    class RepositoryErrorHandlingTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle ownership repository failure gracefully")
        @Description("Verifies that repository failures during ownership validation are handled and propagated correctly")
        void shouldHandleOwnershipRepositoryFailureGracefully() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenReturn(Result.failure("DATABASE_ERROR", "Connection failed"));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(result.getError()).isEqualTo("Connection failed");
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle active status repository failure gracefully")
        @Description("Verifies that repository failures during active status validation are handled correctly")
        void shouldHandleActiveStatusRepositoryFailureGracefully() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenReturn(Result.failure("NETWORK_ERROR", "Timeout occurred"));

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(true));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("NETWORK_ERROR");
            assertThat(result.getError()).isEqualTo("Timeout occurred");
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle unexpected exceptions gracefully")
        @Description("Verifies that unexpected exceptions during validation are caught and handled")
        void shouldHandleUnexpectedExceptionsGracefully() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenThrow(new RuntimeException("Unexpected error"));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("OWNERSHIP_VALIDATION_ERROR");
            assertThat(result.getError()).isEqualTo("An error occurred while validating question bank ownership");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle boundary values correctly")
        @Description("Verifies that validation works correctly with boundary values for user and question bank IDs")
        void shouldHandleBoundaryValuesCorrectly() {
            // Arrange
            var extremeCommand = new UpsertQuestionCommand(
                Long.MAX_VALUE, Long.MAX_VALUE, createValidRequest()
            );

            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(Long.MAX_VALUE, Long.MAX_VALUE))
                .thenReturn(Result.success(true));
            when(repository.isQuestionBankActive(Long.MAX_VALUE, Long.MAX_VALUE))
                .thenReturn(Result.success(true));

            // Act
            var result = validator.validate(extremeCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            verify(repository).validateOwnership(Long.MAX_VALUE, Long.MAX_VALUE);
            verify(repository).isQuestionBankActive(Long.MAX_VALUE, Long.MAX_VALUE);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle repository returning null results")
        @Description("Verifies that validation handles null results from repository gracefully")
        void shouldHandleRepositoryReturningNullResults() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenAnswer(invocation -> invocation.getArgument(0, java.util.function.Supplier.class).get());

            when(repository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(null));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");
        }
    }

    @Nested
    @DisplayName("Integration with RetryHelper Tests")
    class RetryHelperIntegrationTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should use retry helper for both repository calls")
        @Description("Verifies that both repository calls are wrapped with retry helper")
        void shouldUseRetryHelperForBothRepositoryCalls() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenReturn(Result.success(true));
            when(retryHelper.executeWithRetry(any(), eq("isQuestionBankActive")))
                .thenReturn(Result.success(true));

            // Act
            validator.validate(validCommand);

            // Assert
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
            // Repository should not be called directly
            verifyNoInteractions(repository);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("QuestionBankOwnershipValidatorTest.Should handle retry exhaustion correctly")
        @Description("Verifies that retry exhaustion is handled and appropriate error is returned")
        void shouldHandleRetryExhaustionCorrectly() {
            // Arrange
            when(retryHelper.executeWithRetry(any(), eq("validateOwnership")))
                .thenReturn(Result.failure("RETRY_EXHAUSTED",
                    "Operation validateOwnership failed after 4 attempts"));

            // Act
            var result = validator.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("RETRY_EXHAUSTED");
            assertThat(result.getError()).contains("failed after 4 attempts");
        }
    }

    private UpsertQuestionRequestDto createValidRequest() {
        return UpsertQuestionRequestDto.builder()
            .sourceQuestionId("Q123")
            .questionType("MCQ")
            .title("Test Question")
            .content("Test Content")
            .build();
    }
}