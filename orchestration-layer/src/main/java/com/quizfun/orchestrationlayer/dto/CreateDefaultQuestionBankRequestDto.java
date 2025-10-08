package com.quizfun.orchestrationlayer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Request DTO for creating a default question bank for a new user.
 *
 * This DTO is used by external systems (e.g., user management service)
 * to trigger default question bank provisioning via REST API.
 *
 * Validation rules:
 * - userId: Required, must be positive
 * - userEmail: Optional, must be valid email format if provided
 * - metadata: Optional key-value pairs for additional context
 */
public class CreateDefaultQuestionBankRequestDto {

    @NotNull(message = "userId cannot be null")
    @Positive(message = "userId must be positive")
    private Long userId;

    @Email(message = "userEmail must be a valid email address")
    private String userEmail;

    private Map<String, String> metadata;

    /**
     * Default constructor for JSON deserialization.
     */
    public CreateDefaultQuestionBankRequestDto() {
    }

    /**
     * Constructor for testing and programmatic creation.
     *
     * @param userId The user's unique identifier
     * @param userEmail The user's email address (optional)
     * @param metadata Additional metadata (optional)
     */
    public CreateDefaultQuestionBankRequestDto(Long userId, String userEmail, Map<String, String> metadata) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.metadata = metadata;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    /**
     * Builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long userId;
        private String userEmail;
        private Map<String, String> metadata;

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public CreateDefaultQuestionBankRequestDto build() {
            return new CreateDefaultQuestionBankRequestDto(userId, userEmail, metadata);
        }
    }
}
