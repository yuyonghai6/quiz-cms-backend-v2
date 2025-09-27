package com.quizfun.questionbank.application.commands;

import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.QuestionType;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class UpsertQuestionCommandTest {

    @Nested
    @DisplayName("Command Creation Tests")
    class CommandCreationTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should create command with valid data")
        @Description("Verifies that UpsertQuestionCommand can be created successfully with all required valid data")
        void shouldCreateCommandWithValidData() {
            // Arrange
            var request = createValidRequest();

            // Act
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Assert
            assertThat(command.getUserId()).isEqualTo(1001L);
            assertThat(command.getQuestionBankId()).isEqualTo(2002L);
            assertThat(command.getSourceQuestionId()).isEqualTo("Q123");
            assertThat(command.getQuestionType()).isEqualTo(QuestionType.MCQ);
            assertThat(command.getTitle()).isEqualTo("Sample MCQ Question");
            assertThat(command.getContent()).isEqualTo("What is 2+2?");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should set default values for optional fields")
        @Description("Verifies that optional fields are handled correctly when not provided in the request")
        void shouldSetDefaultValuesForOptionalFields() {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .build();

            // Act
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Assert
            assertThat(command.getPoints()).isNull();
            assertThat(command.getStatus()).isNull();
            assertThat(command.getDisplayOrder()).isNull();
            assertThat(command.getSolutionExplanation()).isNull();
            assertThat(command.getAttachments()).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should reject invalid source question IDs")
        @Description("Validates that command creation fails with appropriate error for invalid source question IDs")
        void shouldRejectInvalidSourceQuestionIds(String invalidSourceId) {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId(invalidSourceId)
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .build();

            // Act & Assert
            assertThatThrownBy(() ->
                new UpsertQuestionCommand(1001L, 2002L, request)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("Source question ID cannot be null or empty");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   ", "INVALID_TYPE"})
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should reject invalid question types")
        @Description("Validates that command creation fails with appropriate error for invalid question types")
        void shouldRejectInvalidQuestionTypes(String invalidType) {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType(invalidType)
                .title("Test Question")
                .content("Test Content")
                .build();

            // Act & Assert
            assertThatThrownBy(() ->
                new UpsertQuestionCommand(1001L, 2002L, request)
            ).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should reject null user ID")
        @Description("Validates that command creation fails when user ID is null")
        void shouldRejectNullUserId() {
            // Arrange
            var request = createValidRequest();

            // Act & Assert
            assertThatThrownBy(() ->
                new UpsertQuestionCommand(null, 2002L, request)
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("User ID cannot be null");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should reject null question bank ID")
        @Description("Validates that command creation fails when question bank ID is null")
        void shouldRejectNullQuestionBankId() {
            // Arrange
            var request = createValidRequest();

            // Act & Assert
            assertThatThrownBy(() ->
                new UpsertQuestionCommand(1001L, null, request)
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("Question Bank ID cannot be null");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should reject null request")
        @Description("Validates that command creation fails when request DTO is null")
        void shouldRejectNullRequest() {
            // Act & Assert
            assertThatThrownBy(() ->
                new UpsertQuestionCommand(1001L, 2002L, null)
            ).isInstanceOf(NullPointerException.class)
             .hasMessageContaining("Request cannot be null");
        }
    }

    @Nested
    @DisplayName("Taxonomy Extraction Tests")
    class TaxonomyExtractionTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should extract all taxonomy IDs correctly")
        @Description("Verifies that all taxonomy IDs are extracted from the complete taxonomy structure")
        void shouldExtractAllTaxonomyIdsCorrectly() {
            // Arrange
            var request = createRequestWithCompleteTaxonomy();
            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var taxonomyIds = command.extractTaxonomyIds();

            // Assert
            assertThat(taxonomyIds).contains(
                "tech", "prog", "web_dev", "javascript", // Category IDs
                "js-arrays", "array-methods",             // Tag IDs
                "101", "205",                            // Quiz IDs
                "easy"                                   // Difficulty level
            );
            assertThat(taxonomyIds).hasSize(9);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should handle partial taxonomy structure")
        @Description("Verifies taxonomy extraction works correctly with partially filled taxonomy structure")
        void shouldHandlePartialTaxonomyStructure() {
            // Arrange
            var categories = new TaxonomyData.Categories();
            categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "tech", null));
            // Level 2, 3, 4 are null

            var taxonomy = new TaxonomyData();
            taxonomy.setCategories(categories);
            taxonomy.setTags(List.of(new TaxonomyData.Tag("js-arrays", "JavaScript", "#f7df1e")));
            taxonomy.setQuizzes(new ArrayList<>());
            taxonomy.setDifficultyLevel(new TaxonomyData.DifficultyLevel("easy", 1, "Easy"));

            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .taxonomy(taxonomy)
                .build();

            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var taxonomyIds = command.extractTaxonomyIds();

            // Assert
            assertThat(taxonomyIds).containsExactlyInAnyOrder("tech", "js-arrays", "easy");
            assertThat(taxonomyIds).hasSize(3);
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should return empty list for null taxonomy")
        @Description("Verifies that empty list is returned when taxonomy is null")
        void shouldReturnEmptyListForNullTaxonomy() {
            // Arrange
            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .taxonomy(null)
                .build();

            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var taxonomyIds = command.extractTaxonomyIds();

            // Assert
            assertThat(taxonomyIds).isEmpty();
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should handle empty taxonomy collections")
        @Description("Verifies that empty collections in taxonomy structure are handled correctly")
        void shouldHandleEmptyTaxonomyCollections() {
            // Arrange
            var taxonomy = new TaxonomyData();
            taxonomy.setCategories(new TaxonomyData.Categories()); // All levels are null
            taxonomy.setTags(new ArrayList<>()); // Empty list
            taxonomy.setQuizzes(new ArrayList<>()); // Empty list
            taxonomy.setDifficultyLevel(null); // Null difficulty

            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .taxonomy(taxonomy)
                .build();

            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act
            var taxonomyIds = command.extractTaxonomyIds();

            // Assert
            assertThat(taxonomyIds).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability Tests")
    class ImmutabilityTests {

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should enforce immutability of taxonomy data")
        @Description("Validates that original taxonomy data cannot affect the command after creation")
        void shouldEnforceImmutabilityOfTaxonomyData() {
            // Arrange
            var originalTags = new ArrayList<TaxonomyData.Tag>();
            originalTags.add(new TaxonomyData.Tag("js-arrays", "JavaScript", "#f7df1e"));

            var taxonomy = new TaxonomyData();
            taxonomy.setTags(originalTags);

            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .taxonomy(taxonomy)
                .build();

            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act - Attempt to modify original tags
            originalTags.clear();

            // Assert - Command should be unaffected
            assertThat(command.getTaxonomy().getTags()).isNotEmpty();
            assertThat(command.extractTaxonomyIds()).contains("js-arrays");
        }

        @Test
        @Epic("Enabler Epic-Core Infrastructure")
        @Story("story-003.validation-chain-implementation")
        @DisplayName("UpsertQuestionCommandTest.Should return defensive copy of attachments")
        @Description("Validates that attachments returned by command are defensive copies")
        void shouldReturnDefensiveCopyOfAttachments() {
            // Arrange
            var attachments = new ArrayList<UpsertQuestionRequestDto.Attachment>();
            attachments.add(new UpsertQuestionRequestDto.Attachment("att1", "image", "test.png", "/path", 1000L, "image/png"));

            var request = UpsertQuestionRequestDto.builder()
                .sourceQuestionId("Q123")
                .questionType("MCQ")
                .title("Test Question")
                .content("Test Content")
                .attachments(attachments)
                .build();

            var command = new UpsertQuestionCommand(1001L, 2002L, request);

            // Act - Modify returned attachments
            var returnedAttachments = command.getAttachments();
            returnedAttachments.clear();

            // Assert - Original data should be preserved
            assertThat(command.getAttachments()).hasSize(1);
        }
    }

    private UpsertQuestionRequestDto createValidRequest() {
        return UpsertQuestionRequestDto.builder()
            .sourceQuestionId("Q123")
            .questionType("MCQ")
            .title("Sample MCQ Question")
            .content("What is 2+2?")
            .build();
    }

    private UpsertQuestionRequestDto createRequestWithCompleteTaxonomy() {
        // Create categories
        var categories = new TaxonomyData.Categories();
        categories.setLevel1(new TaxonomyData.Category("tech", "Technology", "tech", null));
        categories.setLevel2(new TaxonomyData.Category("prog", "Programming", "prog", "tech"));
        categories.setLevel3(new TaxonomyData.Category("web_dev", "Web Development", "web_dev", "prog"));
        categories.setLevel4(new TaxonomyData.Category("javascript", "JavaScript", "javascript", "web_dev"));

        // Create tags
        var tags = List.of(
            new TaxonomyData.Tag("js-arrays", "JavaScript Arrays", "#f7df1e"),
            new TaxonomyData.Tag("array-methods", "Array Methods", "#61dafb")
        );

        // Create quizzes
        var quizzes = List.of(
            new TaxonomyData.Quiz(101L, "JS Fundamentals", "js-fundamentals"),
            new TaxonomyData.Quiz(205L, "Array Methods", "array-methods")
        );

        // Create difficulty level
        var difficultyLevel = new TaxonomyData.DifficultyLevel("easy", 1, "Easy level");

        // Create taxonomy
        var taxonomy = new TaxonomyData(categories, tags, quizzes, difficultyLevel);

        return UpsertQuestionRequestDto.builder()
            .sourceQuestionId("Q123")
            .questionType("MCQ")
            .title("Sample MCQ Question")
            .content("What is 2+2?")
            .taxonomy(taxonomy)
            .build();
    }
}