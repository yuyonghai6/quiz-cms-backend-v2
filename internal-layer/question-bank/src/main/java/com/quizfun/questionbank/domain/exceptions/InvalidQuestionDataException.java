package com.quizfun.questionbank.domain.exceptions;

/**
 * Exception thrown when question data validation fails.
 * Used for user-friendly validation error reporting.
 */
public class InvalidQuestionDataException extends RuntimeException {

    public InvalidQuestionDataException(String message) {
        super(message);
    }

    public InvalidQuestionDataException(String message, Throwable cause) {
        super(message, cause);
    }
}