package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds Aggregation pipelines and pieces for question queries.
 */
@Component
public class QuestionQueryAggregationBuilder {

    static final String F_USER_ID = "user_id";
    static final String F_QB_ID = "question_bank_id";
    static final String F_CREATED_AT = "created_at";
    static final String F_UPDATED_AT = "updated_at";
    static final String F_QUESTION_TEXT = "question_text";

    public Aggregation buildAggregation(QueryQuestionsRequest request) {
        List<AggregationOperation> ops = new ArrayList<>();
        Criteria criteria = buildMatchCriteria(request);

        MatchOperation match = Aggregation.match(criteria);
        ops.add(match);

        // sorting
        Sort sort = buildSort(request);
        ops.add(Aggregation.sort(sort));

        // pagination
        long skip = computeSkip(request);
        if (skip > 0) {
            ops.add(Aggregation.skip(skip));
        }
        ops.add(Aggregation.limit(computeLimit(request)));

        return Aggregation.newAggregation(ops);
    }

    public Criteria buildMatchCriteria(QueryQuestionsRequest request) {
        Criteria criteria = buildMatchCriteriaWithoutText(request);

        if (request.getSearchText() != null && !request.getSearchText().isBlank()) {
            String escaped = Pattern.quote(request.getSearchText().trim());
            Pattern regex = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE);
            criteria.and(F_QUESTION_TEXT).regex(regex);
        }
        return criteria;
    }

    /**
     * Base criteria without searchText; useful for composing with TextQuery.
     */
    public Criteria buildMatchCriteriaWithoutText(QueryQuestionsRequest request) {
        Criteria criteria = Criteria.where(F_USER_ID).is(request.getUserId())
                .and(F_QB_ID).is(request.getQuestionBankId());

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            criteria.and("taxonomy.categories").all(request.getCategories());
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            criteria.and("taxonomy.tags").in(request.getTags());
        }
        if (request.getQuizzes() != null && !request.getQuizzes().isEmpty()) {
            criteria.and("taxonomy.quizzes").in(request.getQuizzes());
        }
        return criteria;
    }

    /**
     * Build a TextQuery leveraging MongoDB full-text search on question_text.
     */
    public Query buildTextQuery(QueryQuestionsRequest request) {
        String raw = request.getSearchText().trim();
        // Enforce AND semantics by prefixing terms with '+' for Mongo $text
    String[] terms = raw.split("\\s+");
        String search = Arrays.stream(terms)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> "+" + t)
                .collect(Collectors.joining(" "));
        TextCriteria text = TextCriteria.forDefaultLanguage().matching(search);
        TextQuery query = TextQuery.queryText(text);
        // add taxonomy + identifiers (no text condition here because TextQuery already has it)
        query.addCriteria(buildMatchCriteriaWithoutText(request));
    // Additionally enforce AND semantics across individual terms using case-insensitive regex
    List<Criteria> perTerm = Arrays.stream(terms)
        .filter(t -> t != null && !t.isBlank())
        .map(t -> Criteria.where(F_QUESTION_TEXT).regex(Pattern.compile(Pattern.quote(t), Pattern.CASE_INSENSITIVE)))
        .toList();
    if (perTerm.size() > 1) {
        query.addCriteria(new Criteria().andOperator(perTerm.toArray(new Criteria[0])));
    }
        return query;
    }

    public Sort buildSort(QueryQuestionsRequest request) {
        Sort.Direction dir = "asc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, mapSortField(request.getSortBy()));
    }

    public long computeSkip(QueryQuestionsRequest request) {
        return (long) request.getPage() * request.getSize();
    }

    public int computeLimit(QueryQuestionsRequest request) {
        return request.getSize();
    }

    String mapSortField(String sortBy) {
        return switch (sortBy) {
            case "createdAt" -> F_CREATED_AT;
            case "updatedAt" -> F_UPDATED_AT;
            case "questionText" -> F_QUESTION_TEXT;
            default -> F_CREATED_AT;
        };
    }
}
