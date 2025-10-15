package com.quizfun.questionbankquery.application.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Question data for query responses (read model DTO).
 *
 * Represents a complete question with all metadata, taxonomy, and type-specific data.
 */
public record QuestionDTO(
        String questionId,           // MongoDB ObjectId as string
        String sourceQuestionId,     // External/client-provided question ID (UUID)
        String questionType,         // "mcq", "essay", "true_false"
        String title,                // Question title
        String content,              // Question content (HTML)
        Integer points,              // Points for correct answer
        String status,               // "draft", "published", "archived"
        String solutionExplanation,  // Optional explanation (HTML)
        Integer displayOrder,        // Display order in quiz
        Map<String, Object> typeSpecificData,  // MCQ options, Essay rubric, etc.
        TaxonomyDTO taxonomy,        // Categories, tags, quizzes, difficulty
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,         // When published (if applicable)
        Instant archivedAt           // When archived (if applicable)
) {}
