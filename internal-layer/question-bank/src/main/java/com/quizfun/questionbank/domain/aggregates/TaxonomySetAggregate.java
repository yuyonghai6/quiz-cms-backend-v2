package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.domain.entities.CategoryLevels;
import com.quizfun.questionbank.domain.entities.DifficultyLevel;
import com.quizfun.questionbank.domain.entities.Quiz;
import com.quizfun.questionbank.domain.entities.Tag;
import com.quizfun.shared.domain.AggregateRoot;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TaxonomySetAggregate extends AggregateRoot {
    private ObjectId id;
    private Long userId;
    private Long questionBankId;
    private CategoryLevels categories;
    private List<Tag> tags;
    private List<Quiz> quizzes;
    private DifficultyLevel currentDifficultyLevel;
    private List<DifficultyLevel> availableDifficultyLevels;

    private TaxonomySetAggregate() {
        // Private constructor for frameworks
    }

    public static TaxonomySetAggregate create(
            ObjectId id,
            Long userId,
            Long questionBankId,
            CategoryLevels categories,
            List<Tag> tags,
            List<Quiz> quizzes,
            DifficultyLevel currentDifficultyLevel,
            List<DifficultyLevel> availableDifficultyLevels) {

        TaxonomySetAggregate aggregate = new TaxonomySetAggregate();
        aggregate.id = Objects.requireNonNull(id, "ID cannot be null");
        aggregate.userId = Objects.requireNonNull(userId, "User ID cannot be null");
        aggregate.questionBankId = Objects.requireNonNull(questionBankId, "Question Bank ID cannot be null");
        aggregate.categories = categories;
        aggregate.tags = tags;
        aggregate.quizzes = quizzes;
        aggregate.currentDifficultyLevel = currentDifficultyLevel;
        aggregate.availableDifficultyLevels = availableDifficultyLevels;
        aggregate.markCreatedNow();

        return aggregate;
    }

    public boolean validateTaxonomyReferences(List<String> taxonomyIds) {
        if (taxonomyIds == null || taxonomyIds.isEmpty()) {
            return true;
        }

        Set<String> allValidIds = getAllValidTaxonomyIds();
        return taxonomyIds.stream().allMatch(allValidIds::contains);
    }

    public boolean validateSingleTaxonomyReference(String taxonomyId) {
        if (taxonomyId == null) {
            return false;
        }
        return getAllValidTaxonomyIds().contains(taxonomyId);
    }

    public List<String> findInvalidTaxonomyReferences(List<String> taxonomyIds) {
        if (taxonomyIds == null || taxonomyIds.isEmpty()) {
            return List.of();
        }

        Set<String> validIds = getAllValidTaxonomyIds();
        return taxonomyIds.stream()
            .filter(id -> !validIds.contains(id))
            .collect(Collectors.toList());
    }

    public boolean validateCategoryHierarchy() {
        if (categories == null) {
            return true;
        }
        return categories.isValidHierarchy();
    }

    public boolean belongsToUser(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean belongsToQuestionBank(Long questionBankId) {
        return this.questionBankId.equals(questionBankId);
    }

    private Set<String> getAllValidTaxonomyIds() {
        Set<String> validIds = new HashSet<>();

        // Add category IDs from all levels
        if (categories != null) {
            if (categories.getLevel1() != null) {
                validIds.add(categories.getLevel1().getId());
            }
            if (categories.getLevel2() != null) {
                validIds.add(categories.getLevel2().getId());
            }
            if (categories.getLevel3() != null) {
                validIds.add(categories.getLevel3().getId());
            }
            if (categories.getLevel4() != null) {
                validIds.add(categories.getLevel4().getId());
            }
        }

        // Add tag IDs
        if (tags != null) {
            tags.forEach(tag -> validIds.add(tag.getId()));
        }

        // Add quiz IDs (convert to string)
        if (quizzes != null) {
            quizzes.forEach(quiz -> validIds.add(quiz.getQuizId().toString()));
        }

        // Add current difficulty level
        if (currentDifficultyLevel != null) {
            validIds.add(currentDifficultyLevel.getLevel());
        }

        return validIds;
    }

    // Getters
    public ObjectId getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionBankId() {
        return questionBankId;
    }

    public CategoryLevels getCategories() {
        return categories;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public DifficultyLevel getCurrentDifficultyLevel() {
        return currentDifficultyLevel;
    }

    public List<DifficultyLevel> getAvailableDifficultyLevels() {
        return availableDifficultyLevels;
    }
}