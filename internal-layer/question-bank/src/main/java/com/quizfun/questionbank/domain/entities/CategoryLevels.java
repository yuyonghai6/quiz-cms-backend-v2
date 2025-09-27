package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class CategoryLevels {
    private final Category level1;
    private final Category level2;
    private final Category level3;
    private final Category level4;

    public CategoryLevels(Category level1, Category level2, Category level3, Category level4) {
        this.level1 = level1;
        this.level2 = level2;
        this.level3 = level3;
        this.level4 = level4;
    }

    public Category getLevel1() {
        return level1;
    }

    public Category getLevel2() {
        return level2;
    }

    public Category getLevel3() {
        return level3;
    }

    public Category getLevel4() {
        return level4;
    }

    public boolean isValidHierarchy() {
        // Level 2 cannot exist without Level 1
        if (level2 != null && level1 == null) {
            return false;
        }
        // Level 3 cannot exist without Level 2
        if (level3 != null && level2 == null) {
            return false;
        }
        // Level 4 cannot exist without Level 3
        if (level4 != null && level3 == null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryLevels that = (CategoryLevels) o;
        return Objects.equals(level1, that.level1) &&
               Objects.equals(level2, that.level2) &&
               Objects.equals(level3, that.level3) &&
               Objects.equals(level4, that.level4);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level1, level2, level3, level4);
    }

    public static class Category {
        private final String id;
        private final String name;
        private final String slug;
        private final String parentId;

        public Category(String id, String name, String slug, String parentId) {
            this.id = Objects.requireNonNull(id, "Category ID cannot be null");
            this.name = Objects.requireNonNull(name, "Category name cannot be null");
            this.slug = slug;
            this.parentId = parentId;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }

        public String getParentId() {
            return parentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Category category = (Category) o;
            return Objects.equals(id, category.id) &&
                   Objects.equals(name, category.name) &&
                   Objects.equals(slug, category.slug) &&
                   Objects.equals(parentId, category.parentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, slug, parentId);
        }

        @Override
        public String toString() {
            return "Category{id='" + id + "', name='" + name + "'}";
        }
    }
}