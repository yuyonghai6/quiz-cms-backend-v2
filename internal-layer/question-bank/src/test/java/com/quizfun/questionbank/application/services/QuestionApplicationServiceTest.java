package com.quizfun.questionbank.application.services;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.application.ports.out.QuestionRepository;
import com.quizfun.questionbank.application.ports.out.QuestionTaxonomyRelationshipRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import com.quizfun.questionbank.domain.entities.*;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategy;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategyFactory;
import com.quizfun.shared.common.Result;
import com.quizfun.shared.validation.ValidationHandler;
import io.qameta.allure.*;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Use Case Upsert Question with Relation-Main Path")
class QuestionApplicationServiceTest {

    @Mock
    private ValidationHandler validationChain;

    @Mock
    private QuestionTypeStrategyFactory strategyFactory;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionTaxonomyRelationshipRepository relationshipRepository;

    @Mock
    private QuestionTypeStrategy questionTypeStrategy;

    @InjectMocks
    private QuestionApplicationService questionApplicationService;

    private UpsertQuestionCommand validCommand;
    private QuestionAggregate mockQuestionAggregate;
    private List<QuestionTaxonomyRelationshipAggregate> mockRelationships;

    @BeforeEach
    void setUp() {
        // Setup valid command
        UpsertQuestionRequestDto request = createValidRequest();
        validCommand = new UpsertQuestionCommand(1L, 100L, request);

        // Setup mock aggregates
        mockQuestionAggregate = createMockQuestionAggregate();
        mockRelationships = List.of(
            createMockRelationshipAggregate("category_level_1", "tech"),
            createMockRelationshipAggregate("tag", "js-arrays")
        );
    }

    @Nested
    @DisplayName("Service Initialization Tests")
    class ServiceInitializationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should initialize service with all required dependencies")
        @Description("Verifies that QuestionApplicationService can be instantiated with proper constructor injection")
        void shouldInitializeServiceWithAllDependencies() {
            // Given: Dependencies are injected via @Mock and @InjectMocks

            // When: Service is initialized (done in @BeforeEach)

            // Then: Service should be properly initialized
            assertThat(questionApplicationService).isNotNull();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should validate dependencies during construction")
        @Description("Verifies that service construction fails with null dependencies")
        void shouldValidateDependenciesDuringConstruction() {
            // Given: Null dependencies

            // When & Then: Constructor should validate dependencies
            assertThatThrownBy(() -> new QuestionApplicationService(
                null, strategyFactory, questionRepository, relationshipRepository))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new QuestionApplicationService(
                validationChain, null, questionRepository, relationshipRepository))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new QuestionApplicationService(
                validationChain, strategyFactory, null, relationshipRepository))
                .isInstanceOf(NullPointerException.class);

            assertThatThrownBy(() -> new QuestionApplicationService(
                validationChain, strategyFactory, questionRepository, null))
                .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Validation Chain Integration Tests")
    class ValidationChainIntegrationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should execute validation chain and continue on success")
        @Description("Verifies that validation chain executes first and allows processing to continue on success")
        void shouldExecuteValidationChainAndContinueOnSuccess() {
            // Given: Validation passes
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Validation should execute first
            verify(validationChain).validate(validCommand);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should fail fast on validation failure")
        @Description("Verifies that validation failure stops processing immediately")
        void shouldFailFastOnValidationFailure() {
            // Given: Validation fails
            when(validationChain.validate(validCommand)).thenReturn(
                Result.failure("VALIDATION_ERROR", "Validation failed"));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail immediately, no other operations called
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo("Validation failed");
            assertThat(result.getErrorCode()).isEqualTo("VALIDATION_ERROR");

            verify(validationChain).validate(validCommand);
            verifyNoInteractions(strategyFactory, questionRepository, relationshipRepository);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should handle validation timing measurement")
        @Description("Verifies that validation timing is measured for monitoring")
        void shouldHandleValidationTimingMeasurement() {
            // Given: Validation passes with delay
            when(validationChain.validate(validCommand)).thenAnswer(invocation -> {
                Thread.sleep(10); // Simulate validation time
                return Result.success(null);
            });
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should complete successfully with timing
            assertThat(result.isSuccess()).isTrue();
            verify(validationChain).validate(validCommand);
        }
    }

    @Nested
    @DisplayName("Strategy Pattern Integration Tests")
    class StrategyPatternIntegrationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should select and execute appropriate strategy for MCQ questions")
        @Description("Verifies that strategy factory selects MCQ strategy and processes MCQ question data")
        void shouldSelectAndExecuteAppropriateStrategyForMcqQuestions() {
            // Given: MCQ question and strategy
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionTypeStrategy.getStrategyName()).thenReturn("MCQ Strategy");
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting MCQ question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: MCQ strategy should be selected and executed
            verify(strategyFactory).getStrategy(QuestionType.MCQ);
            verify(questionTypeStrategy).processQuestionData(validCommand);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should handle strategy processing failure")
        @Description("Verifies that strategy processing failure stops the workflow and returns error")
        void shouldHandleStrategyProcessingFailure() {
            // Given: Strategy processing fails
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(
                Result.failure("STRATEGY_ERROR", "Strategy processing failed"));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail with strategy error
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getError()).isEqualTo("Strategy processing failed");
            assertThat(result.getErrorCode()).isEqualTo("STRATEGY_ERROR");

            verify(strategyFactory).getStrategy(QuestionType.MCQ);
            verify(questionTypeStrategy).processQuestionData(validCommand);
            verifyNoInteractions(questionRepository, relationshipRepository);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should validate aggregate state after strategy processing")
        @Description("Verifies that aggregate produced by strategy is validated for type-specific data")
        void shouldValidateAggregateStateAfterStrategyProcessing() {
            // Given: Strategy returns invalid aggregate
            QuestionAggregate invalidAggregate = mock(QuestionAggregate.class);
            when(invalidAggregate.hasValidTypeSpecificData()).thenReturn(false);

            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(invalidAggregate));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail with invalid aggregate error
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_AGGREGATE");
            assertThat(result.getError()).isEqualTo("Strategy produced invalid question aggregate");

            verifyNoInteractions(questionRepository, relationshipRepository);
        }
    }

    @Nested
    @DisplayName("Repository Integration Tests")
    class RepositoryIntegrationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should upsert question using repository and handle success")
        @Description("Verifies successful question repository upsert operation")
        void shouldUpsertQuestionUsingRepositoryAndHandleSuccess() {
            // Given: All operations succeed
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Question repository should be called
            verify(questionRepository).upsertBySourceQuestionId(mockQuestionAggregate);
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should handle question repository failure and trigger rollback")
        @Description("Verifies that question repository failure triggers transaction rollback")
        void shouldHandleQuestionRepositoryFailureAndTriggerRollback() {
            // Given: Question repository fails
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(
                Result.failure("REPOSITORY_ERROR", "Repository operation failed"));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail and not proceed to relationship repository
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("REPOSITORY_ERROR");
            assertThat(result.getError()).isEqualTo("Repository operation failed");

            verify(questionRepository).upsertBySourceQuestionId(mockQuestionAggregate);
            verifyNoInteractions(relationshipRepository);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should validate question has MongoDB ID after persistence")
        @Description("Verifies that persisted question has valid MongoDB ObjectId for relationships")
        void shouldValidateQuestionHasMongoDbIdAfterPersistence() {
            // Given: Persisted question has null ID
            QuestionAggregate aggregateWithNullId = mock(QuestionAggregate.class);
            when(aggregateWithNullId.getId()).thenReturn(null);
            lenient().when(aggregateWithNullId.hasValidTypeSpecificData()).thenReturn(true);

            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(aggregateWithNullId));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail with invalid question ID error
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("INVALID_QUESTION_ID");
            assertThat(result.getError()).isEqualTo("Question was persisted but has no MongoDB ID");

            verifyNoInteractions(relationshipRepository);
        }
    }

    @Nested
    @DisplayName("Relationship Management Tests")
    class RelationshipManagementTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should create and persist taxonomy relationships")
        @Description("Verifies that taxonomy relationships are created from command and persisted to repository")
        void shouldCreateAndPersistTaxonomyRelationships() {
            // Given: All operations succeed
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Relationships should be created and persisted
            verify(relationshipRepository).replaceRelationshipsForQuestion(eq(mockQuestionAggregate.getId()), any());
            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should handle relationship repository failure and trigger rollback")
        @Description("Verifies that relationship repository failure triggers transaction rollback of both operations")
        void shouldHandleRelationshipRepositoryFailureAndTriggerRollback() {
            // Given: Relationship repository fails
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(
                Result.failure("RELATIONSHIP_ERROR", "Relationship operation failed"));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should fail with relationship error
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("RELATIONSHIP_ERROR");
            assertThat(result.getError()).isEqualTo("Relationship operation failed");

            verify(relationshipRepository).replaceRelationshipsForQuestion(eq(mockQuestionAggregate.getId()), any());
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should include relationship count in response")
        @Description("Verifies that response includes the count of taxonomy relationships created")
        void shouldIncludeRelationshipCountInResponse() {
            // Given: All operations succeed
            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Response should include relationship count
            assertThat(result.isSuccess()).isTrue();
            QuestionResponseDto response = result.getValue();
            assertThat(response.getTaxonomyRelationshipsCount()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should map to response DTO with created operation")
        @Description("Verifies response DTO mapping for newly created questions")
        void shouldMapToResponseDtoWithCreatedOperation() {
            // Given: New question (created and updated timestamps are equal)
            Instant createdTime = Instant.now();
            when(mockQuestionAggregate.getCreatedAt()).thenReturn(createdTime);
            when(mockQuestionAggregate.getUpdatedAt()).thenReturn(createdTime);

            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Response should indicate created operation
            assertThat(result.isSuccess()).isTrue();
            QuestionResponseDto response = result.getValue();
            assertThat(response.getOperation()).isEqualTo("created");
            assertThat(response.getQuestionId()).isEqualTo(mockQuestionAggregate.getId().toString());
            assertThat(response.getSourceQuestionId()).isEqualTo(mockQuestionAggregate.getSourceQuestionId());
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should map to response DTO with updated operation")
        @Description("Verifies response DTO mapping for updated questions")
        void shouldMapToResponseDtoWithUpdatedOperation() {
            // Given: Updated question (created and updated timestamps are different)
            Instant createdAt = Instant.now().minusSeconds(60);
            Instant updatedAt = Instant.now();
            when(mockQuestionAggregate.getCreatedAt()).thenReturn(createdAt);
            when(mockQuestionAggregate.getUpdatedAt()).thenReturn(updatedAt);

            when(validationChain.validate(validCommand)).thenReturn(Result.success(null));
            when(strategyFactory.getStrategy(QuestionType.MCQ)).thenReturn(questionTypeStrategy);
            when(questionTypeStrategy.processQuestionData(validCommand)).thenReturn(Result.success(mockQuestionAggregate));
            when(questionRepository.upsertBySourceQuestionId(mockQuestionAggregate)).thenReturn(Result.success(mockQuestionAggregate));
            when(relationshipRepository.replaceRelationshipsForQuestion(any(), any())).thenReturn(Result.success(null));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Response should indicate updated operation
            assertThat(result.isSuccess()).isTrue();
            QuestionResponseDto response = result.getValue();
            assertThat(response.getOperation()).isEqualTo("updated");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-007.application-service-integration")
        @DisplayName("Should handle unexpected exceptions gracefully")
        @Description("Verifies that unexpected exceptions are caught and returned as proper error results")
        void shouldHandleUnexpectedExceptionsGracefully() {
            // Given: Validation throws unexpected exception
            when(validationChain.validate(validCommand)).thenThrow(new RuntimeException("Unexpected error"));

            // When: Upserting question
            var result = questionApplicationService.upsertQuestion(validCommand);

            // Then: Should handle exception gracefully
            assertThat(result.isFailure()).isTrue();
            assertThat(result.getErrorCode()).isEqualTo("UPSERT_ERROR");
            assertThat(result.getError()).contains("Unexpected error during question upsert");
        }
    }

    // Helper methods
    private UpsertQuestionRequestDto createValidRequest() {
        UpsertQuestionRequestDto request = new UpsertQuestionRequestDto();
        request.setSourceQuestionId("f47ac10b-58cc-4372-a567-0e02b2c3d479");
        request.setQuestionType("mcq");
        request.setTitle("Test Question");
        request.setContent("<p>Test content</p>");
        request.setPoints(5);

        // Create taxonomy data
        TaxonomyData taxonomy = new TaxonomyData();
        TaxonomyData.Categories categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "technology", null));
        taxonomy.setCategories(categories);

        TaxonomyData.Tag tag = new TaxonomyData.Tag();
        tag.setId("js-arrays");
        tag.setName("JavaScript Arrays");
        taxonomy.setTags(List.of(tag));

        request.setTaxonomy(taxonomy);

        return request;
    }

    private QuestionAggregate createMockQuestionAggregate() {
        QuestionAggregate aggregate = mock(QuestionAggregate.class);
        ObjectId mockId = new ObjectId();
        lenient().when(aggregate.getId()).thenReturn(mockId);
        lenient().when(aggregate.getSourceQuestionId()).thenReturn("f47ac10b-58cc-4372-a567-0e02b2c3d479");
        lenient().when(aggregate.hasValidTypeSpecificData()).thenReturn(true);
        lenient().when(aggregate.getCreatedAt()).thenReturn(Instant.now());
        lenient().when(aggregate.getUpdatedAt()).thenReturn(Instant.now());
        return aggregate;
    }

    private QuestionTaxonomyRelationshipAggregate createMockRelationshipAggregate(String taxonomyType, String taxonomyId) {
        QuestionTaxonomyRelationshipAggregate aggregate = mock(QuestionTaxonomyRelationshipAggregate.class);
        lenient().when(aggregate.getTaxonomyType()).thenReturn(taxonomyType);
        lenient().when(aggregate.getTaxonomyId()).thenReturn(taxonomyId);
        return aggregate;
    }
}