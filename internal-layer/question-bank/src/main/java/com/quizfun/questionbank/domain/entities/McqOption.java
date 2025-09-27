package com.quizfun.questionbank.domain.entities;

import java.util.Objects;

public class McqOption {
    private final String key;
    private final String text;
    private final boolean correct;
    private final double points;

    public McqOption(String key, String text, boolean correct, double points) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("text cannot be null or empty");
        }
        if (points < 0) {
            throw new IllegalArgumentException("points cannot be negative");
        }

        this.key = key.trim();
        this.text = text.trim();
        this.correct = correct;
        this.points = points;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public double getPoints() {
        return points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        McqOption mcqOption = (McqOption) o;
        return correct == mcqOption.correct &&
               Double.compare(mcqOption.points, points) == 0 &&
               Objects.equals(key, mcqOption.key) &&
               Objects.equals(text, mcqOption.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, text, correct, points);
    }

    @Override
    public String toString() {
        return String.format("McqOption{key='%s', text='%s', correct=%s, points=%.1f}",
                           key, text, correct, points);
    }
}