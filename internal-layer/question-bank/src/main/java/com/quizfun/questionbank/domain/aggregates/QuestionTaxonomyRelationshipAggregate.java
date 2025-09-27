package com.quizfun.questionbank.domain.aggregates;

import com.quizfun.questionbank.application.commands.UpsertQuestionCommand;
import com.quizfun.shared.domain.AggregateRoot;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.List;

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

    public static List<QuestionTaxonomyRelationshipAggregate> createFromCommand(
            ObjectId questionId, UpsertQuestionCommand command) {

        var relationships = new java.util.ArrayList<QuestionTaxonomyRelationshipAggregate>();

        if (command.getTaxonomy() == null) {
            return relationships;
        }

        // Create category relationships
        var categories = command.getTaxonomy().getCategories();
        if (categories != null) {
            if (categories.getLevel1() != null) {
                relationships.add(createRelationship(questionId, command,
                    "category_level_1", categories.getLevel1().getId()));
            }
            if (categories.getLevel2() != null) {
                relationships.add(createRelationship(questionId, command,
                    "category_level_2", categories.getLevel2().getId()));
            }
            if (categories.getLevel3() != null) {
                relationships.add(createRelationship(questionId, command,
                    "category_level_3", categories.getLevel3().getId()));
            }
            if (categories.getLevel4() != null) {
                relationships.add(createRelationship(questionId, command,
                    "category_level_4", categories.getLevel4().getId()));
            }
        }

        // Create tag relationships
        if (command.getTaxonomy().getTags() != null) {
            command.getTaxonomy().getTags().forEach(tag -> {
                relationships.add(createRelationship(questionId, command, "tag", tag.getId()));
            });
        }

        // Create quiz relationships
        if (command.getTaxonomy().getQuizzes() != null) {
            command.getTaxonomy().getQuizzes().forEach(quiz -> {
                relationships.add(createRelationship(questionId, command,
                    "quiz", quiz.getQuizId().toString()));
            });
        }

        // Create difficulty level relationship
        if (command.getTaxonomy().getDifficultyLevel() != null) {
            relationships.add(createRelationship(questionId, command,
                "difficulty_level", command.getTaxonomy().getDifficultyLevel().getLevel()));
        }

        return relationships;
    }

    private static QuestionTaxonomyRelationshipAggregate createRelationship(
            ObjectId questionId, UpsertQuestionCommand command,
            String taxonomyType, String taxonomyId) {

        return create(
            command.getUserId(),
            command.getQuestionBankId(),
            questionId,
            taxonomyType,
            taxonomyId
        );
    }

    public boolean isValidRelationshipType() {
        return java.util.Arrays.asList(
            "category_level_1", "category_level_2", "category_level_3", "category_level_4",
            "tag", "quiz", "difficulty_level"
        ).contains(this.taxonomyType);
    }

    public boolean belongsToUser(Long userId) {
        return this.userId.equals(userId);
    }

    public boolean belongsToQuestionBank(Long questionBankId) {
        return this.questionBankId.equals(questionBankId);
    }

    public boolean belongsToQuestion(ObjectId questionId) {
        return this.questionId.equals(questionId);
    }

    public ObjectId getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getQuestionBankId() { return questionBankId; }
    public ObjectId getQuestionId() { return questionId; }
    public String getTaxonomyType() { return taxonomyType; }
    public String getTaxonomyId() { return taxonomyId; }
}


