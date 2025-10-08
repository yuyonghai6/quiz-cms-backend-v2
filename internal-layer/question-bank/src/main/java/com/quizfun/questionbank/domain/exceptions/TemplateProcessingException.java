package com.quizfun.questionbank.domain.exceptions;

/**
 * Exception thrown when template processing fails.
 */
public class TemplateProcessingException extends RuntimeException {

    public TemplateProcessingException(String message) {
        super(message);
    }

    public TemplateProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
