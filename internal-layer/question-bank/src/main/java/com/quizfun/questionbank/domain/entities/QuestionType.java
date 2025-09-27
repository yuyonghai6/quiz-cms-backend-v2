package com.quizfun.questionbank.domain.entities;

public enum QuestionType {
    MCQ("mcq"),
    ESSAY("essay"),
    TRUE_FALSE("true_false");

    private final String value;

    QuestionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static QuestionType fromValue(String value) {
        for (QuestionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown question type: " + value);
    }
}