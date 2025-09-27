package com.quizfun.questionbank.application.dto;

import java.util.List;

public class TaxonomyData {
    private Categories categories;
    private List<Tag> tags;
    private List<Quiz> quizzes;
    private DifficultyLevel difficultyLevel;

    public TaxonomyData() {}

    public TaxonomyData(Categories categories, List<Tag> tags, List<Quiz> quizzes, DifficultyLevel difficultyLevel) {
        this.categories = categories;
        this.tags = tags;
        this.quizzes = quizzes;
        this.difficultyLevel = difficultyLevel;
    }

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Quiz> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Quiz> quizzes) {
        this.quizzes = quizzes;
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public static class Categories {
        private Category level1;
        private Category level2;
        private Category level3;
        private Category level4;

        public Categories() {}

        public Category getLevel1() {
            return level1;
        }

        public void setLevel1(Category level1) {
            this.level1 = level1;
        }

        public Category getLevel2() {
            return level2;
        }

        public void setLevel2(Category level2) {
            this.level2 = level2;
        }

        public Category getLevel3() {
            return level3;
        }

        public void setLevel3(Category level3) {
            this.level3 = level3;
        }

        public Category getLevel4() {
            return level4;
        }

        public void setLevel4(Category level4) {
            this.level4 = level4;
        }
    }

    public static class Category {
        private String id;
        private String name;
        private String slug;
        private String parentId;

        public Category() {}

        public Category(String id, String name, String slug, String parentId) {
            this.id = id;
            this.name = name;
            this.slug = slug;
            this.parentId = parentId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getParentId() {
            return parentId;
        }

        public void setParentId(String parentId) {
            this.parentId = parentId;
        }
    }

    public static class Tag {
        private String id;
        private String name;
        private String color;

        public Tag() {}

        public Tag(String id, String name, String color) {
            this.id = id;
            this.name = name;
            this.color = color;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class Quiz {
        private Long quizId;
        private String quizName;
        private String quizSlug;

        public Quiz() {}

        public Quiz(Long quizId, String quizName, String quizSlug) {
            this.quizId = quizId;
            this.quizName = quizName;
            this.quizSlug = quizSlug;
        }

        public Long getQuizId() {
            return quizId;
        }

        public void setQuizId(Long quizId) {
            this.quizId = quizId;
        }

        public String getQuizName() {
            return quizName;
        }

        public void setQuizName(String quizName) {
            this.quizName = quizName;
        }

        public String getQuizSlug() {
            return quizSlug;
        }

        public void setQuizSlug(String quizSlug) {
            this.quizSlug = quizSlug;
        }
    }

    public static class DifficultyLevel {
        private String level;
        private Integer numericValue;
        private String description;

        public DifficultyLevel() {}

        public DifficultyLevel(String level, Integer numericValue, String description) {
            this.level = level;
            this.numericValue = numericValue;
            this.description = description;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public Integer getNumericValue() {
            return numericValue;
        }

        public void setNumericValue(Integer numericValue) {
            this.numericValue = numericValue;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}