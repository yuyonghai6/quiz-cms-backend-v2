package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

/**
 * HTTP request DTO for question upsert operations.
 * This DTO handles JSON deserialization and validation for the REST API layer.
 * It is separate from the internal command DTOs to maintain clean architecture.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpsertQuestionHttpRequestDto {

    @NotBlank(message = "Source question ID is required")
    @Size(max = 100, message = "Source question ID cannot exceed 100 characters")
    @JsonProperty("source_question_id")
    private String sourceQuestionId;

    @NotBlank(message = "Question type is required")
    @Pattern(regexp = "^(mcq|essay|true_false)$", message = "Question type must be mcq, essay, or true_false")
    @JsonProperty("question_type")
    private String questionType;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content cannot exceed 10000 characters")
    private String content;

    @NotNull(message = "Taxonomy data is required")
    @Valid
    private TaxonomyHttpDto taxonomy;

    @Min(value = 0, message = "Points cannot be negative")
    @Max(value = 1000, message = "Points cannot exceed 1000")
    private Integer points;

    @Size(max = 2000, message = "Solution explanation cannot exceed 2000 characters")
    @JsonProperty("solution_explanation")
    private String solutionExplanation;

    @Pattern(regexp = "^(draft|published|archived)$", message = "Status must be draft, published, or archived")
    private String status;

    @Min(value = 1, message = "Display order must be positive")
    @JsonProperty("display_order")
    private Integer displayOrder;

    @Valid
    private List<AttachmentHttpDto> attachments;

    @Valid
    @JsonProperty("mcq_data")
    private McqHttpDto mcqData;

    @Valid
    @JsonProperty("essay_data")
    private EssayHttpDto essayData;

    @Valid
    @JsonProperty("true_false_data")
    private TrueFalseHttpDto trueFalseData;

    @Valid
    @JsonProperty("question_settings")
    private QuestionSettingsHttpDto questionSettings;

    @Valid
    private QuestionMetadataHttpDto metadata;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("published_at")
    private Instant publishedAt;

    @JsonProperty("archived_at")
    private Instant archivedAt;

    public UpsertQuestionHttpRequestDto() {}

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Custom validation method to ensure type-specific data matches question type
     */
    @AssertTrue(message = "Type-specific data must match question type")
    public boolean isTypeSpecificDataValid() {
        if (questionType == null) {
            return true; // Let @NotBlank handle this
        }

        switch (questionType.toLowerCase()) {
            case "mcq":
                return mcqData != null && essayData == null && trueFalseData == null;
            case "essay":
                return essayData != null && mcqData == null && trueFalseData == null;
            case "true_false":
                return trueFalseData != null && mcqData == null && essayData == null;
            default:
                return false;
        }
    }

    public static class Builder {
        private final UpsertQuestionHttpRequestDto dto = new UpsertQuestionHttpRequestDto();

        public Builder sourceQuestionId(String sourceQuestionId) {
            dto.sourceQuestionId = sourceQuestionId;
            return this;
        }

        public Builder questionType(String questionType) {
            dto.questionType = questionType;
            return this;
        }

        public Builder title(String title) {
            dto.title = title;
            return this;
        }

        public Builder content(String content) {
            dto.content = content;
            return this;
        }

        public Builder taxonomy(TaxonomyHttpDto taxonomy) {
            dto.taxonomy = taxonomy;
            return this;
        }

        public Builder points(Integer points) {
            dto.points = points;
            return this;
        }

        public Builder status(String status) {
            dto.status = status;
            return this;
        }

        public Builder mcqData(McqHttpDto mcqData) {
            dto.mcqData = mcqData;
            return this;
        }

        public Builder essayData(EssayHttpDto essayData) {
            dto.essayData = essayData;
            return this;
        }

        public Builder trueFalseData(TrueFalseHttpDto trueFalseData) {
            dto.trueFalseData = trueFalseData;
            return this;
        }

        public UpsertQuestionHttpRequestDto build() {
            return dto;
        }
    }

    // Getters and setters
    public String getSourceQuestionId() { return sourceQuestionId; }
    public void setSourceQuestionId(String sourceQuestionId) { this.sourceQuestionId = sourceQuestionId; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public TaxonomyHttpDto getTaxonomy() { return taxonomy; }
    public void setTaxonomy(TaxonomyHttpDto taxonomy) { this.taxonomy = taxonomy; }

    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }

    public String getSolutionExplanation() { return solutionExplanation; }
    public void setSolutionExplanation(String solutionExplanation) { this.solutionExplanation = solutionExplanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public List<AttachmentHttpDto> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentHttpDto> attachments) { this.attachments = attachments; }

    public McqHttpDto getMcqData() { return mcqData; }
    public void setMcqData(McqHttpDto mcqData) { this.mcqData = mcqData; }

    public EssayHttpDto getEssayData() { return essayData; }
    public void setEssayData(EssayHttpDto essayData) { this.essayData = essayData; }

    public TrueFalseHttpDto getTrueFalseData() { return trueFalseData; }
    public void setTrueFalseData(TrueFalseHttpDto trueFalseData) { this.trueFalseData = trueFalseData; }

    public QuestionSettingsHttpDto getQuestionSettings() { return questionSettings; }
    public void setQuestionSettings(QuestionSettingsHttpDto questionSettings) { this.questionSettings = questionSettings; }

    public QuestionMetadataHttpDto getMetadata() { return metadata; }
    public void setMetadata(QuestionMetadataHttpDto metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
}