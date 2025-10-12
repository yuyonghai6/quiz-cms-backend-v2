package com.quizfun.questionbankquery.application.ports.in;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;

/**
 * Input port for querying questions.
 *
 * <p>This interface defines the contract for the application service
 * in hexagonal architecture (ports and adapters pattern).
 */
public interface IQueryQuestionsService {

    /**
     * Queries questions with filters, pagination, and sorting.
     *
     * @param request Query request with filters and pagination
     * @return Result containing questions with pagination metadata, or error
     */
    Result<QueryQuestionsResponse> queryQuestions(QueryQuestionsRequest request);
}
