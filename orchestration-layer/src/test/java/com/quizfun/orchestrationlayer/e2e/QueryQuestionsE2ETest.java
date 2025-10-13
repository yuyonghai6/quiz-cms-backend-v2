package com.quizfun.orchestrationlayer.e2e;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private static final String QUESTIONS_COLLECTION = "questions";
    private static final String REL_COLLECTION = "question_taxonomy_relationships";

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1/users/" + TEST_USER_ID
                + "/question-banks/" + TEST_QUESTION_BANK_ID + "/questions";

    mongoTemplate.dropCollection(QUESTIONS_COLLECTION);
    mongoTemplate.createCollection(QUESTIONS_COLLECTION);

    mongoTemplate.dropCollection(REL_COLLECTION);
    mongoTemplate.createCollection(REL_COLLECTION);

    // Ensure text index on title and content for search scenarios
    Document indexDefinition = new Document("title", "text").append("content", "text");
    mongoTemplate.getDb().getCollection(QUESTIONS_COLLECTION).createIndex(indexDefinition);
    mongoTemplate.getDb().getCollection(QUESTIONS_COLLECTION).createIndex(new Document("status", 1));

        insertTestQuestions();
    }

    @AfterEach
    void tearDown() {
    mongoTemplate.dropCollection(QUESTIONS_COLLECTION);
    mongoTemplate.dropCollection(REL_COLLECTION);
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

    String content = (String) questions.get(0).get("content");
    assertThat(content.toLowerCase()).contains("equation");
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
        "questionId", "sourceQuestionId", "questionType",
        "title", "content", "status", "solutionExplanation",
        "typeSpecificData", "taxonomy", "createdAt", "updatedAt"
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
        List<Document> relationships = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ObjectId questionId = new ObjectId();
            Instant timestamp = now.minusSeconds(200 - i);
            Document questionDoc = new Document()
                    .append("_id", questionId)
                    .append("user_id", TEST_USER_ID)
                    .append("question_bank_id", TEST_QUESTION_BANK_ID)
                    .append("source_question_id", UUID.randomUUID().toString())
                    .append("question_type", "mcq")
                    .append("title", "Equation practice #" + i)
                    .append("content", "<p>Solve the equation for x: " + i + "</p>")
                    .append("points", 5)
                    .append("status", "draft")
                    .append("solution_explanation", "<p>Rearrange the equation to isolate x.</p>")
                    .append("display_order", i + 1)
            .append("mcq_data", new Document()
                .append("options", List.of(
                    new Document("key", "1")
                        .append("text", "Correct answer")
                        .append("is_correct", true),
                    new Document("key", "2")
                        .append("text", "Wrong answer")
                        .append("is_correct", false)
                ))
                .append("shuffle_options", false)
            )
                    .append("created_at", Date.from(timestamp))
                    .append("updated_at", Date.from(timestamp));

            mongoTemplate.getDb().getCollection(QUESTIONS_COLLECTION).insertOne(questionDoc);
            relationships.addAll(createRelationshipDocuments(questionId, timestamp));
        }

        if (!relationships.isEmpty()) {
            mongoTemplate.getDb().getCollection(REL_COLLECTION).insertMany(relationships);
        }
    }

    private List<Document> createRelationshipDocuments(ObjectId questionId, Instant createdAt) {
        Date createdDate = Date.from(createdAt);
        return List.of(
                new Document()
                        .append("user_id", TEST_USER_ID)
                        .append("question_bank_id", TEST_QUESTION_BANK_ID)
                        .append("question_id", questionId)
                        .append("taxonomy_type", "category_level_1")
                        .append("taxonomy_id", "Math")
                        .append("created_at", createdDate),
                new Document()
                        .append("user_id", TEST_USER_ID)
                        .append("question_bank_id", TEST_QUESTION_BANK_ID)
                        .append("question_id", questionId)
                        .append("taxonomy_type", "category_level_2")
                        .append("taxonomy_id", "Algebra")
                        .append("created_at", createdDate),
                new Document()
                        .append("user_id", TEST_USER_ID)
                        .append("question_bank_id", TEST_QUESTION_BANK_ID)
                        .append("question_id", questionId)
                        .append("taxonomy_type", "tag")
                        .append("taxonomy_id", "algebra")
                        .append("created_at", createdDate),
                new Document()
                        .append("user_id", TEST_USER_ID)
                        .append("question_bank_id", TEST_QUESTION_BANK_ID)
                        .append("question_id", questionId)
                        .append("taxonomy_type", "tag")
                        .append("taxonomy_id", "equations")
                        .append("created_at", createdDate),
                new Document()
                        .append("user_id", TEST_USER_ID)
                        .append("question_bank_id", TEST_QUESTION_BANK_ID)
                        .append("question_id", questionId)
                        .append("taxonomy_type", "quiz")
                        .append("taxonomy_id", "midterm-2024")
                        .append("created_at", createdDate)
        );
    }
}
