package com.quizfun.questionbankquery.infrastructure.persistence.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document for questions collection (read model).
 *
 * Maps to actual MongoDB schema created by the command side.
 * Field names match MongoDB collection structure exactly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questions")
public class QuestionDocument {

    @Id
    private String id;  // MongoDB ObjectId as string

    @Field("user_id")
    private Long userId;

    @Field("question_bank_id")
    private Long questionBankId;

    @Field("source_question_id")
    private String sourceQuestionId;

    @Field("question_type")
    private String questionType;

    @Field("title")
    private String title;

    @Field("content")
    private String content;

    @Field("points")
    private Integer points;

    @Field("status")
    private String status;

    @Field("solution_explanation")
    private String solutionExplanation;

    @Field("display_order")
    private Integer displayOrder;

    // Type-specific data fields - only one will be populated based on question_type
    @Field("mcq_data")
    private Map<String, Object> mcqData;

    @Field("essay_data")
    private Map<String, Object> essayData;

    @Field("true_false_data")
    private Map<String, Object> trueFalseData;

    // Taxonomy data - will be populated by $lookup aggregation
    private TaxonomyDocument taxonomy;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("published_at")
    private Instant publishedAt;

    @Field("archived_at")
    private Instant archivedAt;

    /**
     * Helper method to get type-specific data based on question type.
     * Returns the populated type-specific data map.
     */
    public Map<String, Object> getTypeSpecificData() {
        if (mcqData != null) return mcqData;
        if (essayData != null) return essayData;
        if (trueFalseData != null) return trueFalseData;
        return null;
    }
}
