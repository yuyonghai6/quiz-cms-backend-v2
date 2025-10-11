package com.quizfun.questionbankquery.application.ports.out;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;

import java.util.List;

/**
 * Output port for querying questions from the data store.
 */
public interface IQuestionQueryRepository {

    /**
     * Queries questions by user and question bank with pagination and sorting (no taxonomy filters yet).
     */
    List<QuestionDTO> queryQuestions(QueryQuestionsRequest request);

    /**
     * Counts total number of questions for pagination metadata.
     */
    long countQuestions(Long userId, Long questionBankId);
}
