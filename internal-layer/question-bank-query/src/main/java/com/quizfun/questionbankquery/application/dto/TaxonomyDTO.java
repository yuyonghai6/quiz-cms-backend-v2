package com.quizfun.questionbankquery.application.dto;

import java.util.List;

/**
 * Taxonomy information attached to a question in the read model.
 *
 * @param categories hierarchical categories (AND logic)
 * @param tags tag identifiers (OR logic)
 * @param quizzes quiz identifiers (OR logic)
 */
public record TaxonomyDTO(
        List<String> categories,
        List<String> tags,
        List<String> quizzes
) {}
