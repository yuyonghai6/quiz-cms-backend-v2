package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1013.complex-taxonomy-filter-aggregation")
@DisplayName("QuestionQueryAggregationBuilder Unit Tests")
class QuestionQueryAggregationBuilderTest {

    private final QuestionQueryAggregationBuilder builder = new QuestionQueryAggregationBuilder();

    @Test
    @DisplayName("Builds base criteria for user and question bank")
    void buildsBaseCriteria() {
        QueryQuestionsRequest req = QueryQuestionsRequest.builder()
                .userId(1L)
                .questionBankId(2L)
                .build();
    Criteria c = builder.buildMatchCriteriaWithoutText(req);
        assertThat(c.getCriteriaObject()).containsKeys("user_id", "question_bank_id");
        assertThat(c.getCriteriaObject().get("user_id").toString()).contains("1");
        assertThat(c.getCriteriaObject().get("question_bank_id").toString()).contains("2");
    }

    @Test
    @DisplayName("Adds $all for categories and $in for tags/quizzes")
    void addsTaxonomyFilters() {
        QueryQuestionsRequest req = QueryQuestionsRequest.builder()
                .userId(1L)
                .questionBankId(2L)
                .categories(List.of("A", "B"))
                .tags(List.of("t1", "t2"))
                .quizzes(List.of("q1"))
                .build();
    Criteria c = builder.buildMatchCriteriaWithoutText(req);
        var obj = c.getCriteriaObject();
        assertThat(obj).containsKeys("taxonomy.categories", "taxonomy.tags", "taxonomy.quizzes");
        assertThat(obj.get("taxonomy.categories").toString()).contains("$all");
        assertThat(obj.get("taxonomy.tags").toString()).contains("$in");
        assertThat(obj.get("taxonomy.quizzes").toString()).contains("$in");
    }

    @Test
    @DisplayName("Adds regex for searchText (case-insensitive)")
    void addsRegexForSearchText() {
        QueryQuestionsRequest req = QueryQuestionsRequest.builder()
                .userId(1L)
                .questionBankId(2L)
                .searchText("Capital")
                .build();
        // Search text is applied inside the aggregation pipeline in buildAggregation() now
        Aggregation agg = builder.buildAggregation(req);
        String pipeline = agg.toString();
        assertThat(pipeline).contains("$match");
        assertThat(pipeline).contains("title");
        assertThat(pipeline).containsIgnoringCase("Capital");
    }

    @Test
    @DisplayName("Builds sort and pagination operations in aggregation")
    void buildsSortAndPagination() {
        QueryQuestionsRequest req = QueryQuestionsRequest.builder()
                .userId(1L)
                .questionBankId(2L)
                .page(2)
                .size(10)
                .sortBy("title")
                .sortDirection("asc")
                .build();

        Aggregation agg = builder.buildAggregation(req);
        String pipeline = agg.toString();

        // Expect operations present: $match, $sort, $skip, $limit
        assertThat(pipeline).contains("$match");
        assertThat(pipeline).contains("$sort");
        assertThat(pipeline).contains("$skip");
        assertThat(pipeline).contains("$limit");

        // sort
    Sort sort = builder.buildSort(req);
    assertThat(sort.getOrderFor("title")).isNotNull();
    assertThat(sort.getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
        // pagination calculations
        assertThat(builder.computeSkip(req)).isEqualTo(20);
        assertThat(builder.computeLimit(req)).isEqualTo(10);
    }
}
