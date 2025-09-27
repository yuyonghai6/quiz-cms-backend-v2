package com.quizfun.questionbank.domain.services;

/**
 * Exception thrown when attempting to process a question type that has no registered strategy.
 * This typically indicates a configuration issue where a question type is defined
 * but no corresponding strategy implementation is available.
 */
public class UnsupportedQuestionTypeException extends RuntimeException {

    public UnsupportedQuestionTypeException(String message) {
        super(message);
    }

    public UnsupportedQuestionTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}