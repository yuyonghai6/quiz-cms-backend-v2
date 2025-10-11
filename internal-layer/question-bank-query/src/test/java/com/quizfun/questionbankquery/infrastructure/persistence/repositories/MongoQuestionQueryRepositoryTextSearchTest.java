package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.ports.out.IQuestionQueryRepository;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.TaxonomyDocument;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1014.full-text-search-implementation")
@Testcontainers
@SpringBootTest
@DisplayName("Text Search Integration Tests")
class MongoQuestionQueryRepositoryTextSearchTest {

    @SuppressWarnings("resource")
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
            .withExposedPorts(27017)
            .withReuse(false);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IQuestionQueryRepository questionQueryRepository;

    private static final Long TEST_USER_ID = 12345L;
    private static final Long TEST_QUESTION_BANK_ID = 67890L;
    private static final String COLLECTION_NAME = "questions";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

        // Create text index on question_text field (for full-text search performance)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("question_text")
                .build();
        mongoTemplate.indexOps(QuestionDocument.class).ensureIndex(textIndex);

        insertTestQuestionsForTextSearch();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }

    @Test
    @DisplayName("Should search questions by single keyword")
    void shouldSearchQuestionsBySingleKeyword() {
        // GIVEN: Request with search text "equation"
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions containing "equation"
        assertThat(results).hasSize(2);
        assertThat(results).extracting(QuestionDTO::questionText)
                .allMatch(text -> text.toLowerCase().contains("equation"));
    }

    @Test
    @DisplayName("Should search questions case-insensitively")
    void shouldSearchQuestionsCaseInsensitively() {
        // GIVEN: Request with uppercase search text
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("CAPITAL")
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions with "capital" (case-insensitive)
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().questionText()).containsIgnoringCase("capital");
    }

    @Test
    @DisplayName("Should search questions with multiple keywords (phrase search)")
    void shouldSearchQuestionsWithMultipleKeywords() {
        // GIVEN: Request with multiple search keywords (treated as a phrase)
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("linear equation")
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions containing the phrase
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().questionText()).contains("linear equation");
    }

    @Test
    @DisplayName("Should search questions with partial word matching")
    void shouldSearchQuestionsWithPartialWordMatching() {
        // GIVEN: Request with partial word
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("calc")
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions with "calculate" or "calculation"
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(QuestionDTO::questionText)
                .anyMatch(text -> text.toLowerCase().contains("calc"));
    }

    @Test
    @DisplayName("Should return empty list when search text matches nothing")
    void shouldReturnEmptyListWhenSearchTextMatchesNothing() {
        // GIVEN: Request with non-matching search text
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("xyzabc123")
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return empty list
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should combine text search with category filter")
    void shouldCombineTextSearchWithCategoryFilter() {
        // GIVEN: Request with search text and category
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .categories(List.of("Math", "Algebra"))
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching with combined filters
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions matching BOTH filters (AND logic for categories)
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(q -> q.questionText().contains("equation"));
        assertThat(results).allMatch(q -> q.taxonomy().categories().containsAll(List.of("Math", "Algebra")));
    }

    @Test
    @DisplayName("Should combine text search with tag filter")
    void shouldCombineTextSearchWithTagFilter() {
        // GIVEN: Request with search text and tag
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("circle")
                .tags(List.of("geometry"))
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching with combined filters
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions matching BOTH filters
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().questionText()).containsIgnoringCase("circle");
        assertThat(results.getFirst().taxonomy().tags()).contains("geometry");
    }

    @Test
    @DisplayName("Should combine text search with all taxonomy filters")
    void shouldCombineTextSearchWithAllTaxonomyFilters() {
        // GIVEN: Request with search text and all taxonomy filters
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .categories(List.of("Math"))
                .tags(List.of("algebra"))
                .quizzes(List.of("midterm-2024"))
                .page(0)
                .size(10)
                .build();

        // WHEN: Searching with all filters
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return questions matching ALL filters
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().questionText()).contains("equation");
    }

    @Test
    @DisplayName("Should ignore empty search text")
    void shouldIgnoreEmptySearchText() {
        // GIVEN: Request with empty search text
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("")
                .page(0)
                .size(10)
                .build();

        // WHEN: Querying questions
        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        // THEN: Should return all questions (no text filtering)
        assertThat(results).hasSize(5); // All test questions
    }

    @Test
    @DisplayName("Should work with pagination when searching")
    void shouldWorkWithPaginationWhenSearching() {
        // GIVEN: Request with search text "equation" and pagination (2 results exist)
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .page(0)
                .size(1)
                .build();

        // WHEN: Querying first page with size 1
        List<QuestionDTO> page1 = questionQueryRepository.queryQuestions(request);

        // THEN: Should return first result
        assertThat(page1).hasSize(1);
        assertThat(page1.getFirst().questionText()).containsIgnoringCase("equation");

        // AND: Second page should have different result
        QueryQuestionsRequest request2 = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .page(1)
                .size(1)
                .build();
        List<QuestionDTO> page2 = questionQueryRepository.queryQuestions(request2);
        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().questionText()).containsIgnoringCase("equation");
        assertThat(page2.getFirst().questionId()).isNotEqualTo(page1.getFirst().questionId());
    }

    @Test
    @DisplayName("Should count questions matching search text")
    void shouldCountQuestionsMatchingSearchText() {
        // GIVEN: Request with search text
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("equation")
                .build();

        // WHEN: Counting matching questions
        long count = questionQueryRepository.countQuestions(
                TEST_USER_ID,
                TEST_QUESTION_BANK_ID,
                request
        );

        // THEN: Should return correct count
        assertThat(count).isEqualTo(2);
    }

    // Helper methods

    private void insertTestQuestionsForTextSearch() {
        Instant now = Instant.now();

        // Q1: Contains "equation" and "solve"
        QuestionDocument q1 = QuestionDocument.builder()
                .questionId(2000000000001L)
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .questionText("Solve the linear equation for x")
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("Math", "Algebra"))
                        .tags(List.of("algebra", "equations"))
                        .quizzes(List.of("midterm-2024"))
                        .build())
                .createdAt(now.minusSeconds(500))
                .updatedAt(now.minusSeconds(500))
                .build();

        // Q2: Contains "circle" and "calculate"
        QuestionDocument q2 = QuestionDocument.builder()
                .questionId(2000000000002L)
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .questionText("Calculate the area of a circle")
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("Math", "Geometry"))
                        .tags(List.of("geometry"))
                        .quizzes(List.of("final-2024"))
                        .build())
                .createdAt(now.minusSeconds(400))
                .updatedAt(now.minusSeconds(400))
                .build();

        // Q3: Contains "capital"
        QuestionDocument q3 = QuestionDocument.builder()
                .questionId(2000000000003L)
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .questionText("What is the capital of France?")
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("Geography"))
                        .tags(List.of("trivia"))
                        .quizzes(List.of("geography-quiz"))
                        .build())
                .createdAt(now.minusSeconds(300))
                .updatedAt(now.minusSeconds(300))
                .build();

        // Q4: Contains "equation" (different from Q1)
        QuestionDocument q4 = QuestionDocument.builder()
                .questionId(2000000000004L)
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .questionText("Simplify the quadratic equation")
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("Math", "Algebra"))
                        .tags(List.of("algebra"))
                        .quizzes(List.of("practice-set"))
                        .build())
                .createdAt(now.minusSeconds(200))
                .updatedAt(now.minusSeconds(200))
                .build();

        // Q5: Generic question for pagination tests
        QuestionDocument q5 = QuestionDocument.builder()
                .questionId(2000000000005L)
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .questionText("This is a generic test question")
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("General"))
                        .tags(List.of("test"))
                        .quizzes(List.of("test-quiz"))
                        .build())
                .createdAt(now.minusSeconds(100))
                .updatedAt(now.minusSeconds(100))
                .build();

        mongoTemplate.insert(q1, COLLECTION_NAME);
        mongoTemplate.insert(q2, COLLECTION_NAME);
        mongoTemplate.insert(q3, COLLECTION_NAME);
        mongoTemplate.insert(q4, COLLECTION_NAME);
        mongoTemplate.insert(q5, COLLECTION_NAME);
    }
}
