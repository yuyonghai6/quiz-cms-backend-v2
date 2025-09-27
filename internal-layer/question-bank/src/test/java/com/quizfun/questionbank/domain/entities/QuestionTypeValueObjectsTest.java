package com.quizfun.questionbank.domain.entities;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class QuestionTypeValueObjectsTest {

    @Nested
    @DisplayName("QuestionType Enumeration Tests")
    class QuestionTypeTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should provide all required question types")
        @Description("Validates that QuestionType enum contains all supported question types")
        void shouldProvideAllRequiredQuestionTypes() {
            // This test will fail initially - QuestionType doesn't exist yet
            assertThat(QuestionType.values()).hasSize(3);
            assertThat(QuestionType.MCQ).isNotNull();
            assertThat(QuestionType.ESSAY).isNotNull();
            assertThat(QuestionType.TRUE_FALSE).isNotNull();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should provide string values for each type")
        @Description("Ensures each QuestionType has a corresponding string value for persistence")
        void shouldProvideStringValuesForEachType() {
            assertThat(QuestionType.MCQ.getValue()).isEqualTo("mcq");
            assertThat(QuestionType.ESSAY.getValue()).isEqualTo("essay");
            assertThat(QuestionType.TRUE_FALSE.getValue()).isEqualTo("true_false");
        }
    }

    @Nested
    @DisplayName("MCQ Data Tests")
    class McqDataTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should create valid MCQ data with correct options")
        @Description("Validates McqData creation with proper option validation")
        void shouldCreateValidMcqDataWithCorrectOptions() {
            var options = List.of(
                new McqOption("A", "Option A", true, 1.0),
                new McqOption("B", "Option B", false, 0.0)
            );

            var mcqData = new McqData(options, true, false, false, 300);

            assertThat(mcqData.getOptions()).hasSize(2);
            assertThat(mcqData.isShuffleOptions()).isTrue();
            assertThat(mcqData.isAllowMultipleCorrect()).isFalse();
            assertThat(mcqData.isAllowPartialCredit()).isFalse();
            assertThat(mcqData.getTimeLimitSeconds()).isEqualTo(300);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject MCQ data without correct answers")
        @Description("Ensures McqData validation rejects questions without any correct answers")
        void shouldRejectMcqDataWithoutCorrectAnswers() {
            var options = List.of(
                new McqOption("A", "Option A", false, 0.0),
                new McqOption("B", "Option B", false, 0.0)
            );

            assertThatThrownBy(() ->
                new McqData(options, true, false, false, 300)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("must have at least one correct answer");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should be immutable value object")
        @Description("Verifies McqData immutability by testing defensive copying")
        void shouldBeImmutableValueObject() {
            var options = new ArrayList<>(List.of(
                new McqOption("A", "Option A", true, 1.0)
            ));

            var mcqData = new McqData(options, true, false, false, 300);
            options.clear(); // Modify original list

            assertThat(mcqData.getOptions()).hasSize(1); // Should be unaffected
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject null or empty options")
        @Description("Validates McqData constructor properly handles null and empty option lists")
        void shouldRejectNullOrEmptyOptions() {
            assertThatThrownBy(() ->
                new McqData(null, true, false, false, 300)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("MCQ must have at least one option");

            assertThatThrownBy(() ->
                new McqData(List.of(), true, false, false, 300)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("MCQ must have at least one option");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should implement equals and hashCode correctly")
        @Description("Verifies McqData value object equality based on content, not identity")
        void shouldImplementEqualsAndHashCodeCorrectly() {
            var options1 = List.of(
                new McqOption("A", "Option A", true, 1.0)
            );
            var options2 = List.of(
                new McqOption("A", "Option A", true, 1.0)
            );

            var mcqData1 = new McqData(options1, true, false, false, 300);
            var mcqData2 = new McqData(options2, true, false, false, 300);
            var mcqData3 = new McqData(options1, false, false, false, 300);

            assertThat(mcqData1).isEqualTo(mcqData2);
            assertThat(mcqData1.hashCode()).isEqualTo(mcqData2.hashCode());
            assertThat(mcqData1).isNotEqualTo(mcqData3);
        }
    }

    @Nested
    @DisplayName("Essay Data Tests")
    class EssayDataTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should create valid essay data with rubric")
        @Description("Validates EssayData creation with word count limits and rubric")
        void shouldCreateValidEssayDataWithRubric() {
            var rubric = new EssayRubric(
                "Demonstrates understanding",
                List.of("Grammar", "Content", "Structure"),
                1000
            );

            var essayData = new EssayData(
                1000, 5000, true, rubric
            );

            assertThat(essayData.getMinWordCount()).isEqualTo(1000);
            assertThat(essayData.getMaxWordCount()).isEqualTo(5000);
            assertThat(essayData.isAllowRichText()).isTrue();
            assertThat(essayData.getRubric()).isEqualTo(rubric);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject invalid word count ranges")
        @Description("Ensures EssayData validation rejects min > max word count scenarios")
        void shouldRejectInvalidWordCountRanges() {
            assertThatThrownBy(() ->
                new EssayData(5000, 1000, true, null) // min > max
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("minimum word count cannot exceed maximum");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should handle null rubric gracefully")
        @Description("Validates EssayData works correctly with null rubric for simple essays")
        void shouldHandleNullRubricGracefully() {
            var essayData = new EssayData(100, 500, false, null);

            assertThat(essayData.getRubric()).isNull();
            assertThat(essayData.getMinWordCount()).isEqualTo(100);
            assertThat(essayData.getMaxWordCount()).isEqualTo(500);
            assertThat(essayData.isAllowRichText()).isFalse();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject negative word counts")
        @Description("Ensures EssayData validation prevents negative word count values")
        void shouldRejectNegativeWordCounts() {
            assertThatThrownBy(() ->
                new EssayData(-1, 1000, true, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("word count cannot be negative");

            assertThatThrownBy(() ->
                new EssayData(100, -1, true, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("word count cannot be negative");
        }
    }

    @Nested
    @DisplayName("True/False Data Tests")
    class TrueFalseDataTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should create valid true/false data")
        @Description("Validates TrueFalseData creation with correct answer and explanation")
        void shouldCreateValidTrueFalseData() {
            var trueFalseData = new TrueFalseData(
                true,
                "This is true because...",
                300
            );

            assertThat(trueFalseData.getCorrectAnswer()).isTrue();
            assertThat(trueFalseData.getExplanation()).isEqualTo("This is true because...");
            assertThat(trueFalseData.getTimeLimitSeconds()).isEqualTo(300);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should handle null explanation")
        @Description("Validates TrueFalseData works correctly without explanation text")
        void shouldHandleNullExplanation() {
            var trueFalseData = new TrueFalseData(false, null, 60);

            assertThat(trueFalseData.getCorrectAnswer()).isFalse();
            assertThat(trueFalseData.getExplanation()).isNull();
            assertThat(trueFalseData.getTimeLimitSeconds()).isEqualTo(60);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should implement value equality")
        @Description("Verifies TrueFalseData equality based on values, not object identity")
        void shouldImplementValueEquality() {
            var data1 = new TrueFalseData(true, "Explanation", 300);
            var data2 = new TrueFalseData(true, "Explanation", 300);
            var data3 = new TrueFalseData(false, "Explanation", 300);

            assertThat(data1).isEqualTo(data2);
            assertThat(data1.hashCode()).isEqualTo(data2.hashCode());
            assertThat(data1).isNotEqualTo(data3);
        }
    }

    @Nested
    @DisplayName("Supporting Value Objects Tests")
    class SupportingValueObjectsTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should create valid MCQ option")
        @Description("Validates McqOption creation with all required properties")
        void shouldCreateValidMcqOption() {
            var option = new McqOption("A", "Option A text", true, 1.0);

            assertThat(option.getKey()).isEqualTo("A");
            assertThat(option.getText()).isEqualTo("Option A text");
            assertThat(option.isCorrect()).isTrue();
            assertThat(option.getPoints()).isEqualTo(1.0);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject invalid MCQ option data")
        @Description("Ensures McqOption validation prevents invalid key or text values")
        void shouldRejectInvalidMcqOptionData() {
            assertThatThrownBy(() ->
                new McqOption(null, "Text", true, 1.0)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("key cannot be null or empty");

            assertThatThrownBy(() ->
                new McqOption("A", "", true, 1.0)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("text cannot be null or empty");

            assertThatThrownBy(() ->
                new McqOption("A", "Text", true, -1.0)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("points cannot be negative");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should create valid essay rubric")
        @Description("Validates EssayRubric creation with criteria and max points")
        void shouldCreateValidEssayRubric() {
            var criteria = List.of("Grammar", "Content", "Structure");
            var rubric = new EssayRubric(
                "Evaluation guidelines",
                criteria,
                100
            );

            assertThat(rubric.getDescription()).isEqualTo("Evaluation guidelines");
            assertThat(rubric.getCriteria()).hasSize(3);
            assertThat(rubric.getMaxPoints()).isEqualTo(100);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-002.question-domain-aggregate-implementation")
        @DisplayName("QuestionTypeValueObjectsTest.Should reject invalid essay rubric data")
        @Description("Ensures EssayRubric validation prevents invalid max points or empty criteria")
        void shouldRejectInvalidEssayRubricData() {
            assertThatThrownBy(() ->
                new EssayRubric("Description", null, 100)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("criteria cannot be null or empty");

            assertThatThrownBy(() ->
                new EssayRubric("Description", List.of(), 100)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("criteria cannot be null or empty");

            assertThatThrownBy(() ->
                new EssayRubric("Description", List.of("Grammar"), -1)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("maxPoints cannot be negative");
        }
    }
}