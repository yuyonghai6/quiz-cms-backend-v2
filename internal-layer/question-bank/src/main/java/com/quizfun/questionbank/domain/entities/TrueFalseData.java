package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class TrueFalseData {
    private final boolean correctAnswer;
    private final String explanation;
    private final Integer timeLimitSeconds;

    public TrueFalseData(boolean correctAnswer, String explanation, Integer timeLimitSeconds) {
        this.correctAnswer = correctAnswer;
        this.explanation = explanation; // Can be null
        this.timeLimitSeconds = timeLimitSeconds; // Can be null
    }

    public boolean getCorrectAnswer() {
        return correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public Integer getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean hasExplanation() {
        return explanation != null && !explanation.trim().isEmpty();
    }

    public boolean hasTimeLimit() {
        return timeLimitSeconds != null && timeLimitSeconds > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrueFalseData that = (TrueFalseData) o;
        return correctAnswer == that.correctAnswer &&
               Objects.equals(explanation, that.explanation) &&
               Objects.equals(timeLimitSeconds, that.timeLimitSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correctAnswer, explanation, timeLimitSeconds);
    }

    @Override
    public String toString() {
        return String.format("TrueFalseData{correctAnswer=%s, hasExplanation=%s, timeLimitSeconds=%s}",
                           correctAnswer, hasExplanation(), timeLimitSeconds);
    }
}