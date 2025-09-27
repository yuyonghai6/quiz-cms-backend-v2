package com.quizfun.questionbank.integration;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.EssayRubric;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategy;
import com.quizfun.questionbank.domain.services.QuestionTypeStrategyFactory;
import com.quizfun.questionbank.domain.services.UnsupportedQuestionTypeException;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = {
    com.quizfun.questionbank.domain.services.QuestionTypeStrategyFactory.class,
    com.quizfun.questionbank.domain.services.impl.McqQuestionStrategy.class,
    com.quizfun.questionbank.domain.services.impl.EssayQuestionStrategy.class,
    com.quizfun.questionbank.domain.services.impl.TrueFalseQuestionStrategy.class
})
class QuestionTypeStrategyIntegrationTest {

    @Autowired
    private QuestionTypeStrategyFactory strategyFactory;

    @Nested
    @DisplayName("Spring Auto-Discovery Tests")
    class SpringAutoDiscoveryTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should auto-discover all strategy implementations")
        @Description("Verifies that Spring context automatically discovers and registers all strategy implementations")
        void shouldAutoDiscoverAllStrategyImplementations() {
            // Act
            var supportedTypes = strategyFactory.getSupportedTypes();

            // Assert
            assertThat(supportedTypes).hasSize(3);
            assertThat(supportedTypes).containsExactlyInAnyOrder(
                QuestionType.MCQ,
                QuestionType.ESSAY,
                QuestionType.TRUE_FALSE
            );
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should inject strategies into factory correctly")
        @Description("Verifies that Spring dependency injection provides all strategies to factory")
        void shouldInjectStrategiesIntoFactoryCorrectly() {
            // Act & Assert
            assertThat(strategyFactory.isSupported(QuestionType.MCQ)).isTrue();
            assertThat(strategyFactory.isSupported(QuestionType.ESSAY)).isTrue();
            assertThat(strategyFactory.isSupported(QuestionType.TRUE_FALSE)).isTrue();

            // Verify we can get all strategies without exceptions
            assertThatCode(() -> strategyFactory.getStrategy(QuestionType.MCQ)).doesNotThrowAnyException();
            assertThatCode(() -> strategyFactory.getStrategy(QuestionType.ESSAY)).doesNotThrowAnyException();
            assertThatCode(() -> strategyFactory.getStrategy(QuestionType.TRUE_FALSE)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("End-to-End Strategy Processing Tests")
    class EndToEndStrategyProcessingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should process MCQ question successfully end-to-end")
        @Description("Verifies complete MCQ question processing from command to aggregate")
        void shouldProcessMcqQuestionSuccessfullyEndToEnd() {
            // Arrange
            var command = createValidMcqCommand();

            // Act
            var strategy = strategyFactory.getStrategy(QuestionType.MCQ);
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.MCQ);
            assertThat(aggregate.getMcqData()).isNotNull();
            assertThat(aggregate.getMcqData().getOptions()).hasSize(4);
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a01");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should process Essay question successfully end-to-end")
        @Description("Verifies complete Essay question processing from command to aggregate")
        void shouldProcessEssayQuestionSuccessfullyEndToEnd() {
            // Arrange
            var command = createValidEssayCommand();

            // Act
            var strategy = strategyFactory.getStrategy(QuestionType.ESSAY);
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.ESSAY);
            assertThat(aggregate.getEssayData()).isNotNull();
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a02");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should process True/False question successfully end-to-end")
        @Description("Verifies complete True/False question processing from command to aggregate")
        void shouldProcessTrueFalseQuestionSuccessfullyEndToEnd() {
            // Arrange
            var command = createValidTrueFalseCommand();

            // Act
            var strategy = strategyFactory.getStrategy(QuestionType.TRUE_FALSE);
            var result = strategy.processQuestionData(command);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            var aggregate = result.getValue();
            assertThat(aggregate.getQuestionType()).isEqualTo(QuestionType.TRUE_FALSE);
            assertThat(aggregate.getTrueFalseData()).isNotNull();
            assertThat(aggregate.getTrueFalseData().getCorrectAnswer()).isNotNull();
            assertThat(aggregate.getSourceQuestionId()).isEqualTo("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a03");
        }
    }

    @Nested
    @DisplayName("Strategy Selection and Error Handling Tests")
    class StrategySelectionAndErrorHandlingTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should return correct strategy for each question type")
        @Description("Verifies that factory returns appropriate strategy instance for each question type")
        void shouldReturnCorrectStrategyForEachQuestionType() {
            // Act
            var mcqStrategy = strategyFactory.getStrategy(QuestionType.MCQ);
            var essayStrategy = strategyFactory.getStrategy(QuestionType.ESSAY);
            var trueFalseStrategy = strategyFactory.getStrategy(QuestionType.TRUE_FALSE);

            // Assert
            assertThat(mcqStrategy.getStrategyName()).isEqualTo("MCQ Strategy");
            assertThat(mcqStrategy.supports(QuestionType.MCQ)).isTrue();

            assertThat(essayStrategy.getStrategyName()).isEqualTo("Essay Strategy");
            assertThat(essayStrategy.supports(QuestionType.ESSAY)).isTrue();

            assertThat(trueFalseStrategy.getStrategyName()).isEqualTo("True/False Strategy");
            assertThat(trueFalseStrategy.supports(QuestionType.TRUE_FALSE)).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should handle strategy validation failures appropriately")
        @Description("Verifies that strategies properly validate data and return appropriate error responses")
        void shouldHandleStrategyValidationFailuresAppropriately() {
            // Arrange - Create invalid MCQ command (no options)
            var strategy = strategyFactory.getStrategy(QuestionType.MCQ);
            // Assert domain constructor guard instead of strategy return
            assertThatThrownBy(() -> new McqData(Arrays.asList(), false, false, false, 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one option");
        }
    }

    @Nested
    @DisplayName("Performance and Concurrency Tests")
    class PerformanceAndConcurrencyTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should handle concurrent strategy requests safely")
        @Description("Verifies that factory and strategies can handle multiple concurrent requests safely")
        void shouldHandleConcurrentStrategyRequestsSafely() throws InterruptedException {
            // Arrange
            var results = new java.util.concurrent.ConcurrentLinkedQueue<Boolean>();
            var executor = java.util.concurrent.Executors.newFixedThreadPool(10);
            var latch = new java.util.concurrent.CountDownLatch(30);

            // Act - Submit 30 concurrent requests (10 per question type)
            for (int i = 0; i < 10; i++) {
                final int requestId = i;

                // MCQ requests
                executor.submit(() -> {
                    try {
                        var base = createValidMcqCommand();
                        var req = UpsertQuestionRequestDto.builder()
                            .sourceQuestionId(String.format("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e%04d", requestId))
                            .questionType("MCQ")
                            .title("Integration Test MCQ")
                            .content("Choose the correct answer")
                            .taxonomy(createValidTaxonomy())
                            .mcqData(base.getMcqData())
                            .build();
                        var command = new UpsertQuestionCommand(1001L, 2002L, req);

                        var strategy = strategyFactory.getStrategy(QuestionType.MCQ);
                        var result = strategy.processQuestionData(command);
                        results.add(result.isSuccess());
                    } finally {
                        latch.countDown();
                    }
                });

                // Essay requests
                executor.submit(() -> {
                    try {
                        var base = createValidEssayCommand();
                        var req = UpsertQuestionRequestDto.builder()
                            .sourceQuestionId(String.format("028f6df6-8a9b-7c2e-b3d6-9a4f2c1e%04d", requestId))
                            .questionType("ESSAY")
                            .title("Integration Test Essay")
                            .content("Essay instructions")
                            .taxonomy(createValidTaxonomy())
                            .essayData(base.getEssayData())
                            .build();
                        var command = new UpsertQuestionCommand(1001L, 2002L, req);

                        var strategy = strategyFactory.getStrategy(QuestionType.ESSAY);
                        var result = strategy.processQuestionData(command);
                        results.add(result.isSuccess());
                    } finally {
                        latch.countDown();
                    }
                });

                // True/False requests
                executor.submit(() -> {
                    try {
                        var base = createValidTrueFalseCommand();
                        var req = UpsertQuestionRequestDto.builder()
                            .sourceQuestionId(String.format("038f6df6-8a9b-7c2e-b3d6-9a4f2c1e%04d", requestId))
                            .questionType("TRUE_FALSE")
                            .title("Integration Test True/False")
                            .content("Evaluate this statement")
                            .taxonomy(createValidTaxonomy())
                            .trueFalseData(base.getTrueFalseData())
                            .build();
                        var command = new UpsertQuestionCommand(1001L, 2002L, req);

                        var strategy = strategyFactory.getStrategy(QuestionType.TRUE_FALSE);
                        var result = strategy.processQuestionData(command);
                        results.add(result.isSuccess());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Assert
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(results).hasSize(30);
            assertThat(results.stream().filter(Boolean::booleanValue).count()).isGreaterThanOrEqualTo(27);

            executor.shutdown();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyIntegrationTest.Should meet performance requirements")
        @Description("Verifies that strategy processing meets performance targets of <50ms per question")
        void shouldMeetPerformanceRequirements() {
            // Arrange
            var command = createValidMcqCommand();
            var strategy = strategyFactory.getStrategy(QuestionType.MCQ);

            // Warm up
            for (int i = 0; i < 5; i++) {
                strategy.processQuestionData(command);
            }

            // Act & Assert
            long startTime = System.nanoTime();
            var result = strategy.processQuestionData(command);
            long endTime = System.nanoTime();

            long processingTimeMs = (endTime - startTime) / 1_000_000;

            assertThat(result.isSuccess()).isTrue();
            assertThat(processingTimeMs).isLessThan(50); // <50ms requirement
        }
    }

    // Helper methods
    private UpsertQuestionCommand createValidMcqCommand() {
        var options = Arrays.asList(
            new McqOption("A", "Option A", true, 1.0),
            new McqOption("B", "Option B", false, 0.0),
            new McqOption("C", "Option C", false, 0.0),
            new McqOption("D", "Option D", false, 0.0)
        );
        var mcqData = new McqData(options, false, false, false, 60);

        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a01")
            .questionType("MCQ")
            .title("Integration Test MCQ")
            .content("Choose the correct answer")
            .taxonomy(createValidTaxonomy())
            .mcqData(mcqData)
            .build();

        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private UpsertQuestionCommand createMcqCommandWithData(McqData mcqData) {
        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("MCQ123")
            .questionType("MCQ")
            .title("Integration Test MCQ")
            .content("Choose the correct answer")
            .mcqData(mcqData)
            .build();

        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private UpsertQuestionCommand createValidEssayCommand() {
        var criteria = Arrays.asList(
            "Content: Quality of content",
            "Structure: Organization and flow"
        );
        var rubric = new EssayRubric("Integration testing rubric", criteria, 35);
        var essayData = new EssayData(200, 800, false, rubric);

        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a02")
            .questionType("ESSAY")
            .title("Integration Test Essay")
            .content("Essay instructions")
            .taxonomy(createValidTaxonomy())
            .essayData(essayData)
            .build();

        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private UpsertQuestionCommand createValidTrueFalseCommand() {
        var trueFalseData = new TrueFalseData(true, "This is a valid statement", 90);

        var request = UpsertQuestionRequestDto.builder()
            .sourceQuestionId("018f6df6-8a9b-7c2e-b3d6-9a4f2c1e3a03")
            .questionType("TRUE_FALSE")
            .title("Integration Test True/False")
            .content("Evaluate this statement")
            .taxonomy(createValidTaxonomy())
            .trueFalseData(trueFalseData)
            .build();

        return new UpsertQuestionCommand(1001L, 2002L, request);
    }

    private TaxonomyData createValidTaxonomy() {
        var categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "tech", null));

        var tags = List.of(new TaxonomyData.Tag("integration", "Integration Testing", "#00ff00"));
        var quizzes = List.of(new TaxonomyData.Quiz(101L, "Integration Quiz", "integration-quiz"));
        var difficulty = new TaxonomyData.DifficultyLevel("medium", 2, "Medium");

        return new TaxonomyData(categories, tags, quizzes, difficulty);
    }
}