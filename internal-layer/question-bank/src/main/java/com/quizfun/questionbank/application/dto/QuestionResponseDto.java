package com.quizfun.questionbank.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Immutable response DTO for question upsert operations.
 * Contains comprehensive question information and operation metadata for API responses.
 *
 * JSON serialization is configured with snake_case property names for frontend compatibility.
 * All fields are final to ensure immutability and thread safety.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionResponseDto {

    @JsonProperty("question_id")
    private final String questionId;

    @JsonProperty("source_question_id")
    private final String sourceQuestionId;

    @JsonProperty("operation")
    private final String operation;

    @JsonProperty("taxonomy_relationships_count")
    private final Integer taxonomyRelationshipsCount;

    @JsonProperty("question_type")
    private final String questionType;

    @JsonProperty("title")
    private final String title;

    @JsonProperty("content")
    private final String content;

    @JsonProperty("points")
    private final Integer points;

    @JsonProperty("status")
    private final String status;

    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant createdAt;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private final Instant updatedAt;

    /**
     * Constructor using builder pattern (private - use builder() method).
     */
    private QuestionResponseDto(Builder builder) {
        this.questionId = builder.questionId;
        this.sourceQuestionId = builder.sourceQuestionId;
        this.operation = builder.operation;
        this.taxonomyRelationshipsCount = builder.taxonomyRelationshipsCount;
        this.questionType = builder.questionType;
        this.title = builder.title;
        this.content = builder.content;
        this.points = builder.points;
        this.status = builder.status;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    /**
     * Creates a new builder instance.
     * @return Builder for constructing QuestionResponseDto instances
     */
    public static Builder builder() {
        return new Builder();
    }

    // Getters (no setters - immutable object)
    public String getQuestionId() {
        return questionId;
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public String getOperation() {
        return operation;
    }

    public Integer getTaxonomyRelationshipsCount() {
        return taxonomyRelationshipsCount;
    }

    public String getQuestionType() {
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

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Builder pattern for constructing QuestionResponseDto instances.
     * Provides validation and flexible construction options.
     */
    public static class Builder {
        private String questionId;
        private String sourceQuestionId;
        private String operation;
        private Integer taxonomyRelationshipsCount;
        private String questionType;
        private String title;
        private String content;
        private Integer points;
        private String status;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder questionId(String questionId) {
            this.questionId = questionId;
            return this;
        }

        public Builder sourceQuestionId(String sourceQuestionId) {
            this.sourceQuestionId = sourceQuestionId;
            return this;
        }

        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public Builder taxonomyRelationshipsCount(Integer count) {
            this.taxonomyRelationshipsCount = count;
            return this;
        }

        public Builder questionType(String questionType) {
            this.questionType = questionType;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder points(Integer points) {
            this.points = points;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Builds the QuestionResponseDto with validation.
         * @return Immutable QuestionResponseDto instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public QuestionResponseDto build() {
            validateRequiredFields();
            return new QuestionResponseDto(this);
        }

        /**
         * Validates that required fields are present.
         * @throws IllegalArgumentException if validation fails
         */
        private void validateRequiredFields() {
            if (questionId == null || questionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Question ID is required");
            }
            if (sourceQuestionId == null || sourceQuestionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Source question ID is required");
            }
            if (operation == null || operation.trim().isEmpty()) {
                throw new IllegalArgumentException("Operation type is required");
            }
        }
    }

    @Override
    public String toString() {
        return "QuestionResponseDto{" +
                "questionId='" + questionId + '\'' +
                ", sourceQuestionId='" + sourceQuestionId + '\'' +
                ", operation='" + operation + '\'' +
                ", taxonomyRelationshipsCount=" + taxonomyRelationshipsCount +
                ", questionType='" + questionType + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}