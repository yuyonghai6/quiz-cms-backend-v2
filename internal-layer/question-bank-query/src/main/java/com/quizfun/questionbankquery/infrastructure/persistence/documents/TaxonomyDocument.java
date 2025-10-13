package com.quizfun.questionbankquery.infrastructure.persistence.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Taxonomy data aggregated from question_taxonomy_relationships collection.
 *
 * This document is built by MongoDB $lookup aggregation and represents
 * all taxonomy elements associated with a question.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxonomyDocument {
    /**
     * List of category IDs from all levels (category_level_1 through category_level_4).
     * Example: ["general", "technology", "programming"]
     */
    private List<String> categories;

    /**
     * List of tag IDs associated with this question.
     * Example: ["beginner", "practice"]
     */
    private List<String> tags;

    /**
     * List of quiz IDs this question belongs to.
     * Example: ["quiz1", "quiz2"]
     */
    private List<String> quizzes;

    /**
     * Difficulty level ID for this question.
     * Example: "easy", "medium", "hard"
     */
    private String difficultyLevel;
}
