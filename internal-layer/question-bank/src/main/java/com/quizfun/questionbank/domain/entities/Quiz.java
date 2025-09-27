package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class Quiz {
    private final Long quizId;
    private final String quizName;
    private final String quizSlug;

    public Quiz(Long quizId, String quizName, String quizSlug) {
        this.quizId = Objects.requireNonNull(quizId, "Quiz ID cannot be null");
        this.quizName = Objects.requireNonNull(quizName, "Quiz name cannot be null");
        this.quizSlug = quizSlug;
    }

    public Long getQuizId() {
        return quizId;
    }

    public String getQuizName() {
        return quizName;
    }

    public String getQuizSlug() {
        return quizSlug;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quiz quiz = (Quiz) o;
        return Objects.equals(quizId, quiz.quizId) &&
               Objects.equals(quizName, quiz.quizName) &&
               Objects.equals(quizSlug, quiz.quizSlug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(quizId, quizName, quizSlug);
    }

    @Override
    public String toString() {
        return "Quiz{quizId=" + quizId + ", quizName='" + quizName + "', quizSlug='" + quizSlug + "'}";
    }
}