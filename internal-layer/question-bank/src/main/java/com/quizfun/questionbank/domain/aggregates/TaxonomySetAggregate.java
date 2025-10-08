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

    // Default taxonomy constants
    private static final String DEFAULT_CATEGORY_ID = "general";
    private static final String DEFAULT_CATEGORY_NAME = "General";
    private static final String DEFAULT_CATEGORY_SLUG = "general";

    private static final String DEFAULT_TAG_BEGINNER_ID = "beginner";
    private static final String DEFAULT_TAG_BEGINNER_NAME = "Beginner";
    private static final String DEFAULT_TAG_BEGINNER_COLOR = "#28a745";

    private static final String DEFAULT_TAG_PRACTICE_ID = "practice";
    private static final String DEFAULT_TAG_PRACTICE_NAME = "Practice";
    private static final String DEFAULT_TAG_PRACTICE_COLOR = "#007bff";

    private static final String DEFAULT_TAG_QUICKTEST_ID = "quick-test";
    private static final String DEFAULT_TAG_QUICKTEST_NAME = "Quick Test";
    private static final String DEFAULT_TAG_QUICKTEST_COLOR = "#6f42c1";

    private static final String DIFFICULTY_EASY = "easy";
    private static final String DIFFICULTY_EASY_DESC = "Suitable for beginners and initial learning";
    private static final String DIFFICULTY_MEDIUM = "medium";
    private static final String DIFFICULTY_MEDIUM_DESC = "Intermediate knowledge required";
    private static final String DIFFICULTY_HARD = "hard";
    private static final String DIFFICULTY_HARD_DESC = "Advanced understanding needed";

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

    /**
     * Factory method for creating default taxonomy set for new users.
     *
     * Creates minimal taxonomy structure:
     * - Single category (level_1: "general")
     * - Three tags: "beginner", "practice", "quick-test"
     * - Empty quizzes list
     * - Three difficulty levels (easy, medium, hard)
     * - Current difficulty set to "easy"
     *
     * @param userId The user ID
     * @param questionBankId The question bank ID
     * @param timestamp The creation timestamp
     * @return New aggregate with default taxonomy
     * @throws NullPointerException if any parameter is null
     */
    public static TaxonomySetAggregate createDefault(
            Long userId,
            Long questionBankId,
            Instant timestamp) {

        Objects.requireNonNull(userId, "User ID cannot be null");
        Objects.requireNonNull(questionBankId, "Question Bank ID cannot be null");
        Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        // Create default category (only level_1)
        CategoryLevels.Category generalCategory = new CategoryLevels.Category(
            DEFAULT_CATEGORY_ID,
            DEFAULT_CATEGORY_NAME,
            DEFAULT_CATEGORY_SLUG,
            null  // No parent for level_1
        );
        CategoryLevels categories = new CategoryLevels(generalCategory, null, null, null);

        // Create default tags
        List<Tag> tags = List.of(
            new Tag(DEFAULT_TAG_BEGINNER_ID, DEFAULT_TAG_BEGINNER_NAME, DEFAULT_TAG_BEGINNER_COLOR),
            new Tag(DEFAULT_TAG_PRACTICE_ID, DEFAULT_TAG_PRACTICE_NAME, DEFAULT_TAG_PRACTICE_COLOR),
            new Tag(DEFAULT_TAG_QUICKTEST_ID, DEFAULT_TAG_QUICKTEST_NAME, DEFAULT_TAG_QUICKTEST_COLOR)
        );

        // Empty quizzes list
        List<Quiz> quizzes = List.of();

        // Create difficulty levels
        DifficultyLevel easy = new DifficultyLevel(
            DIFFICULTY_EASY,
            1,
            DIFFICULTY_EASY_DESC
        );

        DifficultyLevel medium = new DifficultyLevel(
            DIFFICULTY_MEDIUM,
            2,
            DIFFICULTY_MEDIUM_DESC
        );

        DifficultyLevel hard = new DifficultyLevel(
            DIFFICULTY_HARD,
            3,
            DIFFICULTY_HARD_DESC
        );

        List<DifficultyLevel> availableLevels = List.of(easy, medium, hard);

        // Create aggregate with default values
        return create(
            new ObjectId(),
            userId,
            questionBankId,
            categories,
            tags,
            quizzes,
            easy,  // Current level is "easy"
            availableLevels
        );
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

        // Add all available difficulty levels
        if (availableDifficultyLevels != null) {
            availableDifficultyLevels.forEach(level -> validIds.add(level.getLevel()));
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