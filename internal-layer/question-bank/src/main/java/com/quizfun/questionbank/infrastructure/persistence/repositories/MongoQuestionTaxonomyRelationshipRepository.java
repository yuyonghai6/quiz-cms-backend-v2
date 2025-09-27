package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.questionbank.application.ports.out.QuestionTaxonomyRelationshipRepository;
import com.quizfun.questionbank.domain.aggregates.QuestionTaxonomyRelationshipAggregate;
import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionTaxonomyRelationshipDocument;
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
import java.util.stream.Collectors;

@Repository
public class MongoQuestionTaxonomyRelationshipRepository implements QuestionTaxonomyRelationshipRepository {

    private static final Logger logger = LoggerFactory.getLogger(MongoQuestionTaxonomyRelationshipRepository.class);
    private final MongoTemplate mongoTemplate;

    public MongoQuestionTaxonomyRelationshipRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Result<Void> replaceRelationshipsForQuestion(ObjectId questionId, List<QuestionTaxonomyRelationshipAggregate> relationships) {
        try {
            logger.debug("Replacing taxonomy relationships for question ID: {} with {} relationships", questionId, relationships.size());

            Query deleteQuery = Query.query(Criteria.where("question_id").is(questionId));
            var deleteResult = mongoTemplate.remove(deleteQuery, QuestionTaxonomyRelationshipDocument.class);

            if (relationships != null && !relationships.isEmpty()) {
                var docs = relationships.stream()
                    .map(QuestionTaxonomyRelationshipDocument::fromAggregate)
                    .collect(Collectors.toList());
                mongoTemplate.insertAll(docs);
            }
            return Result.success(null);
        } catch (DataAccessException ex) {
            logger.error("Database error during relationship replacement for question ID: {}", questionId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to replace taxonomy relationships: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during relationship replacement for question ID: {}", questionId, ex);
            return Result.failure("RELATIONSHIP_ERROR", "Unexpected error during relationship replacement: " + ex.getMessage());
        }
    }

    @Override
    public Result<List<QuestionTaxonomyRelationshipAggregate>> findByQuestionId(ObjectId questionId) {
        try {
            logger.debug("Finding taxonomy relationships for question ID: {}", questionId);
            Query query = Query.query(Criteria.where("question_id").is(questionId));
            var docs = mongoTemplate.find(query, QuestionTaxonomyRelationshipDocument.class);
            var aggs = docs.stream().map(QuestionTaxonomyRelationshipDocument::toAggregate).collect(Collectors.toList());
            return Result.success(aggs);
        } catch (DataAccessException ex) {
            logger.error("Database error during relationship find for question ID: {}", questionId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to find taxonomy relationships: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during relationship find for question ID: {}", questionId, ex);
            return Result.failure("FIND_ERROR", "Unexpected error during relationship find: " + ex.getMessage());
        }
    }

    @Override
    public Result<Void> deleteByQuestionId(ObjectId questionId) {
        try {
            logger.debug("Deleting all taxonomy relationships for question ID: {}", questionId);
            Query query = Query.query(Criteria.where("question_id").is(questionId));
            mongoTemplate.remove(query, QuestionTaxonomyRelationshipDocument.class);
            return Result.success(null);
        } catch (DataAccessException ex) {
            logger.error("Database error during relationship deletion for question ID: {}", questionId, ex);
            return Result.failure("DATABASE_ERROR", "Failed to delete taxonomy relationships: " + ex.getMessage());
        } catch (Exception ex) {
            logger.error("Unexpected error during relationship deletion for question ID: {}", questionId, ex);
            return Result.failure("DELETE_ERROR", "Unexpected error during relationship deletion: " + ex.getMessage());
        }
    }
}


