package com.quizfun.orchestrationlayer.controllers;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1017.query-controller-get-endpoint")
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Query Questions Controller Integration Tests")
class QueryQuestionsControllerIntegrationTest {

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
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final Long TEST_USER_ID = 12345L;
    private static final Long TEST_QUESTION_BANK_ID = 67890L;
    private static final String COLLECTION_NAME = "questions";
    private static final String REL_COLLECTION_NAME = "question_taxonomy_relationships";
    private static final String BASE_URL = "/api/v1/users/{userId}/question-banks/{questionBankId}/questions";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

        mongoTemplate.dropCollection(REL_COLLECTION_NAME);
        mongoTemplate.createCollection(REL_COLLECTION_NAME);

    // Create text index on title and content fields for text search
    mongoTemplate.getDb().getCollection(COLLECTION_NAME)
        .createIndex(new Document("title", "text").append("content", "text"));
    mongoTemplate.getDb().getCollection(COLLECTION_NAME)
        .createIndex(new Document("status", 1));

        insertTestQuestions();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.dropCollection(REL_COLLECTION_NAME);
    }

    @Test
    @DisplayName("Should return 200 OK with questions and pagination")
    void shouldReturn200OKWithQuestionsAndPagination() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(10)))
                .andExpect(jsonPath("$.pagination.currentPage", is(0)))
                .andExpect(jsonPath("$.pagination.pageSize", is(10)))
                .andExpect(jsonPath("$.pagination.totalItems", is(20)))
                .andExpect(jsonPath("$.pagination.totalPages", is(2)));
    }

    @Test
    @DisplayName("Should filter by categories")
    void shouldFilterByCategories() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("categories", "Math"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.questions[0].taxonomy.categories", hasItem("Math")));
    }

    @Test
    @DisplayName("Should filter by tags")
    void shouldFilterByTags() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("tags", "algebra"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.questions[0].taxonomy.tags", hasItem("algebra")));
    }

    @Test
    @DisplayName("Should filter by quizzes")
    void shouldFilterByQuizzes() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("quizzes", "midterm-2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.questions[0].taxonomy.quizzes", hasItem("midterm-2024")));
    }

    @Test
    @DisplayName("Should search by text")
    void shouldSearchByText() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("searchText", "equation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.questions[0].content", containsString("equation")));
    }

    @Test
    @DisplayName("Should combine multiple filters")
    void shouldCombineMultipleFilters() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("categories", "Math")
                        .param("tags", "algebra")
                        .param("searchText", "equation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should return 200 OK with empty list when no matches")
    void shouldReturn200OKWithEmptyListWhenNoMatches() throws Exception {
        mockMvc.perform(get(BASE_URL, 99999L, TEST_QUESTION_BANK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(0)))
                .andExpect(jsonPath("$.pagination.totalItems", is(0)))
                .andExpect(jsonPath("$.pagination.totalPages", is(0)));
    }

    @Test
    @DisplayName("Should support pagination with page and size")
    void shouldSupportPaginationWithPageAndSize() throws Exception {
        // First page
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(5)))
                .andExpect(jsonPath("$.pagination.currentPage", is(0)))
                .andExpect(jsonPath("$.pagination.totalPages", is(4)));

        // Second page
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(5)))
                .andExpect(jsonPath("$.pagination.currentPage", is(1)));
    }

    @Test
    @DisplayName("Should support sorting by createdAt")
    void shouldSupportSortingByCreatedAt() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should support sorting by title")
    void shouldSupportSortingByTitle() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("sortBy", "title")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(greaterThan(0))));
    }

    @Test
    @DisplayName("Should use default values when params not provided")
    void shouldUseDefaultValuesWhenParamsNotProvided() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagination.currentPage", is(0)))
                .andExpect(jsonPath("$.pagination.pageSize", is(20)));
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST for invalid page number")
    void shouldReturn400BadRequestForInvalidPageNumber() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 BAD REQUEST for invalid page size")
    void shouldReturn400BadRequestForInvalidPageSize() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("size", "150"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include all question fields in response")
    void shouldIncludeAllQuestionFieldsInResponse() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].questionId", notNullValue()))
                .andExpect(jsonPath("$.questions[0].title", notNullValue()))
                .andExpect(jsonPath("$.questions[0].content", notNullValue()))
                .andExpect(jsonPath("$.questions[0].status", notNullValue()))
                .andExpect(jsonPath("$.questions[0].solutionExplanation", notNullValue()))
                .andExpect(jsonPath("$.questions[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.questions[0].updatedAt", notNullValue()));
    }

    // Helper method

    private void insertTestQuestions() {
        Instant now = Instant.now();
        List<Document> relationships = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ObjectId questionObjectId = new ObjectId();
            Document questionDoc = new Document()
                    .append("_id", questionObjectId)
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
                    .append("created_at", Date.from(now.minusSeconds(100 - i)))
                    .append("updated_at", Date.from(now.minusSeconds(100 - i)));

            mongoTemplate.getDb().getCollection(COLLECTION_NAME).insertOne(questionDoc);

            relationships.addAll(createRelationshipDocuments(questionObjectId, now.minusSeconds(100 - i)));
        }

        if (!relationships.isEmpty()) {
            mongoTemplate.getDb().getCollection(REL_COLLECTION_NAME).insertMany(relationships);
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
