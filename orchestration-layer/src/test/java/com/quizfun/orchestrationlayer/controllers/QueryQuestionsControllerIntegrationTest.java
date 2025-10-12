package com.quizfun.orchestrationlayer.controllers;

import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.TaxonomyDocument;
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
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

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
    private static final String BASE_URL = "/api/v1/users/{userId}/question-banks/{questionBankId}/questions";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
        mongoTemplate.createCollection(COLLECTION_NAME);

        // Create text index on question_text field (required for text search)
        TextIndexDefinition textIndex = TextIndexDefinition.builder()
                .onField("question_text")
                .build();
        mongoTemplate.indexOps(QuestionDocument.class).ensureIndex(textIndex);

        insertTestQuestions();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(COLLECTION_NAME);
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
                .andExpect(jsonPath("$.questions[0].questionText", containsString("equation")));
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
    @DisplayName("Should support sorting by questionText")
    void shouldSupportSortingByQuestionText() throws Exception {
        mockMvc.perform(get(BASE_URL, TEST_USER_ID, TEST_QUESTION_BANK_ID)
                        .param("sortBy", "questionText")
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
                .andExpect(jsonPath("$.questions[0].questionText", notNullValue()))
                .andExpect(jsonPath("$.questions[0].questionType", notNullValue()))
                .andExpect(jsonPath("$.questions[0].createdAt", notNullValue()))
                .andExpect(jsonPath("$.questions[0].updatedAt", notNullValue()));
    }

    // Helper method

    private void insertTestQuestions() {
        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            TaxonomyDocument taxonomy = TaxonomyDocument.builder()
                    .categories(List.of("Math", "Algebra"))
                    .tags(List.of("algebra", "equations"))
                    .quizzes(List.of("midterm-2024"))
                    .build();

            QuestionDocument doc = QuestionDocument.builder()
                    .questionId(5000000000000L + i)
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
