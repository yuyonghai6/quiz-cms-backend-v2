package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.ports.out.IQuestionQueryRepository;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.mappers.QuestionDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MongoQuestionQueryRepository implements IQuestionQueryRepository {

    private final MongoTemplate mongoTemplate;
    private final QuestionDocumentMapper mapper;
    private final QuestionQueryAggregationBuilder aggregationBuilder;

    private static final String COLLECTION = "questions";

    private static final String F_USER_ID = "user_id";
    private static final String F_QB_ID = "question_bank_id";

    @Override
    public List<QuestionDTO> queryQuestions(QueryQuestionsRequest request) {
        // When searchText present and relevance sorting requested, prefer TextQuery
        boolean wantsText = request.getSearchText() != null && !request.getSearchText().isBlank();
        boolean sortByRelevance = "relevance".equalsIgnoreCase(request.getSortBy());
        if (wantsText && sortByRelevance) {
            Query textQuery = aggregationBuilder.buildTextQuery(request);
            // Relevance sort: TextQuery has .sortByScore(); also apply skip/limit
            if (textQuery instanceof TextQuery tq) {
                tq.sortByScore();
            }
            textQuery.skip((long) request.getPage() * request.getSize());
            textQuery.limit(request.getSize());
            List<QuestionDocument> docs = mongoTemplate.find(textQuery, QuestionDocument.class, COLLECTION);
            return docs.stream().map(mapper::toDTO).toList();
        }

        Aggregation aggregation = aggregationBuilder.buildAggregation(request);
        List<QuestionDocument> docs = mongoTemplate
                .aggregate(aggregation, COLLECTION, QuestionDocument.class)
                .getMappedResults();
        return docs.stream().map(mapper::toDTO).toList();
    }

    @Override
    public long countQuestions(Long userId, Long questionBankId) {
        Query query = new Query(Criteria.where(F_USER_ID).is(userId).and(F_QB_ID).is(questionBankId));
        return mongoTemplate.count(query, QuestionDocument.class, COLLECTION);
    }

    @Override
    public long countQuestions(Long userId, Long questionBankId, QueryQuestionsRequest request) {
        // Use base criteria (without regex) alongside TextQuery when searchText provided
        if (request.getSearchText() != null && !request.getSearchText().isBlank()) {
            Query textQuery = aggregationBuilder.buildTextQuery(request);
            return mongoTemplate.count(textQuery, QuestionDocument.class, COLLECTION);
        }

        Criteria criteria = aggregationBuilder.buildMatchCriteriaWithoutText(request);
        Query query = new Query(criteria);
        return mongoTemplate.count(query, QuestionDocument.class, COLLECTION);
    }

}
