package com.quizfun.questionbank.infrastructure.persistence.documents;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB document for taxonomy_sets collection.
 *
 * Stores taxonomy structure including categories, tags, quizzes, and difficulty levels.
 */
@Document(collection = "taxonomy_sets")
@CompoundIndex(name = "user_qb_idx", def = "{'user_id': 1, 'question_bank_id': 1}", unique = true)
public class TaxonomySetDocument {

    @Id
    private ObjectId id;

    @Field("user_id")
    private Long userId;

    @Field("question_bank_id")
    private Long questionBankId;

    @Field("categories")
    private CategoryLevelsEmbedded categories;

    @Field("tags")
    private List<TagEmbedded> tags;

    @Field("quizzes")
    private List<QuizEmbedded> quizzes;

    @Field("current_difficulty_level")
    private DifficultyLevelEmbedded currentDifficultyLevel;

    @Field("available_difficulty_levels")
    private List<DifficultyLevelEmbedded> availableDifficultyLevels;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    /**
     * Embedded category levels structure.
     */
    public static class CategoryLevelsEmbedded {
        @Field("level_1")
        private CategoryEmbedded level1;

        @Field("level_2")
        private CategoryEmbedded level2;

        @Field("level_3")
        private CategoryEmbedded level3;

        @Field("level_4")
        private CategoryEmbedded level4;

        public CategoryLevelsEmbedded() {}

        public CategoryLevelsEmbedded(CategoryEmbedded level1, CategoryEmbedded level2,
                                     CategoryEmbedded level3, CategoryEmbedded level4) {
            this.level1 = level1;
            this.level2 = level2;
            this.level3 = level3;
            this.level4 = level4;
        }

        // Getters and Setters
        public CategoryEmbedded getLevel1() { return level1; }
        public void setLevel1(CategoryEmbedded level1) { this.level1 = level1; }

        public CategoryEmbedded getLevel2() { return level2; }
        public void setLevel2(CategoryEmbedded level2) { this.level2 = level2; }

        public CategoryEmbedded getLevel3() { return level3; }
        public void setLevel3(CategoryEmbedded level3) { this.level3 = level3; }

        public CategoryEmbedded getLevel4() { return level4; }
        public void setLevel4(CategoryEmbedded level4) { this.level4 = level4; }
    }

    /**
     * Embedded category within category levels.
     */
    public static class CategoryEmbedded {
        @Field("id")
        private String id;

        @Field("name")
        private String name;

        @Field("slug")
        private String slug;

        @Field("parent_id")
        private String parentId;

        public CategoryEmbedded() {}

        public CategoryEmbedded(String id, String name, String slug, String parentId) {
            this.id = id;
            this.name = name;
            this.slug = slug;
            this.parentId = parentId;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
    }

    /**
     * Embedded tag.
     */
    public static class TagEmbedded {
        @Field("id")
        private String id;

        @Field("name")
        private String name;

        @Field("color")
        private String color;

        public TagEmbedded() {}

        public TagEmbedded(String id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    /**
     * Embedded quiz reference.
     */
    public static class QuizEmbedded {
        @Field("quiz_id")
        private Long quizId;

        @Field("quiz_name")
        private String quizName;

        @Field("quiz_slug")
        private String quizSlug;

        public QuizEmbedded() {}

        public QuizEmbedded(Long quizId, String quizName, String quizSlug) {
            this.quizId = quizId;
            this.quizName = quizName;
            this.quizSlug = quizSlug;
        }

        // Getters and Setters
        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }

        public String getQuizName() { return quizName; }
        public void setQuizName(String quizName) { this.quizName = quizName; }

        public String getQuizSlug() { return quizSlug; }
        public void setQuizSlug(String quizSlug) { this.quizSlug = quizSlug; }
    }

    /**
     * Embedded difficulty level.
     */
    public static class DifficultyLevelEmbedded {
        @Field("level")
        private String level;

        @Field("numeric_value")
        private Integer numericValue;

        @Field("description")
        private String description;

        public DifficultyLevelEmbedded() {}

        public DifficultyLevelEmbedded(String level, Integer numericValue, String description) {
            this.level = level;
            this.numericValue = numericValue;
            this.description = description;
        }

        // Getters and Setters
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public Integer getNumericValue() { return numericValue; }
        public void setNumericValue(Integer numericValue) { this.numericValue = numericValue; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // Constructors
    public TaxonomySetDocument() {}

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionBankId() { return questionBankId; }
    public void setQuestionBankId(Long questionBankId) { this.questionBankId = questionBankId; }

    public CategoryLevelsEmbedded getCategories() { return categories; }
    public void setCategories(CategoryLevelsEmbedded categories) { this.categories = categories; }

    public List<TagEmbedded> getTags() { return tags; }
    public void setTags(List<TagEmbedded> tags) { this.tags = tags; }

    public List<QuizEmbedded> getQuizzes() { return quizzes; }
    public void setQuizzes(List<QuizEmbedded> quizzes) { this.quizzes = quizzes; }

    public DifficultyLevelEmbedded getCurrentDifficultyLevel() { return currentDifficultyLevel; }
    public void setCurrentDifficultyLevel(DifficultyLevelEmbedded currentDifficultyLevel) {
        this.currentDifficultyLevel = currentDifficultyLevel;
    }

    public List<DifficultyLevelEmbedded> getAvailableDifficultyLevels() {
        return availableDifficultyLevels;
    }
    public void setAvailableDifficultyLevels(List<DifficultyLevelEmbedded> availableDifficultyLevels) {
        this.availableDifficultyLevels = availableDifficultyLevels;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
