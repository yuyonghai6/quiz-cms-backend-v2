package com.quizfun.questionbank.infrastructure.persistence.mappers;

import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;
import com.quizfun.questionbank.domain.entities.CategoryLevels;
import com.quizfun.questionbank.domain.entities.DifficultyLevel;
import com.quizfun.questionbank.domain.entities.Quiz;
import com.quizfun.questionbank.domain.entities.Tag;
import com.quizfun.questionbank.infrastructure.persistence.documents.TaxonomySetDocument;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for TaxonomySetAggregate ↔ TaxonomySetDocument.
 */
@Component
public class TaxonomySetMapper {

    /**
     * Maps domain aggregate to MongoDB document.
     */
    public TaxonomySetDocument toDocument(TaxonomySetAggregate aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("Aggregate cannot be null");
        }

        TaxonomySetDocument document = new TaxonomySetDocument();
        document.setId(aggregate.getId());
        document.setUserId(aggregate.getUserId());
        document.setQuestionBankId(aggregate.getQuestionBankId());

        // Map category levels
        document.setCategories(mapCategoriesToEmbedded(aggregate.getCategories()));

        // Map tags
        List<TaxonomySetDocument.TagEmbedded> tagDocs = aggregate.getTags().stream()
            .map(this::mapTagToEmbedded)
            .collect(Collectors.toList());
        document.setTags(tagDocs);

        // Map quizzes
        List<TaxonomySetDocument.QuizEmbedded> quizDocs = aggregate.getQuizzes().stream()
            .map(this::mapQuizToEmbedded)
            .collect(Collectors.toList());
        document.setQuizzes(quizDocs);

        // Map difficulty levels
        document.setCurrentDifficultyLevel(
            mapDifficultyLevelToEmbedded(aggregate.getCurrentDifficultyLevel())
        );

        List<TaxonomySetDocument.DifficultyLevelEmbedded> difficultyDocs =
            aggregate.getAvailableDifficultyLevels().stream()
                .map(this::mapDifficultyLevelToEmbedded)
                .collect(Collectors.toList());
        document.setAvailableDifficultyLevels(difficultyDocs);

        document.setCreatedAt(aggregate.getCreatedAt());
        document.setUpdatedAt(aggregate.getUpdatedAt());

        return document;
    }

    /**
     * Maps MongoDB document to domain aggregate.
     */
    public TaxonomySetAggregate toAggregate(TaxonomySetDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        // Map categories
        CategoryLevels categories = mapEmbeddedToCategories(document.getCategories());

        // Map tags
        List<Tag> tags = document.getTags().stream()
            .map(this::mapEmbeddedToTag)
            .collect(Collectors.toList());

        // Map quizzes
        List<Quiz> quizzes = document.getQuizzes().stream()
            .map(this::mapEmbeddedToQuiz)
            .collect(Collectors.toList());

        // Map difficulty levels
        DifficultyLevel currentLevel = mapEmbeddedToDifficultyLevel(
            document.getCurrentDifficultyLevel()
        );

        List<DifficultyLevel> availableLevels = document.getAvailableDifficultyLevels().stream()
            .map(this::mapEmbeddedToDifficultyLevel)
            .collect(Collectors.toList());

        return TaxonomySetAggregate.create(
            document.getId(),
            document.getUserId(),
            document.getQuestionBankId(),
            categories,
            tags,
            quizzes,
            currentLevel,
            availableLevels
        );
    }

    // Private mapping helper methods

    private TaxonomySetDocument.CategoryLevelsEmbedded mapCategoriesToEmbedded(CategoryLevels categories) {
        if (categories == null) {
            return null;
        }

        return new TaxonomySetDocument.CategoryLevelsEmbedded(
            mapCategoryToEmbedded(categories.getLevel1()),
            mapCategoryToEmbedded(categories.getLevel2()),
            mapCategoryToEmbedded(categories.getLevel3()),
            mapCategoryToEmbedded(categories.getLevel4())
        );
    }

    private TaxonomySetDocument.CategoryEmbedded mapCategoryToEmbedded(CategoryLevels.Category category) {
        if (category == null) {
            return null;
        }

        return new TaxonomySetDocument.CategoryEmbedded(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getParentId()
        );
    }

    private CategoryLevels mapEmbeddedToCategories(TaxonomySetDocument.CategoryLevelsEmbedded embedded) {
        if (embedded == null) {
            return null;
        }

        return new CategoryLevels(
            mapEmbeddedToCategory(embedded.getLevel1()),
            mapEmbeddedToCategory(embedded.getLevel2()),
            mapEmbeddedToCategory(embedded.getLevel3()),
            mapEmbeddedToCategory(embedded.getLevel4())
        );
    }

    private CategoryLevels.Category mapEmbeddedToCategory(TaxonomySetDocument.CategoryEmbedded embedded) {
        if (embedded == null) {
            return null;
        }

        return new CategoryLevels.Category(
            embedded.getId(),
            embedded.getName(),
            embedded.getSlug(),
            embedded.getParentId()
        );
    }

    private TaxonomySetDocument.TagEmbedded mapTagToEmbedded(Tag tag) {
        return new TaxonomySetDocument.TagEmbedded(
            tag.getId(),
            tag.getName(),
            tag.getColor()
        );
    }

    private Tag mapEmbeddedToTag(TaxonomySetDocument.TagEmbedded embedded) {
        return new Tag(
            embedded.getId(),
            embedded.getName(),
            embedded.getColor()
        );
    }

    private TaxonomySetDocument.QuizEmbedded mapQuizToEmbedded(Quiz quiz) {
        return new TaxonomySetDocument.QuizEmbedded(
            quiz.getQuizId(),
            quiz.getQuizName(),
            quiz.getQuizSlug()
        );
    }

    private Quiz mapEmbeddedToQuiz(TaxonomySetDocument.QuizEmbedded embedded) {
        return new Quiz(
            embedded.getQuizId(),
            embedded.getQuizName(),
            embedded.getQuizSlug()
        );
    }

    private TaxonomySetDocument.DifficultyLevelEmbedded mapDifficultyLevelToEmbedded(DifficultyLevel level) {
        return new TaxonomySetDocument.DifficultyLevelEmbedded(
            level.getLevel(),
            level.getNumericValue(),
            level.getDescription()
        );
    }

    private DifficultyLevel mapEmbeddedToDifficultyLevel(TaxonomySetDocument.DifficultyLevelEmbedded embedded) {
        return new DifficultyLevel(
            embedded.getLevel(),
            embedded.getNumericValue(),
            embedded.getDescription()
        );
    }
}
