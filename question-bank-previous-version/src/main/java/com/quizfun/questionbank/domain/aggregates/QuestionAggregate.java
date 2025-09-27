package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.shared.domain.AggregateRoot;
import com.quizfun.questionbank.domain.entities.*;
import com.quizfun.questionbank.domain.events.QuestionCreatedEvent;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class QuestionAggregate extends AggregateRoot {

    // Core immutable properties
    private ObjectId id;
    private Long userId;
    private Long questionBankId;
    private String sourceQuestionId;
    private QuestionType questionType;

    // Modifiable content properties
    private String title;
    private String content;
    private Integer points;
    private String status;
    private Integer displayOrder;
    private String solutionExplanation;

    // Type-specific data
    private McqData mcqData;
    private EssayData essayData;
    private TrueFalseData trueFalseData;

    // Audit fields (managed by AggregateRoot base class)
    private Instant publishedAt;
    private Instant archivedAt;

    // Private constructor to enforce factory methods
    private QuestionAggregate() {}

    public static QuestionAggregate createNew(Long userId, Long questionBankId,
                                            String sourceQuestionId, QuestionType questionType,
                                            String title, String content, Integer points) {
        var aggregate = new QuestionAggregate();

        // Validate required fields first (fail fast)
        aggregate.validateRequiredFields(userId, questionBankId, sourceQuestionId, questionType, title, content, points);

        // Set core immutable properties
        aggregate.id = new ObjectId(); // Generate new ObjectId
        aggregate.userId = userId;
        aggregate.questionBankId = questionBankId;
        aggregate.sourceQuestionId = sourceQuestionId.trim();
        aggregate.questionType = questionType;

        // Set content properties with trimming
        aggregate.title = title.trim();
        aggregate.content = content.trim();
        aggregate.points = points != null ? points : 0;
        aggregate.status = "draft";

        // Initialize audit timestamps using base class
        aggregate.markCreatedNow();

        // Generate domain event with UUID v7
        String eventAggregateId = com.quizfun.globalshared.utils.UUIDv7Generator.generateAsString();
        aggregate.addDomainEvent(new QuestionCreatedEvent(
            eventAggregateId,
            aggregate.sourceQuestionId,
            aggregate.questionType,
            aggregate.userId,
            aggregate.questionBankId
        ));

        return aggregate;
    }

    private void validateRequiredFields(Long userId, Long questionBankId,
                                      String sourceQuestionId, QuestionType questionType,
                                      String title, String content, Integer points) {
        // Validate userId
        if (userId == null || userId <= 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Please provide a valid user ID");
        }

        // Validate questionBankId
        if (questionBankId == null || questionBankId <= 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Please provide a valid question bank ID");
        }

        // Validate sourceQuestionId (must be UUID v7)
        if (sourceQuestionId == null || sourceQuestionId.trim().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidSourceQuestionIdException("Source question ID is required");
        }
        if (!com.quizfun.globalshared.utils.UUIDv7Generator.isValidUUIDv7(sourceQuestionId.trim())) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidSourceQuestionIdException("Source question ID must be a valid UUID v7 format");
        }

        // Validate questionType
        if (questionType == null) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question type is required");
        }

        // Validate title (max 255 characters)
        if (title == null || title.trim().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question title is required");
        }
        if (title.trim().length() > 255) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question title cannot exceed 255 characters");
        }

        // Validate content (max 4000 characters)
        if (content == null || content.trim().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question content is required");
        }
        if (content.trim().length() > 4000) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question content cannot exceed 4,000 characters");
        }

        // Validate points (cannot be negative)
        if (points != null && points < 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Points cannot be negative");
        }
    }

    // Immutable property getters (no setters for these)
    public ObjectId getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQuestionBankId() { return questionBankId; }
    public String getSourceQuestionId() { return sourceQuestionId; }
    public QuestionType getQuestionType() { return questionType; }

    // Modifiable property getters
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Integer getPoints() { return points; }
    public String getStatus() { return status; }
    public Integer getDisplayOrder() { return displayOrder; }
    public String getSolutionExplanation() { return solutionExplanation; }

    // Type-specific data getters
    public McqData getMcqData() { return mcqData; }
    public EssayData getEssayData() { return essayData; }
    public TrueFalseData getTrueFalseData() { return trueFalseData; }

    // Audit field getters
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getArchivedAt() { return archivedAt; }

    // Update methods for modifiable content properties
    public void updateBasicContent(String title, String content, Integer points) {
        validateContentUpdate(title, content, points);

        // Track which fields are being updated
        var updatedFields = new java.util.ArrayList<String>();

        if (!this.title.equals(title.trim())) {
            this.title = title.trim();
            updatedFields.add("title");
        }
        if (!this.content.equals(content.trim())) {
            this.content = content.trim();
            updatedFields.add("content");
        }

        Integer newPoints = points != null ? points : 0;
        if (!this.points.equals(newPoints)) {
            this.points = newPoints;
            updatedFields.add("points");
        }

        // Only generate event and update timestamp if something actually changed
        if (!updatedFields.isEmpty()) {
            markUpdatedNow();
            generateUpdateEvent(updatedFields);
        }
    }

    public void updateStatusAndMetadata(String status, Integer displayOrder, String solutionExplanation) {
        var updatedFields = new java.util.ArrayList<String>();

        if (status != null && !status.equals(this.status)) {
            this.status = status;
            updatedFields.add("status");
        }
        if (displayOrder != null && !java.util.Objects.equals(this.displayOrder, displayOrder)) {
            this.displayOrder = displayOrder;
            updatedFields.add("displayOrder");
        }
        if (solutionExplanation != null && !java.util.Objects.equals(this.solutionExplanation, solutionExplanation)) {
            this.solutionExplanation = solutionExplanation;
            updatedFields.add("solutionExplanation");
        }

        // Only generate event and update timestamp if something actually changed
        if (!updatedFields.isEmpty()) {
            markUpdatedNow();
            generateUpdateEvent(updatedFields);
        }
    }

    private void validateContentUpdate(String title, String content, Integer points) {
        // Validate title (max 255 characters)
        if (title == null || title.trim().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question title is required");
        }
        if (title.trim().length() > 255) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question title cannot exceed 255 characters");
        }

        // Validate content (max 4000 characters)
        if (content == null || content.trim().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question content is required");
        }
        if (content.trim().length() > 4000) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Question content cannot exceed 4,000 characters");
        }

        // Validate points (cannot be negative)
        if (points != null && points < 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException("Points cannot be negative");
        }
    }

    private void generateUpdateEvent(java.util.List<String> updatedFields) {
        // Generate domain event with UUID v7
        String eventAggregateId = com.quizfun.globalshared.utils.UUIDv7Generator.generateAsString();
        addDomainEvent(new com.quizfun.questionbank.domain.events.QuestionUpdatedEvent(
            eventAggregateId,
            this.sourceQuestionId,
            this.questionType,
            this.userId,
            this.questionBankId,
            updatedFields
        ));
    }

    // Type-specific data management methods
    public void setMcqData(McqData mcqData) {
        if (mcqData != null && this.questionType != QuestionType.MCQ) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "MCQ data can only be set on MCQ questions");
        }

        if (mcqData != null) {
            validateMcqData(mcqData);
        }

        boolean hasChanged = !java.util.Objects.equals(this.mcqData, mcqData);
        if (hasChanged) {
            this.mcqData = mcqData;
            // Maintain exclusivity when setting non-null MCQ data
            if (mcqData != null) {
                this.essayData = null;
                this.trueFalseData = null;
            }
            markUpdatedNow();
            generateUpdateEvent(java.util.List.of("mcqData"));
        }
    }

    public void setEssayData(EssayData essayData) {
        if (essayData != null && this.questionType != QuestionType.ESSAY) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "Essay data can only be set on Essay questions");
        }

        if (essayData != null) {
            validateEssayData(essayData);
        }

        boolean hasChanged = !java.util.Objects.equals(this.essayData, essayData);
        if (hasChanged) {
            this.essayData = essayData;
            // Maintain exclusivity when setting non-null Essay data
            if (essayData != null) {
                this.mcqData = null;
                this.trueFalseData = null;
            }
            markUpdatedNow();
            generateUpdateEvent(java.util.List.of("essayData"));
        }
    }

    public void setTrueFalseData(TrueFalseData trueFalseData) {
        if (trueFalseData != null && this.questionType != QuestionType.TRUE_FALSE) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "True/False data can only be set on True/False questions");
        }

        boolean hasChanged = !java.util.Objects.equals(this.trueFalseData, trueFalseData);
        if (hasChanged) {
            this.trueFalseData = trueFalseData;
            // Maintain exclusivity when setting non-null True/False data
            if (trueFalseData != null) {
                this.mcqData = null;
                this.essayData = null;
            }
            markUpdatedNow();
            generateUpdateEvent(java.util.List.of("trueFalseData"));
        }
    }


    private void validateMcqData(McqData mcqData) {
        if (mcqData.getOptions() == null || mcqData.getOptions().isEmpty()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "MCQ questions must have at least one option");
        }

        long correctAnswersCount = mcqData.getOptions().stream()
            .filter(McqOption::isCorrect)
            .count();

        if (correctAnswersCount == 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "MCQ questions must have at least one correct answer");
        }

        // Note: McqData doesn't have maxSelections, but has built-in validation
        // The validation is already handled in McqData constructor
    }

    private void validateEssayData(EssayData essayData) {
        if (essayData.getMinWordCount() != null && essayData.getMaxWordCount() != null
            && essayData.getMinWordCount() > essayData.getMaxWordCount()) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "Minimum word count cannot be greater than maximum word count");
        }

        if (essayData.getMinWordCount() != null && essayData.getMinWordCount() < 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "Minimum word count cannot be negative");
        }

        if (essayData.getMaxWordCount() != null && essayData.getMaxWordCount() < 0) {
            throw new com.quizfun.questionbank.domain.exceptions.InvalidQuestionDataException(
                "Maximum word count cannot be negative");
        }
    }

    public boolean hasValidTypeSpecificData() {
        return switch (this.questionType) {
            case MCQ -> this.mcqData != null && this.essayData == null && this.trueFalseData == null;
            case ESSAY -> this.essayData != null && this.mcqData == null && this.trueFalseData == null;
            case TRUE_FALSE -> this.trueFalseData != null && this.mcqData == null && this.essayData == null;
        };
    }

    // Lifecycle methods
    public void publish() {
        if (!hasValidTypeSpecificData()) {
            throw new IllegalStateException("Cannot publish question without valid type-specific data");
        }
        if ("archived".equals(this.status)) {
            throw new IllegalStateException("Cannot publish archived question");
        }

        this.status = "published";
        this.publishedAt = Instant.now();
        markUpdatedNow();

        String eventAggregateId = com.quizfun.globalshared.utils.UUIDv7Generator.generateAsString();
        this.addDomainEvent(new com.quizfun.questionbank.domain.events.QuestionPublishedEvent(
            eventAggregateId,
            this.sourceQuestionId,
            this.userId,
            this.questionBankId
        ));
    }

    public void archive() {
        this.status = "archived";
        this.archivedAt = Instant.now();
        markUpdatedNow();

        String eventAggregateId = com.quizfun.globalshared.utils.UUIDv7Generator.generateAsString();
        this.addDomainEvent(new com.quizfun.questionbank.domain.events.QuestionArchivedEvent(
            eventAggregateId,
            this.sourceQuestionId
        ));
    }

    public boolean isNew() {
        return this.id == null;
    }

    public boolean isDraft() {
        return "draft".equals(this.status);
    }

    public boolean isPublished() {
        return "published".equals(this.status);
    }

    public boolean isArchived() {
        return "archived".equals(this.status);
    }

    public boolean belongsToUser(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public boolean belongsToQuestionBank(Long questionBankId) {
        return this.questionBankId != null && this.questionBankId.equals(questionBankId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        QuestionAggregate that = (QuestionAggregate) obj;
        return java.util.Objects.equals(userId, that.userId) &&
               java.util.Objects.equals(questionBankId, that.questionBankId) &&
               java.util.Objects.equals(sourceQuestionId, that.sourceQuestionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId, questionBankId, sourceQuestionId);
    }
}