package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.questionbank.application.ports.out.QuestionRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionAggregate;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.shared.common.Result;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MongoQuestionRepository implements QuestionRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoQuestionRepository.class);
    private final MongoTemplate mongoTemplate;

    public MongoQuestionRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Result<QuestionAggregate> upsertBySourceQuestionId(QuestionAggregate aggregate) {
        try {
            logger.debug("Upserting question with source ID: {} for user: {} and question bank: {}",
                    aggregate.getSourceQuestionId(), aggregate.getUserId(), aggregate.getQuestionBankId());

            Query query = Query.query(Criteria
                .where("user_id").is(aggregate.getUserId())
                .and("question_bank_id").is(aggregate.getQuestionBankId())
                .and("source_question_id").is(aggregate.getSourceQuestionId()));

            QuestionDocument existing = mongoTemplate.findOne(query, QuestionDocument.class);
            if (existing != null && existing.getId() != null) {
                // preserve _id and createdAt for update scenario
                try {
                    java.lang.reflect.Field idField = com.quizfun.questionbank.domain.aggregates.QuestionAggregate.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(aggregate, existing.getId());

                    // Preserve createdAt timestamp so update detection works correctly
                    java.lang.reflect.Field createdAtField = com.quizfun.shared.domain.AggregateRoot.class.getDeclaredField("createdAt");
                    createdAtField.setAccessible(true);
                    createdAtField.set(aggregate, existing.toAggregate().getCreatedAt());
                } catch (Exception ignored) {}
            }

            QuestionDocument toSave = QuestionDocument.fromAggregate(aggregate);
            QuestionDocument saved = mongoTemplate.save(toSave);
            return Result.success(saved.toAggregate());
        } catch (DataAccessException ex) {
            logger.error("Database error during question upsert for source ID: {}", aggregate.getSourceQuestionId(), ex);
            return Result.failure("DATABASE_ERROR", "Failed to upsert question: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during question upsert for source ID: {}", aggregate.getSourceQuestionId(), ex);
            return Result.failure("UPSERT_ERROR", "Unexpected error during question upsert: " + ex.getMessage());
        }
    }

    @Override
    public Result<Optional<QuestionAggregate>> findBySourceQuestionId(Long userId, Long questionBankId, String sourceQuestionId) {
        try {
            logger.debug("Finding question by source ID: {} for user: {} and question bank: {}",
                    sourceQuestionId, userId, questionBankId);

            Query query = Query.query(Criteria
                .where("user_id").is(userId)
                .and("question_bank_id").is(questionBankId)
                .and("source_question_id").is(sourceQuestionId));

            QuestionDocument doc = mongoTemplate.findOne(query, QuestionDocument.class);
            return Result.success(Optional.ofNullable(doc).map(QuestionDocument::toAggregate));
        } catch (DataAccessException ex) {
            logger.error("Database error during question find for source ID: {}", sourceQuestionId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to find question: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during question find for source ID: {}", sourceQuestionId, ex);
            return Result.failure("FIND_ERROR", "Unexpected error during question find: " + ex.getMessage());
        }
    }

    @Override
    public Result<List<QuestionAggregate>> findByQuestionBank(Long userId, Long questionBankId) {
        try {
            logger.debug("Finding questions for user: {} and question bank: {}", userId, questionBankId);

            Query query = Query.query(Criteria
                .where("user_id").is(userId)
                .and("question_bank_id").is(questionBankId));

            List<QuestionDocument> documents = mongoTemplate.find(query, QuestionDocument.class);
            List<QuestionAggregate> aggregates = documents.stream()
                .map(QuestionDocument::toAggregate)
                .collect(Collectors.toList());
            return Result.success(aggregates);
        } catch (DataAccessException ex) {
            logger.error("Database error during questions find for user: {} and question bank: {}", userId, questionBankId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to find questions: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during questions find for user: {} and question bank: {}", userId, questionBankId, ex);
            return Result.failure("FIND_ERROR", "Unexpected error during questions find: " + ex.getMessage());
        }
    }

    @Override
    public Result<Void> delete(ObjectId questionId) {
        try {
            logger.debug("Deleting question with MongoDB ID: {}", questionId);
            Query query = Query.query(Criteria.where("_id").is(questionId));
            var deleteResult = mongoTemplate.remove(query, QuestionDocument.class);
            if (deleteResult.getDeletedCount() > 0) {
                return Result.success(null);
            }
            return Result.failure("QUESTION_NOT_FOUND", "Question not found for deletion");
        } catch (DataAccessException ex) {
            logger.error("Database error during question deletion for ID: {}", questionId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to delete question: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during question deletion for ID: {}", questionId, ex);
            return Result.failure("DELETE_ERROR", "Unexpected error during question deletion: " + ex.getMessage());
        }
    }
}


