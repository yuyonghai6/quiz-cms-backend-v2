package com.quizfun.questionbankquery.application.queries;

import com.quizfun.globalshared.mediator.IQuery;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Query for retrieving questions with filters and pagination.
 *
 * <p>This implements the CQRS query pattern using the mediator.
 * Queries are routed to the appropriate handler via the mediator.
 */
@Getter
@AllArgsConstructor
public class QueryQuestions implements IQuery<QueryQuestionsResponse> {

    private final QueryQuestionsRequest request;
}
