package com.quizfun.questionbank.domain.validation;

public enum ValidationErrorCode {
    UNAUTHORIZED_ACCESS("User doesn't own the question bank"),
    TAXONOMY_REFERENCE_NOT_FOUND("Taxonomy reference not found"),
    MISSING_REQUIRED_FIELD("Required field is missing"),
    TYPE_DATA_MISMATCH("Question type doesn't match provided data"),
    INVALID_QUESTION_TYPE("Invalid question type"),
    DUPLICATE_SOURCE_QUESTION_ID("Source question ID already exists"),
    INVALID_AUTHENTICATION_TOKEN("Invalid or missing authentication token"),
    PATH_PARAMETER_MANIPULATION("Token user ID does not match path parameter user ID"),
    SESSION_SECURITY_VIOLATION("Session security violation detected");

    private final String message;

    ValidationErrorCode(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}