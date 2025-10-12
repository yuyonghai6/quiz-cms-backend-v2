package com.quizfun.orchestrationlayer.e2e;

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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.mongodb.client.model.Indexes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1018.end-to-end-integration-tests")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Query Questions End-to-End Tests (RestTemplate + Testcontainers)")
class QueryQuestionsE2ETest {

    @Container
    @SuppressWarnings("resource")
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Long TEST_USER_ID = 12345L;
    private static final Long TEST_QUESTION_BANK_ID = 67890L;
    private static final String COLLECTION_NAME = "questions";

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/users/" + TEST_USER_ID
                + "/question-banks/" + TEST_QUESTION_BANK_ID + "/questions";

        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

    // Ensure text index on question_text for full-text search paths (driver-level to avoid deprecations)
    String collection = mongoTemplate.getCollectionName(QuestionDocument.class);
    mongoTemplate.getDb().getCollection(collection).createIndex(Indexes.text("question_text"));

        insertTestQuestions();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
    }

    @Test
    @DisplayName("E2E: Should query questions with basic pagination")
    void shouldQueryQuestionsWithBasicPagination() {
        String url = baseUrl + "?page=0&size=10";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) response.getBody().get("questions");
        assertThat(questions).hasSize(10);

    @SuppressWarnings("unchecked")
    Map<String, Object> pagination = (Map<String, Object>) response.getBody().get("pagination");
        assertThat(pagination.get("currentPage")).isEqualTo(0);
        assertThat(pagination.get("pageSize")).isEqualTo(10);
        assertThat(pagination.get("totalItems")).isEqualTo(50);
        assertThat(pagination.get("totalPages")).isEqualTo(5);
    }

    @Test
    @DisplayName("E2E: Should query questions with category filter")
    void shouldQueryQuestionsWithCategoryFilter() {
        String url = baseUrl + "?categories=Math&page=0&size=20";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) response.getBody().get("questions");
        assertThat(questions).hasSize(20);

    Map<String, Object> firstQuestion = questions.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> taxonomy = (Map<String, Object>) firstQuestion.get("taxonomy");
        @SuppressWarnings("unchecked")
        List<String> categories = (List<String>) taxonomy.get("categories");
        assertThat(categories).contains("Math");
    }

    @Test
    @DisplayName("E2E: Should query questions with text search")
    void shouldQueryQuestionsWithTextSearch() {
        String url = baseUrl + "?searchText=equation&page=0&size=10";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) response.getBody().get("questions");
        assertThat(questions).isNotEmpty();

        String questionText = (String) questions.get(0).get("questionText");
        assertThat(questionText.toLowerCase()).contains("equation");
    }

    @Test
    @DisplayName("E2E: Should query questions with combined filters")
    void shouldQueryQuestionsWithCombinedFilters() {
        String url = baseUrl + "?categories=Math&tags=algebra&searchText=equation&page=0&size=10";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) response.getBody().get("questions");
        assertThat(questions).isNotEmpty();
    }

    @Test
    @DisplayName("E2E: Should handle pagination across multiple pages")
    void shouldHandlePaginationAcrossMultiplePages() {
        String url1 = baseUrl + "?page=0&size=10";
        String url2 = baseUrl + "?page=1&size=10";
    String url3 = baseUrl + "?page=5&size=10"; // beyond total pages (0..4)

    ResponseEntity<Map<String, Object>> response1 = restTemplate.exchange(url1, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
    ResponseEntity<Map<String, Object>> response2 = restTemplate.exchange(url2, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
    ResponseEntity<Map<String, Object>> response3 = restTemplate.exchange(url3, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
    List<Map<String, Object>> page1 = (List<Map<String, Object>>) response1.getBody().get("questions");
        @SuppressWarnings("unchecked")
    List<Map<String, Object>> page2 = (List<Map<String, Object>>) response2.getBody().get("questions");
        @SuppressWarnings("unchecked")
    List<Map<String, Object>> page3 = (List<Map<String, Object>>) response3.getBody().get("questions");

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(10);
        assertThat(page3).isEmpty(); // Last page beyond total items
    }

    @Test
    @DisplayName("E2E: Should return empty list for non-existent user")
    void shouldReturnEmptyListForNonExistentUser() {
        String url = "http://localhost:" + port + "/api/v1/users/99999/question-banks/" + TEST_QUESTION_BANK_ID + "/questions?page=0&size=10";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
    List<Map<String, Object>> questions = (List<Map<String, Object>>) response.getBody().get("questions");
        assertThat(questions).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) response.getBody().get("pagination");
        assertThat(pagination.get("totalItems")).isEqualTo(0);
    }

    @Test
    @DisplayName("E2E: Should return 400 for invalid page number")
    void shouldReturn400ForInvalidPageNumber() {
        String url = baseUrl + "?page=-1&size=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("E2E: Should verify complete response structure")
    void shouldVerifyCompleteResponseStructure() {
        String url = baseUrl + "?page=0&size=1";
    ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
        url,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<Map<String, Object>>() {}
    );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    Map<String, Object> body = response.getBody();
        assertThat(body).containsKeys("questions", "pagination");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        assertThat(questions).hasSize(1);

        Map<String, Object> question = questions.get(0);
        assertThat(question).containsKeys(
                "questionId", "questionText", "questionType",
                "difficultyLevel", "taxonomy", "createdAt", "updatedAt"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> taxonomy = (Map<String, Object>) question.get("taxonomy");
        assertThat(taxonomy).containsKeys("categories", "tags", "quizzes");

        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) body.get("pagination");
        assertThat(pagination).containsKeys(
                "currentPage", "pageSize", "totalItems", "totalPages"
        );
    }

    // Helper method

    private void insertTestQuestions() {
        Instant now = Instant.now();
        for (int i = 0; i < 50; i++) {
            TaxonomyDocument taxonomy = TaxonomyDocument.builder()
                    .categories(List.of("Math", "Algebra"))
                    .tags(List.of("algebra", "equations"))
                    .quizzes(List.of("midterm-2024"))
                    .build();

            QuestionDocument doc = QuestionDocument.builder()
                    .questionId(6000000000000L + i)
                    .userId(TEST_USER_ID)
                    .questionBankId(TEST_QUESTION_BANK_ID)
                    .questionText("Solve the equation for x: " + i)
                    .questionType("MCQ")
                    .difficultyLevel("EASY")
                    .taxonomy(taxonomy)
                    .createdAt(now.minusSeconds(100 - i))
                    .updatedAt(now.minusSeconds(100 - i))
                    .build();

            mongoTemplate.insert(doc, COLLECTION_NAME);
        }
    }
}
