package com.quizfun.questionbankquery.application.queries;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.TestQuestionBankQueryApplication;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1016.query-handler-mediator-integration")
@Testcontainers
@SpringBootTest(classes = TestQuestionBankQueryApplication.class)
@ActiveProfiles("test")
@DisplayName("Query Questions Mediator Integration Tests")
class QueryQuestionsMediatorIntegrationTest {

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
    private IMediator mediator;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private static final Long TEST_USER_ID = 12345L;
    private static final Long TEST_QUESTION_BANK_ID = 67890L;
    private static final String COLLECTION_NAME = "questions";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);
        insertTestQuestions(10);
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }

    @Test
    @DisplayName("Should route query through mediator to handler")
    void shouldRouteQueryThroughMediatorToHandler() {
        // GIVEN: Query request
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        QueryQuestions query = new QueryQuestions(request);

        // WHEN: Sending query through mediator
        Result<QueryQuestionsResponse> result = mediator.send(query);

        // THEN: Should return success
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().questions()).hasSize(10);
    }

    @Test
    @DisplayName("Should handle query with filters through mediator")
    void shouldHandleQueryWithFiltersThroughMediator() {
        // GIVEN: Query with filters
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .categories(List.of("Math"))
                .page(0)
                .size(10)
                .build();

        QueryQuestions query = new QueryQuestions(request);

        // WHEN: Sending query through mediator
        Result<QueryQuestionsResponse> result = mediator.send(query);

        // THEN: Should return filtered results
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle empty results through mediator")
    void shouldHandleEmptyResultsThroughMediator() {
        // GIVEN: Query for non-existent user
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(99999L)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(10)
                .build();

        QueryQuestions query = new QueryQuestions(request);

        // WHEN: Sending query through mediator
        Result<QueryQuestionsResponse> result = mediator.send(query);

        // THEN: Should return empty list (not error)
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).isEmpty();
        assertThat(result.data().pagination().totalItems()).isZero();
    }

    @Test
    @DisplayName("Should return correct pagination metadata through mediator")
    void shouldReturnCorrectPaginationMetadataThroughMediator() {
        // GIVEN: Query with pagination
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(TEST_USER_ID)
                .questionBankId(TEST_QUESTION_BANK_ID)
                .page(0)
                .size(5)
                .build();

        QueryQuestions query = new QueryQuestions(request);

        // WHEN: Sending query through mediator
        Result<QueryQuestionsResponse> result = mediator.send(query);

        // THEN: Pagination should be correct
        assertThat(result.success()).isTrue();
        assertThat(result.data().pagination().currentPage()).isZero();
        assertThat(result.data().pagination().pageSize()).isEqualTo(5);
        assertThat(result.data().pagination().totalItems()).isEqualTo(10);
        assertThat(result.data().pagination().totalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should have QueryQuestionsHandler registered in mediator")
    void shouldHaveQueryQuestionsHandlerRegisteredInMediator() {
        // GIVEN: Spring context is loaded

        // WHEN: Checking if handler is available as Spring bean
        QueryQuestionsHandler handler = applicationContext.getBean(QueryQuestionsHandler.class);

        // THEN: Handler should be registered
        assertThat(handler).isNotNull();
    }

    @Test
    @DisplayName("Should handle invalid query gracefully through mediator")
    void shouldHandleInvalidQueryGracefullyThroughMediator() {
        // GIVEN: Query with null request
        QueryQuestions query = new QueryQuestions(null);

        // WHEN: Sending invalid query through mediator
        Result<QueryQuestionsResponse> result = mediator.send(query);

        // THEN: Should return failure (not throw exception)
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isNotNull();
    }

    // Helper method

    private void insertTestQuestions(int count) {
        Instant now = Instant.now();
        for (int i = 0; i < count; i++) {
            QuestionDocument doc = QuestionDocument.builder()
                    .questionId(4000000000000L + i)
                    .userId(TEST_USER_ID)
                    .questionBankId(TEST_QUESTION_BANK_ID)
                    .questionText("Test Question " + i)
                    .questionType("MCQ")
                    .taxonomy(TaxonomyDocument.builder()
                            .categories(List.of("Math"))
                            .tags(List.of("test"))
                            .quizzes(List.of("test-quiz"))
                            .build())
                    .createdAt(now.minusSeconds(count - i))
                    .updatedAt(now.minusSeconds(count - i))
                    .build();
            mongoTemplate.insert(doc, COLLECTION_NAME);
        }
    }
}
