package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "question_taxonomy_relationships")
public class QuestionTaxonomyRelationshipDocument {
    @Id
    private ObjectId id;
    @Field("user_id")
    private Long userId;
    @Field("question_bank_id")
    private Long questionBankId;
    @Field("question_id")
    private ObjectId questionId;
    @Field("taxonomy_type")
    private String taxonomyType;
    @Field("taxonomy_id")
    private String taxonomyId;
    @Field("created_at")
    private Instant createdAt;

    public static QuestionTaxonomyRelationshipDocument fromAggregate(QuestionTaxonomyRelationshipAggregate agg) {
        QuestionTaxonomyRelationshipDocument doc = new QuestionTaxonomyRelationshipDocument();
        doc.id = agg.getId();
        doc.userId = agg.getUserId();
        doc.questionBankId = agg.getQuestionBankId();
        doc.questionId = agg.getQuestionId();
        doc.taxonomyType = agg.getTaxonomyType();
        doc.taxonomyId = agg.getTaxonomyId();
        doc.createdAt = agg.getCreatedAt();
        return doc;
    }

    public QuestionTaxonomyRelationshipAggregate toAggregate() {
        // Aggregate factory creates new id; to preserve id and timestamps, set via reflection
        QuestionTaxonomyRelationshipAggregate agg = QuestionTaxonomyRelationshipAggregate.create(
            this.userId, this.questionBankId, this.questionId, this.taxonomyType, this.taxonomyId
        );
        try {
            java.lang.reflect.Field idField = QuestionTaxonomyRelationshipAggregate.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(agg, this.id);

            java.lang.reflect.Field createdAtField = com.quizfun.shared.domain.AggregateRoot.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(agg, this.createdAt);
        } catch (Exception ignored) {}
        return agg;
    }
}


