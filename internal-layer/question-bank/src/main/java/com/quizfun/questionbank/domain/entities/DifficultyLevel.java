package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class DifficultyLevel {
    private final String level;
    private final Integer numericValue;
    private final String description;

    public DifficultyLevel(String level, Integer numericValue, String description) {
        this.level = Objects.requireNonNull(level, "Difficulty level cannot be null");
        this.numericValue = numericValue;
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public Integer getNumericValue() {
        return numericValue;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DifficultyLevel that = (DifficultyLevel) o;
        return Objects.equals(level, that.level) &&
               Objects.equals(numericValue, that.numericValue) &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, numericValue, description);
    }

    @Override
    public String toString() {
        return "DifficultyLevel{level='" + level + "', numericValue=" + numericValue + ", description='" + description + "'}";
    }
}