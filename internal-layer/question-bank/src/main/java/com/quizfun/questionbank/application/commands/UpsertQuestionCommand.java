package com.quizfun.questionbank.application.commands;

import com.quizfun.globalshared.mediator.ICommand;
import com.quizfun.questionbank.application.dto.QuestionResponseDto;
import com.quizfun.questionbank.application.dto.TaxonomyData;
import com.quizfun.questionbank.application.dto.UpsertQuestionRequestDto;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.TrueFalseData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UpsertQuestionCommand implements ICommand<QuestionResponseDto> {
    private final Long userId;
    private final Long questionBankId;
    private final String sourceQuestionId;
    private final QuestionType questionType;
    private final String title;
    private final String content;
    private final Integer points;
    private final String solutionExplanation;
    private final String status;
    private final Integer displayOrder;
    private final TaxonomyData taxonomy;
    private final McqData mcqData;
    private final EssayData essayData;
    private final TrueFalseData trueFalseData;
    private final UpsertQuestionRequestDto.QuestionSettings questionSettings;
    private final UpsertQuestionRequestDto.Metadata metadata;
    private final List<UpsertQuestionRequestDto.Attachment> attachments;

    public UpsertQuestionCommand(Long userId, Long questionBankId, UpsertQuestionRequestDto request) {
        // Validate required parameters
        this.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        this.questionBankId = Objects.requireNonNull(questionBankId, "Question Bank ID cannot be null");

        Objects.requireNonNull(request, "Request cannot be null");

        // Validate and set core fields
        this.sourceQuestionId = validateAndSetSourceQuestionId(request.getSourceQuestionId());
        this.questionType = validateAndSetQuestionType(request.getQuestionType());
        this.title = validateAndSetTitle(request.getTitle());
        this.content = validateAndSetContent(request.getContent());

        // Set optional fields
        this.points = request.getPoints();
        this.solutionExplanation = request.getSolutionExplanation();
        this.status = request.getStatus();
        this.displayOrder = request.getDisplayOrder();

        // Defensive copy of taxonomy to ensure immutability
        this.taxonomy = copyTaxonomy(request.getTaxonomy());

        // Set type-specific data
        this.mcqData = request.getMcqData();
        this.essayData = request.getEssayData();
        this.trueFalseData = request.getTrueFalseData();

        // Set additional data
        this.questionSettings = request.getQuestionSettings();
        this.metadata = request.getMetadata();

        // Defensive copy of attachments
        this.attachments = request.getAttachments() != null ?
            new ArrayList<>(request.getAttachments()) : new ArrayList<>();
    }

    private String validateAndSetSourceQuestionId(String sourceQuestionId) {
        if (sourceQuestionId == null || sourceQuestionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source question ID cannot be null or empty");
        }
        return sourceQuestionId.trim();
    }

    private QuestionType validateAndSetQuestionType(String questionType) {
        if (questionType == null || questionType.trim().isEmpty()) {
            throw new IllegalArgumentException("Question type cannot be null or empty");
        }

        try {
            return QuestionType.valueOf(questionType.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid question type: " + questionType);
        }
    }

    private String validateAndSetTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        return title.trim();
    }

    private String validateAndSetContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        return content.trim();
    }

    private TaxonomyData copyTaxonomy(TaxonomyData original) {
        if (original == null) {
            return null;
        }

        // Create defensive copies to ensure immutability
        TaxonomyData copy = new TaxonomyData();

        if (original.getCategories() != null) {
            copy.setCategories(copyCategories(original.getCategories()));
        }

        if (original.getTags() != null) {
            copy.setTags(new ArrayList<>(original.getTags()));
        }

        if (original.getQuizzes() != null) {
            copy.setQuizzes(new ArrayList<>(original.getQuizzes()));
        }

        copy.setDifficultyLevel(original.getDifficultyLevel());

        return copy;
    }

    private TaxonomyData.Categories copyCategories(TaxonomyData.Categories original) {
        TaxonomyData.Categories copy = new TaxonomyData.Categories();
        copy.setLevel1(original.getLevel1());
        copy.setLevel2(original.getLevel2());
        copy.setLevel3(original.getLevel3());
        copy.setLevel4(original.getLevel4());
        return copy;
    }

    /**
     * Extracts all taxonomy IDs from the taxonomy structure for validation purposes.
     * This includes category IDs, tag IDs, quiz IDs, and difficulty level.
     *
     * @return List of all taxonomy reference IDs that need to be validated
     */
    public List<String> extractTaxonomyIds() {
        List<String> ids = new ArrayList<>();

        if (taxonomy == null) {
            return ids;
        }

        // Extract category IDs from all levels
        if (taxonomy.getCategories() != null) {
            TaxonomyData.Categories categories = taxonomy.getCategories();

            if (categories.getLevel1() != null && categories.getLevel1().getId() != null) {
                ids.add(categories.getLevel1().getId());
            }
            if (categories.getLevel2() != null && categories.getLevel2().getId() != null) {
                ids.add(categories.getLevel2().getId());
            }
            if (categories.getLevel3() != null && categories.getLevel3().getId() != null) {
                ids.add(categories.getLevel3().getId());
            }
            if (categories.getLevel4() != null && categories.getLevel4().getId() != null) {
                ids.add(categories.getLevel4().getId());
            }
        }

        // Extract tag IDs
        if (taxonomy.getTags() != null) {
            taxonomy.getTags().stream()
                .filter(tag -> tag.getId() != null)
                .forEach(tag -> ids.add(tag.getId()));
        }

        // Extract quiz IDs
        if (taxonomy.getQuizzes() != null) {
            taxonomy.getQuizzes().stream()
                .filter(quiz -> quiz.getQuizId() != null)
                .forEach(quiz -> ids.add(quiz.getQuizId().toString()));
        }

        // Extract difficulty level
        if (taxonomy.getDifficultyLevel() != null && taxonomy.getDifficultyLevel().getLevel() != null) {
            ids.add(taxonomy.getDifficultyLevel().getLevel());
        }

        return ids;
    }

    // Getters for all fields
    public Long getUserId() {
        return userId;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Integer getPoints() {
        return points;
    }

    public String getSolutionExplanation() {
        return solutionExplanation;
    }

    public String getStatus() {
        return status;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public TaxonomyData getTaxonomy() {
        return taxonomy;
    }

    public McqData getMcqData() {
        return mcqData;
    }

    public EssayData getEssayData() {
        return essayData;
    }

    public TrueFalseData getTrueFalseData() {
        return trueFalseData;
    }

    public UpsertQuestionRequestDto.QuestionSettings getQuestionSettings() {
        return questionSettings;
    }

    public UpsertQuestionRequestDto.Metadata getMetadata() {
        return metadata;
    }

    public List<UpsertQuestionRequestDto.Attachment> getAttachments() {
        return new ArrayList<>(attachments); // Return defensive copy
    }
}