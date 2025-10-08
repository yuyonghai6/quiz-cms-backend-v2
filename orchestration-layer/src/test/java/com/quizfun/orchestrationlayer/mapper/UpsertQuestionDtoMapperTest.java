package com.quizfun.orchestrationlayer.mapper;

import com.quizfun.orchestrationlayer.dto.*;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UpsertQuestionDtoMapper.
 * Validates mapping between HTTP DTOs and internal command DTOs.
 */
@DisplayName("UpsertQuestionDtoMapperTest")
@Epic("Orchestration Layer")
@Story("DTO Mapping")
class UpsertQuestionDtoMapperTest {

    private UpsertQuestionDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UpsertQuestionDtoMapper();
    }

    @Nested
    @DisplayName("Basic Field Mapping Tests")
    class BasicFieldMappingTests {

        @Test
        @DisplayName("Should map all basic fields correctly")
        @Description("Validates that basic question fields are mapped from HTTP DTO to internal DTO")
        void shouldMapBasicFieldsCorrectly() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            httpDto.setSourceQuestionId("q123");
            httpDto.setQuestionType("mcq");
            httpDto.setTitle("Sample Question");
            httpDto.setContent("What is 2+2?");
            httpDto.setPoints(5);
            httpDto.setSolutionExplanation("2+2 equals 4");
            httpDto.setStatus("published");
            httpDto.setDisplayOrder(1);

            Instant now = Instant.now();
            httpDto.setCreatedAt(now);
            httpDto.setUpdatedAt(now);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getSourceQuestionId()).isEqualTo("q123");
            assertThat(result.getQuestionType()).isEqualTo("mcq");
            assertThat(result.getTitle()).isEqualTo("Sample Question");
            assertThat(result.getContent()).isEqualTo("What is 2+2?");
            assertThat(result.getPoints()).isEqualTo(5);
            assertThat(result.getSolutionExplanation()).isEqualTo("2+2 equals 4");
            assertThat(result.getStatus()).isEqualTo("published");
            assertThat(result.getDisplayOrder()).isEqualTo(1);
            assertThat(result.getCreatedAt()).isEqualTo(now);
            assertThat(result.getUpdatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should handle null optional fields gracefully")
        @Description("Validates that mapper handles null values for optional fields")
        void shouldHandleNullOptionalFields() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            httpDto.setQuestionType("mcq");
            // All other fields null

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getQuestionType()).isEqualTo("mcq");
            assertThat(result.getTaxonomy()).isNull();
            assertThat(result.getMcqData()).isNull();
            assertThat(result.getEssayData()).isNull();
            assertThat(result.getTrueFalseData()).isNull();
        }
    }

    @Nested
    @DisplayName("MCQ Data Mapping Tests")
    class McqDataMappingTests {

        @Test
        @DisplayName("Should map MCQ data with multiple options")
        @Description("Validates mapping of MCQ question with options, shuffle, and settings")
        void shouldMapMcqDataWithMultipleOptions() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            httpDto.setQuestionType("mcq");

            var mcqData = new McqHttpDto();
            mcqData.setShuffleOptions(true);
            mcqData.setAllowMultipleCorrect(true);
            mcqData.setAllowPartialCredit(true);
            mcqData.setTimeLimitSeconds(60);

            var option1 = new McqHttpDto.McqOptionHttpDto();
            option1.setId(1);
            option1.setText("Option A");
            option1.setIsCorrect(true);

            var option2 = new McqHttpDto.McqOptionHttpDto();
            option2.setId(2);
            option2.setText("Option B");
            option2.setIsCorrect(false);

            mcqData.setOptions(Arrays.asList(option1, option2));
            httpDto.setMcqData(mcqData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getMcqData()).isNotNull();
            McqData mapped = result.getMcqData();
            assertThat(mapped.isShuffleOptions()).isTrue();
            assertThat(mapped.isAllowMultipleCorrect()).isTrue();
            assertThat(mapped.isAllowPartialCredit()).isTrue();
            assertThat(mapped.getTimeLimitSeconds()).isEqualTo(60);
            assertThat(mapped.getOptions()).hasSize(2);
            assertThat(mapped.getOptions().get(0).getText()).isEqualTo("Option A");
            assertThat(mapped.getOptions().get(0).isCorrect()).isTrue();
            assertThat(mapped.getOptions().get(1).getText()).isEqualTo("Option B");
            assertThat(mapped.getOptions().get(1).isCorrect()).isFalse();
        }

        @Test
        @DisplayName("Should use defaults for null MCQ boolean fields")
        @Description("Validates that null boolean fields in MCQ data default to false")
        void shouldUseDefaultsForNullMcqBooleans() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var mcqData = new McqHttpDto();

            // Create at least one option (required by domain validation)
            var option = new McqHttpDto.McqOptionHttpDto();
            option.setId(1);
            option.setText("Test Option");
            option.setIsCorrect(true);
            mcqData.setOptions(Collections.singletonList(option));

            // shuffleOptions, allowMultipleCorrect, allowPartialCredit all null
            httpDto.setMcqData(mcqData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            McqData mapped = result.getMcqData();
            assertThat(mapped.isShuffleOptions()).isFalse();
            assertThat(mapped.isAllowMultipleCorrect()).isFalse();
            assertThat(mapped.isAllowPartialCredit()).isFalse();
        }

        @Test
        @DisplayName("Should handle MCQ option with null ID")
        @Description("Validates that MCQ option without ID gets default ID")
        void shouldHandleMcqOptionWithNullId() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var mcqData = new McqHttpDto();

            var option = new McqHttpDto.McqOptionHttpDto();
            option.setId(null);  // Null ID
            option.setText("Test Option");
            option.setIsCorrect(true);

            mcqData.setOptions(Collections.singletonList(option));
            httpDto.setMcqData(mcqData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getMcqData().getOptions().get(0).getKey()).isEqualTo("1");
        }
    }

    @Nested
    @DisplayName("Essay Data Mapping Tests")
    class EssayDataMappingTests {

        @Test
        @DisplayName("Should map essay data with rubric")
        @Description("Validates mapping of essay question with rubric criteria and points")
        void shouldMapEssayDataWithRubric() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            httpDto.setQuestionType("essay");

            var essayData = new EssayHttpDto();
            essayData.setMinWords(100);
            essayData.setMaxWords(500);
            essayData.setAllowFileUpload(true);

            var rubricItem1 = new EssayHttpDto.RubricItemHttpDto();
            rubricItem1.setCriteria("Grammar and spelling");
            rubricItem1.setMaxPoints(10);

            var rubricItem2 = new EssayHttpDto.RubricItemHttpDto();
            rubricItem2.setCriteria("Content quality");
            rubricItem2.setMaxPoints(20);

            essayData.setRubric(Arrays.asList(rubricItem1, rubricItem2));
            httpDto.setEssayData(essayData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getEssayData()).isNotNull();
            EssayData mapped = result.getEssayData();
            assertThat(mapped.getMinWordCount()).isEqualTo(100);
            assertThat(mapped.getMaxWordCount()).isEqualTo(500);
            assertThat(mapped.isAllowRichText()).isTrue();
            assertThat(mapped.getRubric()).isNotNull();
            assertThat(mapped.getRubric().getCriteria()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle essay without rubric")
        @Description("Validates essay mapping when rubric is null or empty")
        void shouldHandleEssayWithoutRubric() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var essayData = new EssayHttpDto();
            essayData.setMinWords(50);
            essayData.setMaxWords(200);
            essayData.setRubric(null);
            httpDto.setEssayData(essayData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            EssayData mapped = result.getEssayData();
            assertThat(mapped.getRubric()).isNull();
        }

        @Test
        @DisplayName("Should default allowFileUpload to false when null")
        @Description("Validates that null allowFileUpload defaults to false")
        void shouldDefaultAllowFileUploadToFalse() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var essayData = new EssayHttpDto();
            essayData.setMinWords(100);
            essayData.setAllowFileUpload(null);
            httpDto.setEssayData(essayData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getEssayData().isAllowRichText()).isFalse();
        }
    }

    @Nested
    @DisplayName("True/False Data Mapping Tests")
    class TrueFalseDataMappingTests {

        @Test
        @DisplayName("Should map true/false data correctly")
        @Description("Validates mapping of true/false question with answer and explanation")
        void shouldMapTrueFalseDataCorrectly() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            httpDto.setQuestionType("true_false");

            var trueFalseData = new TrueFalseHttpDto();
            trueFalseData.setCorrectAnswer(true);
            trueFalseData.setExplanation("This statement is true because...");
            httpDto.setTrueFalseData(trueFalseData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getTrueFalseData()).isNotNull();
            TrueFalseData mapped = result.getTrueFalseData();
            assertThat(mapped.getCorrectAnswer()).isTrue();
            assertThat(mapped.getExplanation()).isEqualTo("This statement is true because...");
            assertThat(mapped.getTimeLimitSeconds()).isNull();
        }

        @Test
        @DisplayName("Should default correctAnswer to false when null")
        @Description("Validates that null correctAnswer defaults to false")
        void shouldDefaultCorrectAnswerToFalse() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var trueFalseData = new TrueFalseHttpDto();
            trueFalseData.setCorrectAnswer(null);
            httpDto.setTrueFalseData(trueFalseData);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getTrueFalseData().getCorrectAnswer()).isFalse();
        }
    }

    @Nested
    @DisplayName("Taxonomy Mapping Tests")
    class TaxonomyMappingTests {

        @Test
        @DisplayName("Should map complete taxonomy with all levels")
        @Description("Validates mapping of taxonomy with categories, tags, quizzes, and difficulty")
        void shouldMapCompleteTaxonomy() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();

            var taxonomy = new TaxonomyHttpDto();

            // Categories
            var categories = new TaxonomyHttpDto.CategoriesHttpDto();
            var cat1 = new TaxonomyHttpDto.CategoryHttpDto();
            cat1.setId("cat1");
            cat1.setName("Mathematics");
            cat1.setSlug("mathematics");
            cat1.setParentId(null);
            categories.setLevel1(cat1);

            var cat2 = new TaxonomyHttpDto.CategoryHttpDto();
            cat2.setId("cat2");
            cat2.setName("Algebra");
            cat2.setSlug("algebra");
            cat2.setParentId("cat1");
            categories.setLevel2(cat2);

            taxonomy.setCategories(categories);

            // Tags
            var tag1 = new TaxonomyHttpDto.TagHttpDto();
            tag1.setId("tag1");
            tag1.setName("Equations");
            tag1.setColor("#FF0000");
            taxonomy.setTags(Collections.singletonList(tag1));

            // Quizzes
            var quiz1 = new TaxonomyHttpDto.QuizHttpDto();
            quiz1.setQuizId(123L);
            quiz1.setQuizName("Midterm Exam");
            quiz1.setQuizSlug("midterm-exam");
            taxonomy.setQuizzes(Collections.singletonList(quiz1));

            // Difficulty
            var difficulty = new TaxonomyHttpDto.DifficultyLevelHttpDto();
            difficulty.setLevel("MEDIUM");
            difficulty.setNumericValue(5);
            difficulty.setDescription("Medium difficulty");
            taxonomy.setDifficultyLevel(difficulty);

            httpDto.setTaxonomy(taxonomy);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getTaxonomy()).isNotNull();
            var mappedTaxonomy = result.getTaxonomy();

            assertThat(mappedTaxonomy.getCategories()).isNotNull();
            assertThat(mappedTaxonomy.getCategories().getLevel1().getId()).isEqualTo("cat1");
            assertThat(mappedTaxonomy.getCategories().getLevel2().getId()).isEqualTo("cat2");

            assertThat(mappedTaxonomy.getTags()).hasSize(1);
            assertThat(mappedTaxonomy.getTags().get(0).getId()).isEqualTo("tag1");

            assertThat(mappedTaxonomy.getQuizzes()).hasSize(1);
            assertThat(mappedTaxonomy.getQuizzes().get(0).getQuizId()).isEqualTo(123L);

            assertThat(mappedTaxonomy.getDifficultyLevel()).isNotNull();
            assertThat(mappedTaxonomy.getDifficultyLevel().getLevel()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("Should map all category levels")
        @Description("Validates mapping of all 4 category levels")
        void shouldMapAllCategoryLevels() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var taxonomy = new TaxonomyHttpDto();
            var categories = new TaxonomyHttpDto.CategoriesHttpDto();

            var cat1 = new TaxonomyHttpDto.CategoryHttpDto();
            cat1.setId("c1");
            cat1.setName("Level 1");
            categories.setLevel1(cat1);

            var cat2 = new TaxonomyHttpDto.CategoryHttpDto();
            cat2.setId("c2");
            cat2.setName("Level 2");
            categories.setLevel2(cat2);

            var cat3 = new TaxonomyHttpDto.CategoryHttpDto();
            cat3.setId("c3");
            cat3.setName("Level 3");
            categories.setLevel3(cat3);

            var cat4 = new TaxonomyHttpDto.CategoryHttpDto();
            cat4.setId("c4");
            cat4.setName("Level 4");
            categories.setLevel4(cat4);

            taxonomy.setCategories(categories);
            httpDto.setTaxonomy(taxonomy);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            var mappedCategories = result.getTaxonomy().getCategories();
            assertThat(mappedCategories.getLevel1().getId()).isEqualTo("c1");
            assertThat(mappedCategories.getLevel2().getId()).isEqualTo("c2");
            assertThat(mappedCategories.getLevel3().getId()).isEqualTo("c3");
            assertThat(mappedCategories.getLevel4().getId()).isEqualTo("c4");
        }

        @Test
        @DisplayName("Should handle null taxonomy sections")
        @Description("Validates that null sections within taxonomy don't cause errors")
        void shouldHandleNullTaxonomySections() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var taxonomy = new TaxonomyHttpDto();
            taxonomy.setCategories(null);
            taxonomy.setTags(null);
            taxonomy.setQuizzes(null);
            taxonomy.setDifficultyLevel(null);
            httpDto.setTaxonomy(taxonomy);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getTaxonomy()).isNotNull();
            assertThat(result.getTaxonomy().getCategories()).isNull();
            assertThat(result.getTaxonomy().getTags()).isNull();
            assertThat(result.getTaxonomy().getQuizzes()).isNull();
            assertThat(result.getTaxonomy().getDifficultyLevel()).isNull();
        }
    }

    @Nested
    @DisplayName("Attachment Mapping Tests")
    class AttachmentMappingTests {

        @Test
        @DisplayName("Should map attachments correctly")
        @Description("Validates mapping of multiple attachments with all fields")
        void shouldMapAttachmentsCorrectly() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();

            var attachment1 = new AttachmentHttpDto();
            attachment1.setId("att1");
            attachment1.setType("image");
            attachment1.setFilename("diagram.png");
            attachment1.setUrl("https://example.com/diagram.png");
            attachment1.setSize(102400L);
            attachment1.setMimeType("image/png");

            var attachment2 = new AttachmentHttpDto();
            attachment2.setId("att2");
            attachment2.setType("document");
            attachment2.setFilename("reference.pdf");
            attachment2.setUrl("https://example.com/reference.pdf");
            attachment2.setSize(204800L);
            attachment2.setMimeType("application/pdf");

            httpDto.setAttachments(Arrays.asList(attachment1, attachment2));

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getAttachments()).hasSize(2);
            var mapped1 = result.getAttachments().get(0);
            assertThat(mapped1.getId()).isEqualTo("att1");
            assertThat(mapped1.getType()).isEqualTo("image");
            assertThat(mapped1.getFilename()).isEqualTo("diagram.png");
            assertThat(mapped1.getSize()).isEqualTo(102400L);
        }
    }

    @Nested
    @DisplayName("Question Settings and Metadata Mapping Tests")
    class SettingsAndMetadataMappingTests {

        @Test
        @DisplayName("Should map question settings correctly")
        @Description("Validates mapping of question display and behavior settings")
        void shouldMapQuestionSettingsCorrectly() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var settings = new QuestionSettingsHttpDto();
            settings.setRandomizeDisplay(true);
            settings.setShowExplanationImmediately(false);
            settings.setAllowReview(true);
            httpDto.setQuestionSettings(settings);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            var mappedSettings = result.getQuestionSettings();
            assertThat(mappedSettings.getRandomizeDisplay()).isTrue();
            assertThat(mappedSettings.getShowExplanationImmediately()).isFalse();
            assertThat(mappedSettings.getAllowReview()).isTrue();
        }

        @Test
        @DisplayName("Should map metadata correctly")
        @Description("Validates mapping of question metadata fields")
        void shouldMapMetadataCorrectly() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            var metadata = new QuestionMetadataHttpDto();
            metadata.setCreatedSource("web-ui");
            metadata.setLastModified("2025-10-08T10:00:00Z");
            metadata.setVersion(2);
            metadata.setAuthorId(1001L);
            httpDto.setMetadata(metadata);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            var mappedMetadata = result.getMetadata();
            assertThat(mappedMetadata.getCreatedSource()).isEqualTo("web-ui");
            assertThat(mappedMetadata.getLastModified()).isEqualTo("2025-10-08T10:00:00Z");
            assertThat(mappedMetadata.getVersion()).isEqualTo(2);
            assertThat(mappedMetadata.getAuthorId()).isEqualTo(1001L);
        }
    }

    @Nested
    @DisplayName("Timestamp Mapping Tests")
    class TimestampMappingTests {

        @Test
        @DisplayName("Should map all timestamp fields")
        @Description("Validates mapping of created, updated, published, and archived timestamps")
        void shouldMapAllTimestampFields() {
            // Arrange
            var httpDto = new UpsertQuestionHttpRequestDto();
            Instant created = Instant.parse("2025-10-01T10:00:00Z");
            Instant updated = Instant.parse("2025-10-05T15:30:00Z");
            Instant published = Instant.parse("2025-10-06T09:00:00Z");
            Instant archived = Instant.parse("2025-10-07T18:00:00Z");

            httpDto.setCreatedAt(created);
            httpDto.setUpdatedAt(updated);
            httpDto.setPublishedAt(published);
            httpDto.setArchivedAt(archived);

            // Act
            UpsertQuestionRequestDto result = mapper.mapToInternal(httpDto);

            // Assert
            assertThat(result.getCreatedAt()).isEqualTo(created);
            assertThat(result.getUpdatedAt()).isEqualTo(updated);
            assertThat(result.getPublishedAt()).isEqualTo(published);
            assertThat(result.getArchivedAt()).isEqualTo(archived);
        }
    }
}
