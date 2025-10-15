package com.quizfun.questionbankquery.application.queries;

import com.quizfun.globalshared.mediator.IQueryHandler;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.ports.in.IQueryQuestionsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Query handler for querying questions.
 *
 * <p>This handler is part of the CQRS pattern implementation.
 * It is automatically registered with the mediator via Spring's component scanning.
 *
 * <p><strong>Handler Registration:</strong></p>
 * <ul>
 *   <li>The handler is marked with @Service for Spring auto-discovery</li>
 *   <li>The mediator uses reflection to extract generic type parameters</li>
 *   <li>At runtime, the mediator routes QueryQuestions queries to this handler</li>
 * </ul>
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Receives QueryQuestions query from mediator</li>
 *   <li>Delegates to application service</li>
 *   <li>Returns Result<QueryQuestionsResponse> back through mediator</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryQuestionsHandler implements IQueryHandler<QueryQuestions, QueryQuestionsResponse> {

    private final IQueryQuestionsService queryQuestionsService;

    @Override
    public Result<QueryQuestionsResponse> handle(QueryQuestions query) {
        log.debug("Handling QueryQuestions query");

        // Delegate to application service
        return queryQuestionsService.queryQuestions(query.getRequest());
    }
}
