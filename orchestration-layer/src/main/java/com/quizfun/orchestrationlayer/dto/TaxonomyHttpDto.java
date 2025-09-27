package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * HTTP DTO for taxonomy data in question upsert requests.
 * Handles the complex nested taxonomy structure from the JSON request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxonomyHttpDto {

    @NotNull(message = "Categories are required")
    @Valid
    private CategoriesHttpDto categories;

    @Valid
    private List<TagHttpDto> tags;

    @Valid
    private List<QuizHttpDto> quizzes;

    @Valid
    @JsonProperty("difficulty_level")
    private DifficultyLevelHttpDto difficultyLevel;

    public TaxonomyHttpDto() {}

    // Getters and setters
    public CategoriesHttpDto getCategories() { return categories; }
    public void setCategories(CategoriesHttpDto categories) { this.categories = categories; }

    public List<TagHttpDto> getTags() { return tags; }
    public void setTags(List<TagHttpDto> tags) { this.tags = tags; }

    public List<QuizHttpDto> getQuizzes() { return quizzes; }
    public void setQuizzes(List<QuizHttpDto> quizzes) { this.quizzes = quizzes; }

    public DifficultyLevelHttpDto getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyLevelHttpDto difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public static class CategoriesHttpDto {
        @Valid
        @JsonProperty("level_1")
        private CategoryHttpDto level1;

        @Valid
        @JsonProperty("level_2")
        private CategoryHttpDto level2;

        @Valid
        @JsonProperty("level_3")
        private CategoryHttpDto level3;

        @Valid
        @JsonProperty("level_4")
        private CategoryHttpDto level4;

        public CategoriesHttpDto() {}

        public CategoryHttpDto getLevel1() { return level1; }
        public void setLevel1(CategoryHttpDto level1) { this.level1 = level1; }

        public CategoryHttpDto getLevel2() { return level2; }
        public void setLevel2(CategoryHttpDto level2) { this.level2 = level2; }

        public CategoryHttpDto getLevel3() { return level3; }
        public void setLevel3(CategoryHttpDto level3) { this.level3 = level3; }

        public CategoryHttpDto getLevel4() { return level4; }
        public void setLevel4(CategoryHttpDto level4) { this.level4 = level4; }
    }

    public static class CategoryHttpDto {
        private String id;
        private String name;
        private String slug;

        @JsonProperty("parent_id")
        private String parentId;

        public CategoryHttpDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSlug() { return slug; }
        public void setSlug(String slug) { this.slug = slug; }

        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
    }

    public static class TagHttpDto {
        private String id;
        private String name;
        private String color;

        public TagHttpDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class QuizHttpDto {
        @JsonProperty("quiz_id")
        private Long quizId;

        @JsonProperty("quiz_name")
        private String quizName;

        @JsonProperty("quiz_slug")
        private String quizSlug;

        public QuizHttpDto() {}

        public Long getQuizId() { return quizId; }
        public void setQuizId(Long quizId) { this.quizId = quizId; }

        public String getQuizName() { return quizName; }
        public void setQuizName(String quizName) { this.quizName = quizName; }

        public String getQuizSlug() { return quizSlug; }
        public void setQuizSlug(String quizSlug) { this.quizSlug = quizSlug; }
    }

    public static class DifficultyLevelHttpDto {
        private String level;

        @JsonProperty("numeric_value")
        private Integer numericValue;

        private String description;

        public DifficultyLevelHttpDto() {}

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public Integer getNumericValue() { return numericValue; }
        public void setNumericValue(Integer numericValue) { this.numericValue = numericValue; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}