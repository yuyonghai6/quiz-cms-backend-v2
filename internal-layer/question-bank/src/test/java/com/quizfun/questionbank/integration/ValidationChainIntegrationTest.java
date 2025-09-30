package com.quizfun.questionbank.integration;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.application.ports.out.TaxonomySetRepository;
import com.quizfun.questionbank.application.security.SecurityAuditLogger;
import com.quizfun.questionbank.application.security.SecurityContextValidator;
import com.quizfun.questionbank.application.validation.QuestionBankOwnershipValidator;
import com.quizfun.questionbank.application.validation.QuestionDataIntegrityValidator;
import com.quizfun.questionbank.application.validation.TaxonomyReferenceValidator;
import com.quizfun.questionbank.infrastructure.monitoring.ValidationChainMetrics;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import com.quizfun.questionbank.infrastructure.configuration.ValidationChainConfig;
import com.quizfun.questionbank.infrastructure.utils.RetryHelper;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
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

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

    @ExtendWith(MockitoExtension.class)
    @org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ValidationChainIntegrationTest {

    @Mock
    private QuestionBanksPerUserRepository questionBanksRepository;

    @Mock
    private TaxonomySetRepository taxonomyRepository;

    @Mock
    private RetryHelper retryHelper;

    @Mock
    private SecurityAuditLogger securityAuditLogger;

    @Mock
    private ValidationChainMetrics metrics;

    private ValidationHandler validationChain;
    private UpsertQuestionCommand validCommand;

    @BeforeEach
    void setUp() {
        // Create individual validators
        var securityValidator = new SecurityContextValidator(null, securityAuditLogger, retryHelper, metrics);
        var ownershipValidator = new QuestionBankOwnershipValidator(questionBanksRepository, retryHelper);
        var taxonomyValidator = new TaxonomyReferenceValidator(taxonomyRepository);
        var dataValidator = new QuestionDataIntegrityValidator();

        // Configure the validation chain using the configuration class
        var config = new ValidationChainConfig();
        validationChain = config.questionUpsertValidationChain(securityValidator, ownershipValidator, taxonomyValidator, dataValidator);

        // Create a valid command for testing
        validCommand = new UpsertQuestionCommand(1001L, 2002L, createValidRequest());

        // Setup valid JWT authentication context for security validation
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .claim("sub", "1001")  // Matches the user ID in validCommand
                .claim("iat", Instant.now())
                .claim("exp", Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        // Setup default successful behavior for retry helper
        when(retryHelper.executeWithRetry(any(), any())).thenAnswer(invocation -> {
            var supplier = invocation.getArgument(0, java.util.function.Supplier.class);
            if (supplier == null) {
                return Result.failure("RETRY_EXHAUSTED", "Operation failed: supplier was null");
            }
            return supplier.get();
        });
    }

    @Nested
    @DisplayName("Complete Chain Success Tests")
    class CompleteChainSuccessTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should pass validation through complete chain")
        @Description("Verifies that a valid command passes through all validators in the chain successfully")
        void shouldPassValidationThroughCompleteChain() {
            // Arrange
            setupSuccessfulRepositoryResponses();

            // Act
            var result = validationChain.validate(validCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();

            // Verify all validators were called in order
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
            verify(taxonomyRepository).validateTaxonomyReferences(eq(1001L), eq(2002L), any());
            // Data integrity validator doesn't call external services
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should handle minimal valid command")
        @Description("Verifies that a minimal valid command passes through the entire validation chain")
        void shouldHandleMinimalValidCommand() {
            // Arrange
            var minimalRequest = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3001")
                .questionType("TRUE_FALSE")
                .title("Minimal Question")
                .content("True or False?")
                .taxonomy(createValidTaxonomy())
                .trueFalseData(new com.quizfun.questionbank.domain.entities.TrueFalseData(true, null, 30))
                .build();

            var minimalCommand = new UpsertQuestionCommand(1001L, 2002L, minimalRequest);
            setupSuccessfulRepositoryResponses();

            // Act
            var result = validationChain.validate(minimalCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("Chain Failure at Different Steps Tests")
    class ChainFailureTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should fail at ownership validation step")
        @Description("Verifies that validation chain fails at the first step (ownership) and doesn't proceed to subsequent validators")
        void shouldFailAtOwnershipValidationStep() {
            // Arrange - ownership validation fails
            when(questionBanksRepository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(false));

            // Act
            var result = validationChain.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UNAUTHORIZED_ACCESS");

            // Verify chain stopped at first validator
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper, never()).executeWithRetry(any(), eq("isQuestionBankActive"));
            verifyNoInteractions(taxonomyRepository);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should fail at taxonomy validation step")
        @Description("Verifies that validation chain fails at the second step (taxonomy) after passing ownership validation")
        void shouldFailAtTaxonomyValidationStep() {
            // Arrange - ownership passes, taxonomy fails
            when(questionBanksRepository.validateOwnership(1001L, 2002L))
                .thenReturn(Result.success(true));
            when(questionBanksRepository.isQuestionBankActive(1001L, 2002L))
                .thenReturn(Result.success(true));
            when(taxonomyRepository.validateTaxonomyReferences(eq(1001L), eq(2002L), any()))
                .thenReturn(Result.success(false));
            when(taxonomyRepository.getInvalidTaxonomyReferences(eq(1001L), eq(2002L), any()))
                .thenReturn(Result.success(List.of("invalid-tag-id")));

            // Act
            var result = validationChain.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TAXONOMY_REFERENCE_NOT_FOUND");
            assertThat(result.getError()).contains("invalid-tag-id");

            // Verify chain executed ownership and taxonomy validation
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
            verify(taxonomyRepository).validateTaxonomyReferences(eq(1001L), eq(2002L), any());
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should fail at data integrity validation step")
        @Description("Verifies that validation chain fails at the final step (data integrity) after passing all previous validations")
        void shouldFailAtDataIntegrityValidationStep() {
            // Arrange - ownership and taxonomy pass, data validation fails
            setupSuccessfulRepositoryResponses();

            // Create command with invalid data (missing MCQ data for MCQ question)
            var invalidRequest = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("MCQ Question")
                .content("Choose the correct answer")
                .taxonomy(createValidTaxonomy())
                // Missing mcqData for MCQ question
                .build();

            var invalidCommand = new UpsertQuestionCommand(1001L, 2002L, invalidRequest);

            // Act
            var result = validationChain.validate(invalidCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("TYPE_DATA_MISMATCH");
            assertThat(result.getError()).contains("mcq_data is required for MCQ questions");

            // Verify all validators were called
            verify(retryHelper).executeWithRetry(any(), eq("validateOwnership"));
            verify(retryHelper).executeWithRetry(any(), eq("isQuestionBankActive"));
            verify(taxonomyRepository).validateTaxonomyReferences(eq(1001L), eq(2002L), any());
        }
    }

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should handle complex taxonomy structure validation")
        @Description("Verifies that validation chain correctly handles commands with complex taxonomy structures")
        void shouldHandleComplexTaxonomyStructureValidation() {
            // Arrange
            var complexRequest = createRequestWithComplexTaxonomy();
            var complexCommand = new UpsertQuestionCommand(1001L, 2002L, complexRequest);

            setupSuccessfulRepositoryResponses();

            // Act
            var result = validationChain.validate(complexCommand);

            // Assert
            assertThat(result.isSuccess()).isTrue();

            // Verify taxonomy validation was called with extracted IDs
            verify(taxonomyRepository).validateTaxonomyReferences(
                eq(1001L),
                eq(2002L),
                argThat(ids -> ids.size() == 9 && // All taxonomy IDs from complex structure
                              ids.contains("tech") &&
                              ids.contains("js-arrays") &&
                              ids.contains("101") &&
                              ids.contains("easy"))
            );
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should handle repository failures gracefully throughout chain")
        @Description("Verifies that the validation chain handles repository failures at any step gracefully")
        void shouldHandleRepositoryFailuresGracefullyThroughoutChain() {
            // Arrange - simulate network timeout
            when(retryHelper.executeWithRetry(any(), any()))
                .thenAnswer(invocation -> Result.failure("RETRY_EXHAUSTED", "Connection timeout after 4 attempts"));

            // Act
            var result = validationChain.validate(validCommand);

            // Assert
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("RETRY_EXHAUSTED");
            assertThat(result.getError()).contains("Connection timeout after 4 attempts");
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency Tests")
    class PerformanceConcurrencyTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("ValidationChainIntegrationTest.Should handle concurrent validation requests")
        @Description("Verifies that validation chain can handle multiple concurrent validation requests safely")
        void shouldHandleConcurrentValidationRequests() throws InterruptedException {
            // Arrange
            setupSuccessfulRepositoryResponses();

            var results = new java.util.concurrent.ConcurrentLinkedQueue<Boolean>();
            var executor = java.util.concurrent.Executors.newFixedThreadPool(10);
            var latch = new java.util.concurrent.CountDownLatch(50);

            // Act - Submit 50 concurrent validation requests
            for (int i = 0; i < 50; i++) {
                final int requestId = i;
                executor.submit(() -> {
                    try {
                        // Set up security context for this thread (required for SecurityContextValidator)
                        var jwt = Jwt.withTokenValue("test-token-" + requestId)
                                .header("alg", "HS256")
                                .claim("sub", "1001")  // Matches the user ID in command
                                .claim("iat", Instant.now())
                                .claim("exp", Instant.now().plusSeconds(3600))
                                .build();
                        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

                        var request = UpsertQuestionRequestDto.builder()
                            .sourceQuestionId(String.format("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e%04d", requestId))
                            .questionType("MCQ")
                            .title("Concurrent Question " + requestId)
                            .content("Test content " + requestId)
                            .taxonomy(createValidTaxonomy())
                            .mcqData(createValidMcqData())
                            .build();

                        var command = new UpsertQuestionCommand(1001L, 2002L, request);
                        var result = validationChain.validate(command);
                        results.add(result.isSuccess());
                    } finally {
                        // Clear security context for this thread
                        SecurityContextHolder.clearContext();
                        latch.countDown();
                    }
                });
            }

            // Assert
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(results).hasSize(50);
            assertThat(results.stream().allMatch(success -> success)).isTrue();

            executor.shutdown();
        }
    }

    private void setupSuccessfulRepositoryResponses() {
        when(questionBanksRepository.validateOwnership(1001L, 2002L))
            .thenReturn(Result.success(true));
        when(questionBanksRepository.isQuestionBankActive(1001L, 2002L))
            .thenReturn(Result.success(true));
        when(taxonomyRepository.validateTaxonomyReferences(eq(1001L), eq(2002L), any()))
            .thenReturn(Result.success(true));
    }

    private UpsertQuestionRequestDto createValidRequest() {
        return UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3b5a")
            .questionType("MCQ")
            .title("Sample MCQ Question")
            .content("What is 2+2?")
            .taxonomy(createValidTaxonomy())
            .mcqData(createValidMcqData())
            .build();
    }

    private TaxonomyData createValidTaxonomy() {
        var categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "tech", null));

        var tags = List.of(new TaxonomyData.Tag("js-arrays", "JavaScript", "#f7df1e"));
        var quizzes = List.of(new TaxonomyData.Quiz(101L, "JS Quiz", "js-quiz"));
        var difficulty = new TaxonomyData.DifficultyLevel("easy", 1, "Easy");

        return new TaxonomyData(categories, tags, quizzes, difficulty);
    }

    private UpsertQuestionRequestDto createRequestWithComplexTaxonomy() {
        var categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "tech", null));
        categories.setLevel2(new TaxonomyData.Category("prog", "Programming", "prog", "tech"));
        categories.setLevel3(new TaxonomyData.Category("web", "Web Dev", "web", "prog"));
        categories.setLevel4(new TaxonomyData.Category("js", "JavaScript", "js", "web"));

        var tags = List.of(
            new TaxonomyData.Tag("js-arrays", "JS Arrays", "#f7df1e"),
            new TaxonomyData.Tag("methods", "Methods", "#61dafb")
        );

        var quizzes = List.of(
            new TaxonomyData.Quiz(101L, "JS Fundamentals", "js-fundamentals"),
            new TaxonomyData.Quiz(202L, "Advanced JS", "advanced-js")
        );

        var difficulty = new TaxonomyData.DifficultyLevel("easy", 1, "Easy");
        var taxonomy = new TaxonomyData(categories, tags, quizzes, difficulty);

        return UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3999")
            .questionType("MCQ")
            .title("Complex Taxonomy Question")
            .content("Test complex taxonomy")
            .taxonomy(taxonomy)
            .mcqData(createValidMcqData())
            .build();
    }

    private McqData createValidMcqData() {
        var options = List.of(
            new McqOption("A", "Option A", true, 1.0),
            new McqOption("B", "Option B", false, 0.0)
        );

        return new McqData(options, false, false, false, 60);
    }
}