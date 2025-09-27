package com.quizfun.orchestrationlayer.mapper;

import com.quizfun.orchestrationlayer.dto.*;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.EssayRubric;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper to transform between HTTP DTOs and internal command DTOs.
 * This maintains clean separation between HTTP layer and business logic layer.
 */
@Component
public class UpsertQuestionDtoMapper {

    /**
     * Maps HTTP request DTO to internal request DTO for command processing.
     *
     * @param httpDto The HTTP request DTO from REST API
     * @return Internal request DTO for command processing
     */
    public UpsertQuestionRequestDto mapToInternal(UpsertQuestionHttpRequestDto httpDto) {
        var internalDto = new UpsertQuestionRequestDto();

        // Map basic fields
        internalDto.setSourceQuestionId(httpDto.getSourceQuestionId());
        internalDto.setQuestionType(httpDto.getQuestionType());
        internalDto.setTitle(httpDto.getTitle());
        internalDto.setContent(httpDto.getContent());
        internalDto.setPoints(httpDto.getPoints());
        internalDto.setSolutionExplanation(httpDto.getSolutionExplanation());
        internalDto.setStatus(httpDto.getStatus());
        internalDto.setDisplayOrder(httpDto.getDisplayOrder());

        // Map taxonomy
        if (httpDto.getTaxonomy() != null) {
            internalDto.setTaxonomy(mapTaxonomy(httpDto.getTaxonomy()));
        }

        // Map type-specific data
        if (httpDto.getMcqData() != null) {
            internalDto.setMcqData(mapMcqData(httpDto.getMcqData()));
        }

        if (httpDto.getEssayData() != null) {
            internalDto.setEssayData(mapEssayData(httpDto.getEssayData()));
        }

        if (httpDto.getTrueFalseData() != null) {
            internalDto.setTrueFalseData(mapTrueFalseData(httpDto.getTrueFalseData()));
        }

        // Map attachments
        if (httpDto.getAttachments() != null) {
            internalDto.setAttachments(mapAttachments(httpDto.getAttachments()));
        }

        // Map question settings
        if (httpDto.getQuestionSettings() != null) {
            internalDto.setQuestionSettings(mapQuestionSettings(httpDto.getQuestionSettings()));
        }

        // Map metadata
        if (httpDto.getMetadata() != null) {
            internalDto.setMetadata(mapMetadata(httpDto.getMetadata()));
        }

        // Map timestamps
        internalDto.setCreatedAt(httpDto.getCreatedAt());
        internalDto.setUpdatedAt(httpDto.getUpdatedAt());
        internalDto.setPublishedAt(httpDto.getPublishedAt());
        internalDto.setArchivedAt(httpDto.getArchivedAt());

        return internalDto;
    }

    private TaxonomyData mapTaxonomy(TaxonomyHttpDto httpTaxonomy) {
        var taxonomy = new TaxonomyData();

        if (httpTaxonomy.getCategories() != null) {
            taxonomy.setCategories(mapCategories(httpTaxonomy.getCategories()));
        }

        if (httpTaxonomy.getTags() != null) {
            taxonomy.setTags(httpTaxonomy.getTags().stream()
                .map(this::mapTag)
                .collect(Collectors.toList()));
        }

        if (httpTaxonomy.getQuizzes() != null) {
            taxonomy.setQuizzes(httpTaxonomy.getQuizzes().stream()
                .map(this::mapQuiz)
                .collect(Collectors.toList()));
        }

        if (httpTaxonomy.getDifficultyLevel() != null) {
            taxonomy.setDifficultyLevel(mapDifficultyLevel(httpTaxonomy.getDifficultyLevel()));
        }

        return taxonomy;
    }

    private TaxonomyData.Categories mapCategories(TaxonomyHttpDto.CategoriesHttpDto httpCategories) {
        var categories = new TaxonomyData.Categories();

        if (httpCategories.getLevel1() != null) {
            categories.setLevel1(mapCategory(httpCategories.getLevel1()));
        }
        if (httpCategories.getLevel2() != null) {
            categories.setLevel2(mapCategory(httpCategories.getLevel2()));
        }
        if (httpCategories.getLevel3() != null) {
            categories.setLevel3(mapCategory(httpCategories.getLevel3()));
        }
        if (httpCategories.getLevel4() != null) {
            categories.setLevel4(mapCategory(httpCategories.getLevel4()));
        }

        return categories;
    }

    private TaxonomyData.Category mapCategory(TaxonomyHttpDto.CategoryHttpDto httpCategory) {
        var category = new TaxonomyData.Category();
        category.setId(httpCategory.getId());
        category.setName(httpCategory.getName());
        category.setSlug(httpCategory.getSlug());
        category.setParentId(httpCategory.getParentId());
        return category;
    }

    private TaxonomyData.Tag mapTag(TaxonomyHttpDto.TagHttpDto httpTag) {
        var tag = new TaxonomyData.Tag();
        tag.setId(httpTag.getId());
        tag.setName(httpTag.getName());
        tag.setColor(httpTag.getColor());
        return tag;
    }

    private TaxonomyData.Quiz mapQuiz(TaxonomyHttpDto.QuizHttpDto httpQuiz) {
        var quiz = new TaxonomyData.Quiz();
        quiz.setQuizId(httpQuiz.getQuizId());
        quiz.setQuizName(httpQuiz.getQuizName());
        quiz.setQuizSlug(httpQuiz.getQuizSlug());
        return quiz;
    }

    private TaxonomyData.DifficultyLevel mapDifficultyLevel(TaxonomyHttpDto.DifficultyLevelHttpDto httpDifficulty) {
        var difficulty = new TaxonomyData.DifficultyLevel();
        difficulty.setLevel(httpDifficulty.getLevel());
        difficulty.setNumericValue(httpDifficulty.getNumericValue());
        difficulty.setDescription(httpDifficulty.getDescription());
        return difficulty;
    }

    private McqData mapMcqData(McqHttpDto httpMcq) {
        List<McqOption> options = new ArrayList<>();

        if (httpMcq.getOptions() != null) {
            options = httpMcq.getOptions().stream()
                .map(this::mapMcqOption)
                .collect(Collectors.toList());
        }

        return new McqData(
            options,
            httpMcq.getShuffleOptions() != null ? httpMcq.getShuffleOptions() : false,
            httpMcq.getAllowMultipleCorrect() != null ? httpMcq.getAllowMultipleCorrect() : false,
            httpMcq.getAllowPartialCredit() != null ? httpMcq.getAllowPartialCredit() : false,
            httpMcq.getTimeLimitSeconds()
        );
    }

    private McqOption mapMcqOption(McqHttpDto.McqOptionHttpDto httpOption) {
        return new McqOption(
            httpOption.getId() != null ? httpOption.getId().toString() : "1",
            httpOption.getText(),
            httpOption.getIsCorrect() != null ? httpOption.getIsCorrect() : false,
            1.0 // Default points
        );
    }

    private EssayData mapEssayData(EssayHttpDto httpEssay) {
        EssayRubric rubric = null;

        if (httpEssay.getRubric() != null && !httpEssay.getRubric().isEmpty()) {
            List<String> criteria = httpEssay.getRubric().stream()
                .map(EssayHttpDto.RubricItemHttpDto::getCriteria)
                .collect(Collectors.toList());

            Integer totalPoints = httpEssay.getRubric().stream()
                .mapToInt(item -> item.getMaxPoints() != null ? item.getMaxPoints() : 0)
                .sum();

            rubric = new EssayRubric(
                "Rubric for essay question",
                criteria,
                totalPoints > 0 ? totalPoints : null
            );
        }

        return new EssayData(
            httpEssay.getMinWords(),
            httpEssay.getMaxWords(),
            httpEssay.getAllowFileUpload() != null ? httpEssay.getAllowFileUpload() : false,
            rubric
        );
    }

    private TrueFalseData mapTrueFalseData(TrueFalseHttpDto httpTrueFalse) {
        return new TrueFalseData(
            httpTrueFalse.getCorrectAnswer() != null ? httpTrueFalse.getCorrectAnswer() : false,
            httpTrueFalse.getExplanation(),
            null // No time limit in the HTTP DTO structure
        );
    }

    private List<UpsertQuestionRequestDto.Attachment> mapAttachments(List<AttachmentHttpDto> httpAttachments) {
        return httpAttachments.stream()
            .map(this::mapAttachment)
            .collect(Collectors.toList());
    }

    private UpsertQuestionRequestDto.Attachment mapAttachment(AttachmentHttpDto httpAttachment) {
        return new UpsertQuestionRequestDto.Attachment(
            httpAttachment.getId(),
            httpAttachment.getType(),
            httpAttachment.getFilename(),
            httpAttachment.getUrl(),
            httpAttachment.getSize(),
            httpAttachment.getMimeType()
        );
    }

    private UpsertQuestionRequestDto.QuestionSettings mapQuestionSettings(QuestionSettingsHttpDto httpSettings) {
        return new UpsertQuestionRequestDto.QuestionSettings(
            httpSettings.getRandomizeDisplay(),
            httpSettings.getShowExplanationImmediately(),
            httpSettings.getAllowReview()
        );
    }

    private UpsertQuestionRequestDto.Metadata mapMetadata(QuestionMetadataHttpDto httpMetadata) {
        return new UpsertQuestionRequestDto.Metadata(
            httpMetadata.getCreatedSource(),
            httpMetadata.getLastModified(),
            httpMetadata.getVersion(),
            httpMetadata.getAuthorId()
        );
    }
}