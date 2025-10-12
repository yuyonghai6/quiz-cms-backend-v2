package com.quizfun.questionbankquery.application.validation;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator for query requests.
 *
 * <p>Performs business validation beyond JSR-303 annotations.
 */
@Slf4j
public class QueryRequestValidator {

    /**
     * Validates query request.
     *
     * @param request Request to validate
     * @return List of validation errors (empty if valid)
     */
    public static List<String> validate(QueryQuestionsRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Request cannot be null");
            return errors;
        }

        if (request.getUserId() == null) {
            errors.add("User ID cannot be null");
        }

        if (request.getQuestionBankId() == null) {
            errors.add("Question Bank ID cannot be null");
        }

        if (request.getPage() < 0) {
            errors.add("Page number cannot be negative");
        }

        if (request.getSize() < 1 || request.getSize() > 100) {
            errors.add("Page size must be between 1 and 100");
        }

        return errors;
    }
}
