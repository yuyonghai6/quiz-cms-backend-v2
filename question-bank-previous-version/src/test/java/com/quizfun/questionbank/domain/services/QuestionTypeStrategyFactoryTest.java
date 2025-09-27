package com.quizfun.questionbank.domain.services;

import com.quizfun.questionbank.domain.entities.QuestionType;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuestionTypeStrategyFactoryTest {

    @Mock
    private QuestionTypeStrategy mcqStrategy;

    @Mock
    private QuestionTypeStrategy essayStrategy;

    @Mock
    private QuestionTypeStrategy trueFalseStrategy;

    private QuestionTypeStrategyFactory factory;

    @BeforeEach
    void setUp() {
        // Setup mock strategies
        when(mcqStrategy.supports(QuestionType.MCQ)).thenReturn(true);
        when(mcqStrategy.supports(QuestionType.ESSAY)).thenReturn(false);
        when(mcqStrategy.supports(QuestionType.TRUE_FALSE)).thenReturn(false);
        when(mcqStrategy.getStrategyName()).thenReturn("MCQ Strategy");

        when(essayStrategy.supports(QuestionType.MCQ)).thenReturn(false);
        when(essayStrategy.supports(QuestionType.ESSAY)).thenReturn(true);
        when(essayStrategy.supports(QuestionType.TRUE_FALSE)).thenReturn(false);
        when(essayStrategy.getStrategyName()).thenReturn("Essay Strategy");

        when(trueFalseStrategy.supports(QuestionType.MCQ)).thenReturn(false);
        when(trueFalseStrategy.supports(QuestionType.ESSAY)).thenReturn(false);
        when(trueFalseStrategy.supports(QuestionType.TRUE_FALSE)).thenReturn(true);
        when(trueFalseStrategy.getStrategyName()).thenReturn("True/False Strategy");
    }

    @Nested
    @DisplayName("Factory Initialization Tests")
    class FactoryInitializationTests {

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should register all strategies successfully")
        @Description("Verifies that factory correctly registers all provided strategies and maps them to their supported types")
        void shouldRegisterAllStrategiesSuccessfully() {
            // Arrange
            List<QuestionTypeStrategy> strategies = Arrays.asList(mcqStrategy, essayStrategy, trueFalseStrategy);

            // Act
            factory = new QuestionTypeStrategyFactory(strategies);

            // Assert
            assertThat(factory.getSupportedTypes()).hasSize(3);
            assertThat(factory.getSupportedTypes()).containsExactlyInAnyOrder(
                QuestionType.MCQ, QuestionType.ESSAY, QuestionType.TRUE_FALSE
            );
            assertThat(factory.isSupported(QuestionType.MCQ)).isTrue();
            assertThat(factory.isSupported(QuestionType.ESSAY)).isTrue();
            assertThat(factory.isSupported(QuestionType.TRUE_FALSE)).isTrue();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should handle empty strategy list")
        @Description("Verifies that factory can be initialized with empty strategy list without errors")
        void shouldHandleEmptyStrategyList() {
            // Arrange
            List<QuestionTypeStrategy> strategies = Collections.emptyList();

            // Act & Assert
            assertThatCode(() -> factory = new QuestionTypeStrategyFactory(strategies))
                .doesNotThrowAnyException();
            assertThat(factory.getSupportedTypes()).isEmpty();
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should reject strategy that supports no types")
        @Description("Verifies that factory throws exception when strategy doesn't support any question type")
        void shouldRejectStrategyThatSupportsNoTypes() {
            // Arrange
            QuestionTypeStrategy invalidStrategy = mock(QuestionTypeStrategy.class);
            when(invalidStrategy.supports(any())).thenReturn(false);
            List<QuestionTypeStrategy> strategies = Arrays.asList(invalidStrategy);

            // Act & Assert
            assertThatThrownBy(() -> new QuestionTypeStrategyFactory(strategies))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Strategy")
                .hasMessageContaining("must support at least one question type");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should reject strategy that supports multiple types")
        @Description("Verifies that factory throws exception when strategy supports more than one question type")
        void shouldRejectStrategyThatSupportsMultipleTypes() {
            // Arrange
            QuestionTypeStrategy invalidStrategy = mock(QuestionTypeStrategy.class);
            when(invalidStrategy.supports(QuestionType.MCQ)).thenReturn(true);
            when(invalidStrategy.supports(QuestionType.ESSAY)).thenReturn(true);
            when(invalidStrategy.supports(QuestionType.TRUE_FALSE)).thenReturn(false);
            List<QuestionTypeStrategy> strategies = Arrays.asList(invalidStrategy);

            // Act & Assert
            assertThatThrownBy(() -> new QuestionTypeStrategyFactory(strategies))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must support exactly one question type");
        }
    }

    @Nested
    @DisplayName("Strategy Retrieval Tests")
    class StrategyRetrievalTests {

        @BeforeEach
        void setUp() {
            List<QuestionTypeStrategy> strategies = Arrays.asList(mcqStrategy, essayStrategy, trueFalseStrategy);
            factory = new QuestionTypeStrategyFactory(strategies);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should return correct strategy for MCQ type")
        @Description("Verifies that factory returns MCQ strategy when requesting MCQ question type")
        void shouldReturnCorrectStrategyForMcqType() {
            // Act
            QuestionTypeStrategy result = factory.getStrategy(QuestionType.MCQ);

            // Assert
            assertThat(result).isEqualTo(mcqStrategy);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should return correct strategy for Essay type")
        @Description("Verifies that factory returns Essay strategy when requesting Essay question type")
        void shouldReturnCorrectStrategyForEssayType() {
            // Act
            QuestionTypeStrategy result = factory.getStrategy(QuestionType.ESSAY);

            // Assert
            assertThat(result).isEqualTo(essayStrategy);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should return correct strategy for TrueFalse type")
        @Description("Verifies that factory returns True/False strategy when requesting True/False question type")
        void shouldReturnCorrectStrategyForTrueFalseType() {
            // Act
            QuestionTypeStrategy result = factory.getStrategy(QuestionType.TRUE_FALSE);

            // Assert
            assertThat(result).isEqualTo(trueFalseStrategy);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @BeforeEach
        void setUp() {
            // Only register MCQ strategy for error testing
            List<QuestionTypeStrategy> strategies = Arrays.asList(mcqStrategy);
            factory = new QuestionTypeStrategyFactory(strategies);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should throw exception for unsupported type")
        @Description("Verifies that factory throws UnsupportedQuestionTypeException when requesting unsupported type")
        void shouldThrowExceptionForUnsupportedType() {
            // Act & Assert
            assertThatThrownBy(() -> factory.getStrategy(QuestionType.ESSAY))
                .isInstanceOf(UnsupportedQuestionTypeException.class)
                .hasMessageContaining("No strategy found for question type: essay");
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should report unsupported type correctly")
        @Description("Verifies that isSupported method correctly identifies unsupported types")
        void shouldReportUnsupportedTypeCorrectly() {
            // Act & Assert
            assertThat(factory.isSupported(QuestionType.MCQ)).isTrue();
            assertThat(factory.isSupported(QuestionType.ESSAY)).isFalse();
            assertThat(factory.isSupported(QuestionType.TRUE_FALSE)).isFalse();
        }
    }

    @Nested
    @DisplayName("Factory Information Tests")
    class FactoryInformationTests {

        @BeforeEach
        void setUp() {
            List<QuestionTypeStrategy> strategies = Arrays.asList(mcqStrategy, essayStrategy);
            factory = new QuestionTypeStrategyFactory(strategies);
        }

        @Test
        @Epic("Use Case Upsert Question with Relation-Main Path")
        @Story("story-004.question-type-strategy-implementation")
        @DisplayName("QuestionTypeStrategyFactoryTest.Should return accurate supported types set")
        @Description("Verifies that getSupportedTypes returns accurate set of supported question types")
        void shouldReturnAccurateSupportedTypesSet() {
            // Act
            var supportedTypes = factory.getSupportedTypes();

            // Assert
            assertThat(supportedTypes).hasSize(2);
            assertThat(supportedTypes).containsExactlyInAnyOrder(QuestionType.MCQ, QuestionType.ESSAY);
            assertThat(supportedTypes).doesNotContain(QuestionType.TRUE_FALSE);
        }
    }
}