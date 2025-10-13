package com.quizfun.questionbankquery.infrastructure.persistence.mappers;

import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.TaxonomyDTO;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.TaxonomyDocument;
import org.springframework.stereotype.Component;

/**
 * Maps MongoDB QuestionDocument to QuestionDTO for API responses.
 *
 * Handles field mapping from database schema to API contract.
 */
@Component
public class QuestionDocumentMapper {

    /**
     * Converts QuestionDocument (from MongoDB) to QuestionDTO (for API response).
     *
     * @param document MongoDB document with all question data
     * @return DTO with mapped fields and taxonomy data
     */
    public QuestionDTO toDTO(QuestionDocument document) {
        if (document == null) return null;

        return new QuestionDTO(
                document.getId(),                    // MongoDB ObjectId as string
                document.getSourceQuestionId(),      // External UUID
                document.getQuestionType(),          // "mcq", "essay", "true_false"
                document.getTitle(),                 // Question title
                document.getContent(),               // Question content (HTML)
                document.getPoints(),                // Points value
                document.getStatus(),                // "draft", "published", "archived"
                document.getSolutionExplanation(),   // Optional explanation
                document.getDisplayOrder(),          // Display order
                document.getTypeSpecificData(),      // MCQ/Essay/TrueFalse data
                toTaxonomyDTO(document.getTaxonomy()), // Taxonomy from $lookup
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getPublishedAt(),
                document.getArchivedAt()
        );
    }

    /**
     * Converts TaxonomyDocument to TaxonomyDTO.
     *
     * @param taxonomy MongoDB taxonomy document built by aggregation
     * @return DTO with taxonomy data
     */
    private TaxonomyDTO toTaxonomyDTO(TaxonomyDocument taxonomy) {
        if (taxonomy == null) return null;
        return new TaxonomyDTO(
                taxonomy.getCategories(),      // All category levels
                taxonomy.getTags(),            // Tag IDs
                taxonomy.getQuizzes(),         // Quiz IDs
                taxonomy.getDifficultyLevel()  // Difficulty level ID
        );
    }
}
