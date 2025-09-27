package com.quizfun.questionbank.domain.exceptions;

/**
 * Exception thrown when source question ID validation fails.
 * Specifically for UUID v7 format validation errors.
 */
public class InvalidSourceQuestionIdException extends RuntimeException {

    public InvalidSourceQuestionIdException(String message) {
        super(message);
    }

    public InvalidSourceQuestionIdException(String message, Throwable cause) {
        super(message, cause);
    }
}