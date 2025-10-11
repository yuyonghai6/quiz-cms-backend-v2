package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.ports.out.IQuestionQueryRepository;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.mappers.QuestionDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MongoQuestionQueryRepository implements IQuestionQueryRepository {

    private final MongoTemplate mongoTemplate;
    private final QuestionDocumentMapper mapper;

    private static final String COLLECTION = "questions";

    private static final String F_USER_ID = "user_id";
    private static final String F_QB_ID = "question_bank_id";
    private static final String F_CREATED_AT = "created_at";
    private static final String F_UPDATED_AT = "updated_at";
    private static final String F_QUESTION_TEXT = "question_text";

    @Override
    public List<QuestionDTO> queryQuestions(QueryQuestionsRequest request) {
        Query query = new Query(Criteria.where(F_USER_ID).is(request.getUserId())
                .and(F_QB_ID).is(request.getQuestionBankId()));

        // pagination
        query.skip((long) request.getPage() * request.getSize());
        query.limit(request.getSize());

        // sorting
        Sort.Direction dir = "asc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        query.with(Sort.by(dir, mapSortField(request.getSortBy())));

        List<QuestionDocument> docs = mongoTemplate.find(query, QuestionDocument.class, COLLECTION);
        return docs.stream().map(mapper::toDTO).toList();
    }

    @Override
    public long countQuestions(Long userId, Long questionBankId) {
        Query query = new Query(Criteria.where(F_USER_ID).is(userId).and(F_QB_ID).is(questionBankId));
        return mongoTemplate.count(query, QuestionDocument.class, COLLECTION);
    }

    private String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "createdAt" -> F_CREATED_AT;
            case "updatedAt" -> F_UPDATED_AT;
            case "questionText" -> F_QUESTION_TEXT;
            default -> F_CREATED_AT;
        };
    }
}
