package com.quizfun.questionbankquery.application.services;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.TestQuestionBankQueryApplication;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.ports.in.IQueryQuestionsService;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1015.query-application-service")
@Testcontainers
@SpringBootTest(classes = TestQuestionBankQueryApplication.class)
@ActiveProfiles("test")
@DisplayName("Query Application Service Integration Tests")
class QueryQuestionsServiceIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
            .withReuse(false);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IQueryQuestionsService queryQuestionsService;

    private static final Long TEST_USER_ID = 12345L;
    private static final Long TEST_QUESTION_BANK_ID = 67890L;
    private static final String COLLECTION_NAME = "questions";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);
        insertTestQuestions(25); // Insert 25 questions
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }

    @Test
    @DisplayName("Should return questions with correct pagination metadata")
    void shouldReturnQuestionsWithCorrectPaginationMetadata() {
        // GIVEN: 25 questions in database
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        // WHEN: Querying first page
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return success with correct data
        assertThat(result.success()).isTrue();

        QueryQuestionsResponse response = result.data();
        assertThat(response.questions()).hasSize(10);
        assertThat(response.pagination().currentPage()).isZero();
        assertThat(response.pagination().pageSize()).isEqualTo(10);
        assertThat(response.pagination().totalItems()).isEqualTo(25);
        assertThat(response.pagination().totalPages()).isEqualTo(3); // ceil(25/10)
    }

    @Test
    @DisplayName("Should handle last page with partial results")
    void shouldHandleLastPageWithPartialResults() {
        // GIVEN: 25 questions in database
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(2) // Last page
                .size(10)
                .build();

        // WHEN: Querying last page
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return 5 questions (25 - 20)
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).hasSize(5);
        assertThat(result.data().pagination().currentPage()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return empty result when no questions match")
    void shouldReturnEmptyResultWhenNoQuestionsMatch() {
        // GIVEN: Request for different user
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(99999L)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        // WHEN: Querying questions
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return empty list
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).isEmpty();
        assertThat(result.data().pagination().totalItems()).isZero();
    }

    // Helper method

    private void insertTestQuestions(int count) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
        QuestionDocument doc = QuestionDocument.builder()
            .id(String.valueOf(3000000000000L + i))
                    .userId(TEST_USER_ID)
                    .questionBankId(TEST_QUESTION_BANK_ID)
            .title("Test Question " + i)
                    .questionType("MCQ")
                    .createdAt(now.minusSeconds(count - i))
                    .updatedAt(now.minusSeconds(count - i))
                    .build();
            mongoTemplate.insert(doc, COLLECTION_NAME);
        }
    }
}
