package com.quizfun.questionbank.application.ports.out;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbank.application.dto.DefaultQuestionBankResponseDto;
import com.quizfun.questionbank.domain.aggregates.QuestionBanksPerUserAggregate;
import com.quizfun.questionbank.domain.aggregates.TaxonomySetAggregate;

/**
 * Port interface for creating default question banks with MongoDB transactions.
 *
 * This repository ensures atomic creation of both question_banks_per_user
 * and taxonomy_sets documents using MongoDB transactions.
 */
public interface DefaultQuestionBankRepository {

    /**
     * Creates default question bank for a new user with transaction support.
     *
     * @param questionBanksAggregate The question banks aggregate containing user data
     * @param taxonomyAggregate The taxonomy aggregate containing default taxonomy
     * @return Result with response DTO on success, or failure message
     */
    Result<DefaultQuestionBankResponseDto> createDefaultQuestionBank(
        QuestionBanksPerUserAggregate questionBanksAggregate,
        TaxonomySetAggregate taxonomyAggregate
    );
}
