package com.quizfun.questionbankquery.infrastructure.persistence.repositories;

import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1012.simple-mongodb-query-repository, 1013.complex-taxonomy-filter-aggregation")
@Testcontainers
@SpringBootTest
@DisplayName("Simple MongoDB Query Repository Integration Tests")
class MongoQuestionQueryRepositoryIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0").withReuse(false);

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
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }

    @Test
    @DisplayName("Full-text search should rank by relevance when sortBy=relevance")
    void fullTextSearchRanksByRelevance() {
        // Ensure text index exists for question_text
        // create text index on title field
        mongoTemplate.getDb().getCollection(COLLECTION_NAME)
                .createIndex(new org.bson.Document("title", "text"));

        Instant now = Instant.now();
        insertQuestionWithTimestamp("Paris is the capital of France", now);
        insertQuestionWithTimestamp("The capital city is Paris", now);
        insertQuestionWithTimestamp("Random unrelated text", now);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("capital Paris")
                .sortBy("relevance")
                .sortDirection("desc") // ignored for relevance path
                .page(0)
                .size(10)
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).hasSize(2);
        // Both relevant docs should come first; order between them depends on Mongo scoring
        assertThat(results.get(0).title().toLowerCase()).contains("capital");
        assertThat(results.get(0).title().toLowerCase()).contains("paris");
    }

    @Test
    @DisplayName("Full-text count should respect taxonomy and text filters")
    void fullTextCountRespectsFilters() {
        mongoTemplate.getDb().getCollection(COLLECTION_NAME)
                                .createIndex(new org.bson.Document("title", "text"));

        Instant now = Instant.now();
        insertQuestion("Apple computer history", now, List.of("tech"), List.of("tag1"), List.of("quiz1"));
        insertQuestion("Apple pie recipe", now, List.of("cooking"), List.of("tag2"), List.of("quiz2"));
        insertQuestion("Banana smoothie recipe", now, List.of("cooking"), List.of("tag2"), List.of("quiz2"));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("apple recipe")
                .tags(List.of("tag2")) // OR matches pie and smoothie, but smoothie lacks apple keyword
                .sortBy("relevance")
                .page(0)
                .size(10)
                .build();

        long count = questionQueryRepository.countQuestions(TEST_USER_ID, TEST_QUESTION_BANK_ID, request);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Should query questions by userId and questionBankId")
    void shouldQueryQuestionsByUserIdAndQuestionBankId() {
        insertTestQuestions(5);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        assertThat(results).hasSize(5);
        assertThat(results.getFirst().title()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty list when no questions match")
    void shouldReturnEmptyListWhenNoQuestionsMatch() {
        insertTestQuestions(3);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(99999L)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should support pagination with page and size")
    void shouldSupportPaginationWithPageAndSize() {
        insertTestQuestions(25);

        QueryQuestionsRequest request1 = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();
        List<QuestionDTO> page1 = questionQueryRepository.queryQuestions(request1);

        QueryQuestionsRequest request2 = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(1)
                .size(10)
                .build();
        List<QuestionDTO> page2 = questionQueryRepository.queryQuestions(request2);

        QueryQuestionsRequest request3 = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(2)
                .size(10)
                .build();
        List<QuestionDTO> page3 = questionQueryRepository.queryQuestions(request3);

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(10);
        assertThat(page3).hasSize(5);
        assertThat(page1.getFirst().questionId()).isNotEqualTo(page2.getFirst().questionId());
    }

    @Test
    @DisplayName("Should sort by createdAt descending")
    void shouldSortByCreatedAtDescending() {
        Instant now = Instant.now();
        insertQuestionWithTimestamp("Q1", now.minusSeconds(300));
        insertQuestionWithTimestamp("Q2", now.minusSeconds(200));
        insertQuestionWithTimestamp("Q3", now.minusSeconds(100));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).title()).isEqualTo("Q3");
        assertThat(results.get(1).title()).isEqualTo("Q2");
        assertThat(results.get(2).title()).isEqualTo("Q1");
    }

    @Test
    @DisplayName("Should sort by createdAt ascending")
    void shouldSortByCreatedAtAscending() {
        Instant now = Instant.now();
        insertQuestionWithTimestamp("Q1", now.minusSeconds(300));
        insertQuestionWithTimestamp("Q2", now.minusSeconds(200));
        insertQuestionWithTimestamp("Q3", now.minusSeconds(100));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .sortBy("createdAt")
                .sortDirection("asc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).title()).isEqualTo("Q1");
        assertThat(results.get(1).title()).isEqualTo("Q2");
        assertThat(results.get(2).title()).isEqualTo("Q3");
    }

    @Test
    @DisplayName("Should sort by questionText alphabetically")
    void shouldSortByQuestionTextAlphabetically() {
        Instant now = Instant.now();
        insertQuestionWithTimestamp("Zebra question", now);
        insertQuestionWithTimestamp("Apple question", now);
        insertQuestionWithTimestamp("Mango question", now);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .sortBy("title")
                .sortDirection("asc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).title()).isEqualTo("Apple question");
        assertThat(results.get(1).title()).isEqualTo("Mango question");
        assertThat(results.get(2).title()).isEqualTo("Zebra question");
    }

    @Test
    @DisplayName("Should count total questions for pagination")
    void shouldCountTotalQuestionsForPagination() {
        insertTestQuestions(42);

        long count = questionQueryRepository.countQuestions(TEST_USER_ID, TEST_QUESTION_BANK_ID);
        assertThat(count).isEqualTo(42);
    }

    @Test
    @DisplayName("Should return zero count when no questions exist")
    void shouldReturnZeroCountWhenNoQuestionsExist() {
        long count = questionQueryRepository.countQuestions(TEST_USER_ID, TEST_QUESTION_BANK_ID);
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Should filter by categories using AND logic")
    void shouldFilterByCategoriesUsingAnd() {
        Instant now = Instant.now();
        // has both catA and catB
        insertQuestion("QB both cats", now, List.of("catA", "catB"), List.of("t1"), List.of("q1"));
        // has only catA
        insertQuestion("QB only catA", now, List.of("catA"), List.of("t2"), List.of("q2"));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .categories(List.of("catA", "catB"))
                .page(0)
                .size(10)
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().title()).isEqualTo("QB both cats");
    }

    @Test
    @DisplayName("Should filter by tags using OR logic")
    void shouldFilterByTagsUsingOr() {
        Instant now = Instant.now();
        insertQuestion("QB tag t1", now, List.of("A"), List.of("t1"), List.of("q1"));
        insertQuestion("QB tag t2", now, List.of("A"), List.of("t2"), List.of("q2"));
        insertQuestion("QB tag none", now, List.of("A"), List.of("t3"), List.of("q3"));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .tags(List.of("t1", "t2"))
                .page(0)
                .size(10)
                .sortBy("title")
                .sortDirection("asc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).hasSize(2);
        assertThat(results.stream().map(QuestionDTO::title).toList()).containsExactly("QB tag t1", "QB tag t2");
    }

    @Test
    @DisplayName("Should filter by quizzes using OR logic")
    void shouldFilterByQuizzesUsingOr() {
        Instant now = Instant.now();
        insertQuestion("QB quiz q1", now, List.of("A"), List.of("t1"), List.of("q1"));
        insertQuestion("QB quiz q2", now, List.of("A"), List.of("t1"), List.of("q2"));
        insertQuestion("QB quiz q3", now, List.of("A"), List.of("t1"), List.of("q3"));

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .quizzes(List.of("q2", "q3"))
                .page(0)
                .size(10)
                .sortBy("title")
                .sortDirection("asc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).hasSize(2);
        assertThat(results.stream().map(QuestionDTO::title).toList()).containsExactly("QB quiz q2", "QB quiz q3");
    }

    @Test
    @DisplayName("Should filter by searchText case-insensitively")
    void shouldFilterBySearchText() {
        Instant now = Instant.now();
        insertQuestionWithTimestamp("Paris is the Capital", now);
        insertQuestionWithTimestamp("Capitalization matters?", now);
        insertQuestionWithTimestamp("Random content", now);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .searchText("capital")
                .page(0)
                .size(10)
                .sortBy("title")
                .sortDirection("asc")
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);
        assertThat(results).hasSize(2);
        assertThat(results.stream().map(QuestionDTO::title).toList())
                .containsExactly("Capitalization matters?", "Paris is the Capital");
    }

    @Test
    @DisplayName("Should map all question fields correctly")
    void shouldMapAllQuestionFieldsCorrectly() {
        Long questionId = 123456789L;
        QuestionDocument doc = QuestionDocument.builder()
                .id(String.valueOf(questionId))
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .title("What is the capital of France?")
                .questionType("MCQ")
                .mcqData(Map.of(
                        "options", List.of("Paris", "London", "Berlin"),
                        "correctAnswer", "Paris"
                ))
                .taxonomy(TaxonomyDocument.builder()
                        .categories(List.of("Geography", "Europe"))
                        .tags(List.of("trivia", "beginner"))
                        .quizzes(List.of("geography-101"))
                        .difficultyLevel("EASY")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        mongoTemplate.insert(doc, COLLECTION_NAME);

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        List<QuestionDTO> results = questionQueryRepository.queryQuestions(request);

        assertThat(results).hasSize(1);
        QuestionDTO dto = results.getFirst();
        assertThat(dto.questionId()).isEqualTo(String.valueOf(questionId));
        assertThat(dto.title()).isEqualTo("What is the capital of France?");
        assertThat(dto.questionType()).isEqualTo("MCQ");
        assertThat(dto.taxonomy().difficultyLevel()).isEqualTo("EASY");
        assertThat(dto.typeSpecificData()).containsKey("options");
        assertThat(dto.taxonomy()).isNotNull();
        assertThat(dto.taxonomy().categories()).containsExactly("Geography", "Europe");
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.updatedAt()).isNotNull();
    }

    private void insertTestQuestions(int count) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            QuestionDocument doc = QuestionDocument.builder()
                    .id(String.valueOf(1000000000000L + i))
                    .userId(TEST_USER_ID)
                    .questionBankId(TEST_QUESTION_BANK_ID)
                    .title("Test Question " + i)
                    .questionType("MCQ")
                    .mcqData(Map.of("options", List.of("A", "B", "C")))
                    .taxonomy(TaxonomyDocument.builder()
                            .categories(List.of("Test"))
                            .tags(List.of("test-tag"))
                            .quizzes(List.of("test-quiz"))
                            .difficultyLevel("EASY")
                            .build())
                    .createdAt(now.minusSeconds(count - i))
                    .updatedAt(now.minusSeconds(count - i))
                    .build();
            mongoTemplate.insert(doc, COLLECTION_NAME);
        }
    }

    private void insertQuestionWithTimestamp(String text, Instant timestamp) {
        QuestionDocument doc = QuestionDocument.builder()
                .id(String.valueOf(System.currentTimeMillis()))
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .title(text)
                .questionType("MCQ")
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
        mongoTemplate.insert(doc, COLLECTION_NAME);
    }

    private void insertQuestion(String text, Instant timestamp, List<String> categories, List<String> tags, List<String> quizzes) {
        QuestionDocument doc = QuestionDocument.builder()
                .id(String.valueOf(System.nanoTime()))
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .title(text)
                .questionType("MCQ")
                .taxonomy(TaxonomyDocument.builder()
                        .categories(categories)
                        .tags(tags)
                        .quizzes(quizzes)
                        .build())
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
        mongoTemplate.insert(doc, COLLECTION_NAME);
    }
}
