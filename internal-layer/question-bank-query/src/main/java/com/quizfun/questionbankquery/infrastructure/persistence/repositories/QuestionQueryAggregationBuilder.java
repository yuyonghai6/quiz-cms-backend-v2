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
    static final String F_TITLE = "title";
    static final String F_CONTENT = "content";

    /**
     * Builds MongoDB aggregation pipeline with $lookup to join taxonomy relationships.
     *
     * Pipeline stages:
     * 1. $match - filter by user_id, question_bank_id
     * 2. $lookup - join question_taxonomy_relationships collection
     * 3. $addFields - build taxonomy object from relationships
     * 4. $match - apply taxonomy filters (if provided)
     * 5. $sort - sort results
     * 6. $skip/$limit - pagination
     */
    public Aggregation buildAggregation(QueryQuestionsRequest request) {
        List<AggregationOperation> ops = new ArrayList<>();

        // Stage 1: Match by user and question bank
        Criteria baseCriteria = Criteria.where(F_USER_ID).is(request.getUserId())
                .and(F_QB_ID).is(request.getQuestionBankId());
        ops.add(Aggregation.match(baseCriteria));

        // Stage 2: $lookup to join taxonomy relationships
        ops.add(Aggregation.lookup(
                "question_taxonomy_relationships",  // from collection
                "_id",                               // local field (question._id)
                "question_id",                       // foreign field (relationship.question_id)
                "taxonomy_relationships"             // output array field
        ));

        // Stage 3: Build taxonomy object from relationships using custom operation
        ops.add(new TaxonomyAggregationOperation());

        // Stage 4: Apply taxonomy filters if provided
        if (hasSearchText(request)) {
            String escaped = Pattern.quote(request.getSearchText().trim());
            Pattern regex = Pattern.compile(escaped, Pattern.CASE_INSENSITIVE);
            Criteria searchCriteria = new Criteria().orOperator(
                    Criteria.where(F_TITLE).regex(regex),
                    Criteria.where(F_CONTENT).regex(regex)
            );
            ops.add(Aggregation.match(searchCriteria));
        }

        if (hasTaxonomyFilters(request)) {
            Criteria taxonomyFilterCriteria = buildTaxonomyFilterCriteria(request);
            ops.add(Aggregation.match(taxonomyFilterCriteria));
        }

        // Stage 5: Sort
        Sort sort = buildSort(request);
        ops.add(Aggregation.sort(sort));

        // Stage 6: Pagination
        long skip = computeSkip(request);
        if (skip > 0) {
            ops.add(Aggregation.skip(skip));
        }
        ops.add(Aggregation.limit(computeLimit(request)));

        return Aggregation.newAggregation(ops);
    }

    private boolean hasSearchText(QueryQuestionsRequest request) {
        return request.getSearchText() != null && !request.getSearchText().isBlank();
    }

    private boolean hasTaxonomyFilters(QueryQuestionsRequest request) {
        return (request.getCategories() != null && !request.getCategories().isEmpty()) ||
               (request.getTags() != null && !request.getTags().isEmpty()) ||
               (request.getQuizzes() != null && !request.getQuizzes().isEmpty());
    }

    private Criteria buildTaxonomyFilterCriteria(QueryQuestionsRequest request) {
        List<Criteria> taxonomyCriteriaList = new ArrayList<>();

        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            taxonomyCriteriaList.add(Criteria.where("taxonomy.categories").all(request.getCategories()));
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            taxonomyCriteriaList.add(Criteria.where("taxonomy.tags").in(request.getTags()));
        }
        if (request.getQuizzes() != null && !request.getQuizzes().isEmpty()) {
            taxonomyCriteriaList.add(Criteria.where("taxonomy.quizzes").in(request.getQuizzes()));
        }

        if (taxonomyCriteriaList.size() == 1) {
            return taxonomyCriteriaList.get(0);
        } else {
            return new Criteria().andOperator(taxonomyCriteriaList.toArray(new Criteria[0]));
        }
    }

    /**
     * Base criteria without searchText or taxonomy filters; useful for count queries.
     */
    public Criteria buildMatchCriteriaWithoutText(QueryQuestionsRequest request) {
        return Criteria.where(F_USER_ID).is(request.getUserId())
                .and(F_QB_ID).is(request.getQuestionBankId());
    }

    /**
     * Build a TextQuery leveraging MongoDB full-text search on title and content.
     * Note: This is kept for compatibility but aggregation pipeline is preferred.
     */
    public Query buildTextQuery(QueryQuestionsRequest request) {
        String raw = request.getSearchText().trim();
        String[] terms = raw.split("\\s+");
        String search = Arrays.stream(terms)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> "+" + t)
                .collect(Collectors.joining(" "));
        TextCriteria text = TextCriteria.forDefaultLanguage().matching(search);
        TextQuery query = TextQuery.queryText(text);
        query.addCriteria(buildMatchCriteriaWithoutText(request));

        // Additionally enforce AND semantics across individual terms
        List<Criteria> perTerm = Arrays.stream(terms)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> new Criteria().orOperator(
                        Criteria.where(F_TITLE).regex(Pattern.compile(Pattern.quote(t), Pattern.CASE_INSENSITIVE)),
                        Criteria.where(F_CONTENT).regex(Pattern.compile(Pattern.quote(t), Pattern.CASE_INSENSITIVE))
                ))
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
            case "title" -> F_TITLE;
            case "content" -> F_CONTENT;
            default -> F_CREATED_AT;
        };
    }
}
