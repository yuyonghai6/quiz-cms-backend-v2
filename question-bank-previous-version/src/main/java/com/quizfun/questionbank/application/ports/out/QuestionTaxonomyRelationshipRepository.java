package com.quizfun.questionbank.application.ports.out;

import com.quizfun.shared.common.Result;
import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import org.bson.types.ObjectId;

import java.util.List;

public interface QuestionTaxonomyRelationshipRepository {
    Result<Void> replaceRelationshipsForQuestion(ObjectId questionId, List<QuestionTaxonomyRelationshipAggregate> relationships);
    Result<List<QuestionTaxonomyRelationshipAggregate>> findByQuestionId(ObjectId questionId);
    Result<Void> deleteByQuestionId(ObjectId questionId);
}


