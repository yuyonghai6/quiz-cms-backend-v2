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


}
