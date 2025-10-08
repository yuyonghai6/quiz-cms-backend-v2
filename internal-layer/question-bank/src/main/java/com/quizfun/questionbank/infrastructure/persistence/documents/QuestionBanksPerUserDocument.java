package com.quizfun.questionbank.infrastructure.persistence.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document for question_banks_per_user collection.
 *
 * Stores user's question bank registry with embedded question bank array.
 */
@Document(collection = "question_banks_per_user")
public class QuestionBanksPerUserDocument {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    @Field("user_id")
    private Long userId;

    @Field("default_question_bank_id")
    private Long defaultQuestionBankId;

    @Field("question_banks")
    private List<QuestionBankEmbedded> questionBanks;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    /**
     * Embedded question bank within user's question_banks array.
     */
    public static class QuestionBankEmbedded {
        @Field("bank_id")
        private Long bankId;

        @Field("name")
        private String name;

        @Field("description")
        private String description;

        @Field("is_active")
        private boolean isActive;

        @Field("created_at")
        private Instant createdAt;

        @Field("updated_at")
        private Instant updatedAt;

        // Constructors
        public QuestionBankEmbedded() {}

        public QuestionBankEmbedded(Long bankId, String name, String description,
                                   boolean isActive, Instant createdAt, Instant updatedAt) {
            this.bankId = bankId;
            this.name = name;
            this.description = description;
            this.isActive = isActive;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters and Setters
        public Long getBankId() { return bankId; }
        public void setBankId(Long bankId) { this.bankId = bankId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public Instant getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    }

    // Constructors
    public QuestionBanksPerUserDocument() {}

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getDefaultQuestionBankId() { return defaultQuestionBankId; }
    public void setDefaultQuestionBankId(Long defaultQuestionBankId) {
        this.defaultQuestionBankId = defaultQuestionBankId;
    }

    public List<QuestionBankEmbedded> getQuestionBanks() { return questionBanks; }
    public void setQuestionBanks(List<QuestionBankEmbedded> questionBanks) {
        this.questionBanks = questionBanks;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
