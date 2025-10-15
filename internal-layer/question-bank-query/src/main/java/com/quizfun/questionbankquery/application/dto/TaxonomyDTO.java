package com.quizfun.questionbankquery.application.dto;

import java.util.List;

/**
 * Taxonomy information attached to a question in the read model.
 *
 * @param categories hierarchical categories from all levels (AND logic)
 * @param tags tag identifiers (OR logic)
 * @param quizzes quiz identifiers (OR logic)
 * @param difficultyLevel difficulty level identifier (e.g., "easy", "medium", "hard")
 */
public record TaxonomyDTO(
        List<String> categories,
        List<String> tags,
        List<String> quizzes,
        String difficultyLevel
) {}
