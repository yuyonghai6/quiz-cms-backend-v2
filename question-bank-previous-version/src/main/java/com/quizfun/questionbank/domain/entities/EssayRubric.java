package com.quizfun.questionbank.domain.entities;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EssayRubric {
    private final String description;
    private final List<String> criteria;
    private final Integer maxPoints;

    public EssayRubric(String description, List<String> criteria, Integer maxPoints) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("criteria cannot be null or empty");
        }
        if (maxPoints != null && maxPoints < 0) {
            throw new IllegalArgumentException("maxPoints cannot be negative");
        }

        this.description = description;
        this.criteria = new ArrayList<>(criteria); // Defensive copy
        this.maxPoints = maxPoints;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCriteria() {
        return new ArrayList<>(criteria); // Return defensive copy
    }

    public Integer getMaxPoints() {
        return maxPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EssayRubric that = (EssayRubric) o;
        return Objects.equals(description, that.description) &&
               Objects.equals(criteria, that.criteria) &&
               Objects.equals(maxPoints, that.maxPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, criteria, maxPoints);
    }

    @Override
    public String toString() {
        return String.format("EssayRubric{description='%s', criteriaCount=%d, maxPoints=%s}",
                           description, criteria.size(), maxPoints);
    }
}