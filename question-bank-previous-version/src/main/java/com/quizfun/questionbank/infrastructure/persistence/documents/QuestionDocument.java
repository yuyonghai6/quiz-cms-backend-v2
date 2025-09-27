package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.domain.entities.QuestionType;
import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.TrueFalseData;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "questions")
public class QuestionDocument {
    @Id
    private ObjectId id;

    @Field("user_id")
    private Long userId;

    @Field("question_bank_id")
    private Long questionBankId;

    @Field("source_question_id")
    private String sourceQuestionId;

    @Field("question_type")
    private String questionType;

    private String title;
    private String content;
    private Integer points;
    private String status;
    @Field("display_order")
    private Integer displayOrder;
    @Field("solution_explanation")
    private String solutionExplanation;

    @Field("mcq_data")
    private McqDataDocument mcqData;

    @Field("essay_data")
    private EssayDataDocument essayData;

    @Field("true_false_data")
    private TrueFalseDataDocument trueFalseData;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("published_at")
    private Instant publishedAt;

    @Field("archived_at")
    private Instant archivedAt;

    public static QuestionDocument fromAggregate(QuestionAggregate aggregate) {
        QuestionDocument document = new QuestionDocument();
        document.id = aggregate.getId();
        document.userId = aggregate.getUserId();
        document.questionBankId = aggregate.getQuestionBankId();
        document.sourceQuestionId = aggregate.getSourceQuestionId();
        document.questionType = aggregate.getQuestionType().getValue();
        document.title = aggregate.getTitle();
        document.content = aggregate.getContent();
        document.points = aggregate.getPoints();
        document.status = aggregate.getStatus();
        document.displayOrder = aggregate.getDisplayOrder();
        document.solutionExplanation = aggregate.getSolutionExplanation();
        document.createdAt = aggregate.getCreatedAt();
        document.updatedAt = aggregate.getUpdatedAt();
        document.publishedAt = aggregate.getPublishedAt();
        document.archivedAt = aggregate.getArchivedAt();

        if (aggregate.getMcqData() != null) {
            document.mcqData = McqDataDocument.fromValueObject(aggregate.getMcqData());
        }
        if (aggregate.getEssayData() != null) {
            document.essayData = EssayDataDocument.fromValueObject(aggregate.getEssayData());
        }
        if (aggregate.getTrueFalseData() != null) {
            document.trueFalseData = TrueFalseDataDocument.fromValueObject(aggregate.getTrueFalseData());
        }
        return document;
    }

    public QuestionAggregate toAggregate() {
        QuestionType type = QuestionType.fromValue(this.questionType);
        QuestionAggregate aggregate = QuestionAggregate.createNew(
            this.userId,
            this.questionBankId,
            this.sourceQuestionId,
            type,
            this.title,
            this.content,
            this.points
        );

        // preserve MongoDB id and timestamps/status fields from document
        try {
            // Using reflection-safe setters via domain methods if available; otherwise relying on lifecycle methods
            java.lang.reflect.Field idField = QuestionAggregate.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(aggregate, this.id);

            if (this.createdAt != null || this.updatedAt != null) {
                java.lang.reflect.Field createdAtField = com.quizfun.shared.domain.AggregateRoot.class.getDeclaredField("createdAt");
                java.lang.reflect.Field updatedAtField = com.quizfun.shared.domain.AggregateRoot.class.getDeclaredField("updatedAt");
                createdAtField.setAccessible(true);
                updatedAtField.setAccessible(true);
                if (this.createdAt != null) createdAtField.set(aggregate, this.createdAt);
                if (this.updatedAt != null) updatedAtField.set(aggregate, this.updatedAt);
            }

            if (this.status != null) {
                java.lang.reflect.Field statusField = QuestionAggregate.class.getDeclaredField("status");
                statusField.setAccessible(true);
                statusField.set(aggregate, this.status);
            }
            if (this.displayOrder != null) {
                java.lang.reflect.Field displayOrderField = QuestionAggregate.class.getDeclaredField("displayOrder");
                displayOrderField.setAccessible(true);
                displayOrderField.set(aggregate, this.displayOrder);
            }
            if (this.solutionExplanation != null) {
                java.lang.reflect.Field solutionExplanationField = QuestionAggregate.class.getDeclaredField("solutionExplanation");
                solutionExplanationField.setAccessible(true);
                solutionExplanationField.set(aggregate, this.solutionExplanation);
            }
            if (this.publishedAt != null) {
                java.lang.reflect.Field publishedAtField = QuestionAggregate.class.getDeclaredField("publishedAt");
                publishedAtField.setAccessible(true);
                publishedAtField.set(aggregate, this.publishedAt);
            }
            if (this.archivedAt != null) {
                java.lang.reflect.Field archivedAtField = QuestionAggregate.class.getDeclaredField("archivedAt");
                archivedAtField.setAccessible(true);
                archivedAtField.set(aggregate, this.archivedAt);
            }
        } catch (Exception ignored) {
        }

        if (this.mcqData != null) {
            aggregate.setMcqData(this.mcqData.toValueObject());
        }
        if (this.essayData != null) {
            aggregate.setEssayData(this.essayData.toValueObject());
        }
        if (this.trueFalseData != null) {
            aggregate.setTrueFalseData(this.trueFalseData.toValueObject());
        }
        return aggregate;
    }

    public ObjectId getId() {
        return id;
    }
}


