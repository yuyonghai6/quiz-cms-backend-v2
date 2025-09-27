package com.quizfun.questionbank.application.dto;

import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.TrueFalseData;

import java.time.Instant;
import java.util.List;

public class UpsertQuestionRequestDto {
    private String sourceQuestionId;
    private String questionType;
    private String title;
    private String content;
    private TaxonomyData taxonomy;
    private Integer points;
    private String solutionExplanation;
    private String status;
    private Integer displayOrder;
    private List<Attachment> attachments;
    private McqData mcqData;
    private EssayData essayData;
    private TrueFalseData trueFalseData;
    private QuestionSettings questionSettings;
    private Metadata metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant archivedAt;

    public UpsertQuestionRequestDto() {}

    public static Builder builder() {
        return new Builder();
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(String sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TaxonomyData getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(TaxonomyData taxonomy) {
        this.taxonomy = taxonomy;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getSolutionExplanation() {
        return solutionExplanation;
    }

    public void setSolutionExplanation(String solutionExplanation) {
        this.solutionExplanation = solutionExplanation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public McqData getMcqData() {
        return mcqData;
    }

    public void setMcqData(McqData mcqData) {
        this.mcqData = mcqData;
    }

    public EssayData getEssayData() {
        return essayData;
    }

    public void setEssayData(EssayData essayData) {
        this.essayData = essayData;
    }

    public TrueFalseData getTrueFalseData() {
        return trueFalseData;
    }

    public void setTrueFalseData(TrueFalseData trueFalseData) {
        this.trueFalseData = trueFalseData;
    }

    public QuestionSettings getQuestionSettings() {
        return questionSettings;
    }

    public void setQuestionSettings(QuestionSettings questionSettings) {
        this.questionSettings = questionSettings;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    public static class Builder {
        private UpsertQuestionRequestDto dto;

        public Builder() {
            this.dto = new UpsertQuestionRequestDto();
        }

        public Builder sourceQuestionId(String sourceQuestionId) {
            this.dto.sourceQuestionId = sourceQuestionId;
            return this;
        }

        public Builder questionType(String questionType) {
            this.dto.questionType = questionType;
            return this;
        }

        public Builder title(String title) {
            this.dto.title = title;
            return this;
        }

        public Builder content(String content) {
            this.dto.content = content;
            return this;
        }

        public Builder taxonomy(TaxonomyData taxonomy) {
            this.dto.taxonomy = taxonomy;
            return this;
        }

        public Builder points(Integer points) {
            this.dto.points = points;
            return this;
        }

        public Builder solutionExplanation(String solutionExplanation) {
            this.dto.solutionExplanation = solutionExplanation;
            return this;
        }

        public Builder status(String status) {
            this.dto.status = status;
            return this;
        }

        public Builder displayOrder(Integer displayOrder) {
            this.dto.displayOrder = displayOrder;
            return this;
        }

        public Builder attachments(List<Attachment> attachments) {
            this.dto.attachments = attachments;
            return this;
        }

        public Builder mcqData(McqData mcqData) {
            this.dto.mcqData = mcqData;
            return this;
        }

        public Builder essayData(EssayData essayData) {
            this.dto.essayData = essayData;
            return this;
        }

        public Builder trueFalseData(TrueFalseData trueFalseData) {
            this.dto.trueFalseData = trueFalseData;
            return this;
        }

        public Builder questionSettings(QuestionSettings questionSettings) {
            this.dto.questionSettings = questionSettings;
            return this;
        }

        public Builder metadata(Metadata metadata) {
            this.dto.metadata = metadata;
            return this;
        }

        public UpsertQuestionRequestDto build() {
            return this.dto;
        }
    }

    public static class Attachment {
        private String id;
        private String type;
        private String filename;
        private String url;
        private Long size;
        private String mimeType;

        public Attachment() {}

        public Attachment(String id, String type, String filename, String url, Long size, String mimeType) {
            this.id = id;
            this.type = type;
            this.filename = filename;
            this.url = url;
            this.size = size;
            this.mimeType = mimeType;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    }

    public static class QuestionSettings {
        private Boolean randomizeDisplay;
        private Boolean showExplanationImmediately;
        private Boolean allowReview;

        public QuestionSettings() {}

        public QuestionSettings(Boolean randomizeDisplay, Boolean showExplanationImmediately, Boolean allowReview) {
            this.randomizeDisplay = randomizeDisplay;
            this.showExplanationImmediately = showExplanationImmediately;
            this.allowReview = allowReview;
        }

        public Boolean getRandomizeDisplay() { return randomizeDisplay; }
        public void setRandomizeDisplay(Boolean randomizeDisplay) { this.randomizeDisplay = randomizeDisplay; }
        public Boolean getShowExplanationImmediately() { return showExplanationImmediately; }
        public void setShowExplanationImmediately(Boolean showExplanationImmediately) { this.showExplanationImmediately = showExplanationImmediately; }
        public Boolean getAllowReview() { return allowReview; }
        public void setAllowReview(Boolean allowReview) { this.allowReview = allowReview; }
    }

    public static class Metadata {
        private String createdSource;
        private String lastModified;
        private Integer version;
        private Long authorId;

        public Metadata() {}

        public Metadata(String createdSource, String lastModified, Integer version, Long authorId) {
            this.createdSource = createdSource;
            this.lastModified = lastModified;
            this.version = version;
            this.authorId = authorId;
        }

        public String getCreatedSource() { return createdSource; }
        public void setCreatedSource(String createdSource) { this.createdSource = createdSource; }
        public String getLastModified() { return lastModified; }
        public void setLastModified(String lastModified) { this.lastModified = lastModified; }
        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        public Long getAuthorId() { return authorId; }
        public void setAuthorId(Long authorId) { this.authorId = authorId; }
    }
}