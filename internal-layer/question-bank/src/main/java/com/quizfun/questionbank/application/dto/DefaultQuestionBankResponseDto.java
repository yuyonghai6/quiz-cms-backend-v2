package com.quizfun.questionbank.application.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Response DTO for default question bank creation.
 * Contains all information about the created question bank and taxonomy.
 */
public class DefaultQuestionBankResponseDto {

    private final Long userId;
    private final Long questionBankId;
    private final String questionBankName;
    private final String description;
    private final boolean isActive;
    private final boolean taxonomySetCreated;
    private final Map<String, Object> availableTaxonomy;
    private final Instant createdAt;

    private DefaultQuestionBankResponseDto(Builder builder) {
        this.userId = builder.userId;
        this.questionBankId = builder.questionBankId;
        this.questionBankName = builder.questionBankName;
        this.description = builder.description;
        this.isActive = builder.isActive;
        this.taxonomySetCreated = builder.taxonomySetCreated;
        this.availableTaxonomy = builder.availableTaxonomy != null
            ? Map.copyOf(builder.availableTaxonomy)
            : Map.of();
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public Long getUserId() {
        return userId;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }

    public String getQuestionBankName() {
        return questionBankName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isTaxonomySetCreated() {
        return taxonomySetCreated;
    }

    public Map<String, Object> getAvailableTaxonomy() {
        return availableTaxonomy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private Long userId;
        private Long questionBankId;
        private String questionBankName;
        private String description;
        private boolean isActive;
        private boolean taxonomySetCreated;
        private Map<String, Object> availableTaxonomy;
        private Instant createdAt;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder questionBankId(Long questionBankId) {
            this.questionBankId = questionBankId;
            return this;
        }

        public Builder questionBankName(String questionBankName) {
            this.questionBankName = questionBankName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public Builder taxonomySetCreated(boolean taxonomySetCreated) {
            this.taxonomySetCreated = taxonomySetCreated;
            return this;
        }

        public Builder availableTaxonomy(Map<String, Object> availableTaxonomy) {
            this.availableTaxonomy = availableTaxonomy;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public DefaultQuestionBankResponseDto build() {
            return new DefaultQuestionBankResponseDto(this);
        }
    }
}
