package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.shared.domain.AggregateRoot;
import org.bson.types.ObjectId;

import java.time.Instant;

public class QuestionTaxonomyRelationshipAggregate extends AggregateRoot {
    private ObjectId id;
    private Long userId;
    private Long questionBankId;
    private ObjectId questionId;
    private String taxonomyType; // e.g., category_level_1, tag, quiz, difficulty_level
    private String taxonomyId;   // id string

    private QuestionTaxonomyRelationshipAggregate() { }

    public static QuestionTaxonomyRelationshipAggregate create(
        Long userId,
        Long questionBankId,
        ObjectId questionId,
        String taxonomyType,
        String taxonomyId
    ) {
        QuestionTaxonomyRelationshipAggregate agg = new QuestionTaxonomyRelationshipAggregate();
        agg.id = new ObjectId();
        agg.userId = userId;
        agg.questionBankId = questionBankId;
        agg.questionId = questionId;
        agg.taxonomyType = taxonomyType;
        agg.taxonomyId = taxonomyId;
        agg.markCreatedNow();
        return agg;
    }

    public ObjectId getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQuestionBankId() { return questionBankId; }
    public ObjectId getQuestionId() { return questionId; }
    public String getTaxonomyType() { return taxonomyType; }
    public String getTaxonomyId() { return taxonomyId; }
}


