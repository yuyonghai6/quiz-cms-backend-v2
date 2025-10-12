package com.quizfun.questionbankquery.application.services;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.PaginationMetadata;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.ports.in.IQueryQuestionsService;
import com.quizfun.questionbankquery.application.ports.out.IQuestionQueryRepository;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1015.query-application-service")
@ExtendWith(MockitoExtension.class)
@DisplayName("Query Application Service Unit Tests")
class QueryQuestionsServiceTest {

    @Mock
    private IQuestionQueryRepository questionQueryRepository;

    private IQueryQuestionsService queryQuestionsService;

    @BeforeEach
    void setUp() {
        queryQuestionsService = new QueryQuestionsService(questionQueryRepository);
    }

    @Test
    @DisplayName("Should return questions with pagination metadata")
    void shouldReturnQuestionsWithPaginationMetadata() {
        // GIVEN: Repository returns 2 questions and total count of 50
        List<QuestionDTO> mockQuestions = List.of(
                new QuestionDTO(1L, "Q1", "MCQ", "EASY", null, null, Instant.now(), Instant.now()),
                new QuestionDTO(2L, "Q2", "MCQ", "EASY", null, null, Instant.now(), Instant.now())
        );

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(20)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(mockQuestions);
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(50L);

        // WHEN: Querying questions
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return success with questions and pagination
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).hasSize(2);

        PaginationMetadata pagination = result.data().pagination();
        assertThat(pagination.currentPage()).isZero();
        assertThat(pagination.pageSize()).isEqualTo(20);
        assertThat(pagination.totalItems()).isEqualTo(50);
        assertThat(pagination.totalPages()).isEqualTo(3); // ceil(50/20) = 3
    }

    @Test
    @DisplayName("Should return empty list when no questions match")
    void shouldReturnEmptyListWhenNoQuestionsMatch() {
        // GIVEN: Repository returns empty list
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(20)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(List.of());
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(0L);

        // WHEN: Querying questions
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return success with empty list
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).isEmpty();
        assertThat(result.data().pagination().totalItems()).isZero();
        assertThat(result.data().pagination().totalPages()).isZero();
    }

    @Test
    @DisplayName("Should calculate total pages correctly")
    void shouldCalculateTotalPagesCorrectly() {
        // GIVEN: 95 total items with page size 20
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(20)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(List.of());
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(95L);

        // WHEN: Querying questions
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Total pages should be 5 (ceil(95/20))
        assertThat(result.success()).isTrue();
        assertThat(result.data().pagination().totalPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle filters in count query")
    void shouldHandleFiltersInCountQuery() {
        // GIVEN: Request with filters
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .categories(List.of("Math"))
                .tags(List.of("algebra"))
                .page(0)
                .size(20)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(List.of());
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(10L);

        // WHEN: Querying with filters
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should pass request to count method for filtering
        verify(questionQueryRepository).countQuestions(eq(12345L), eq(67890L), eq(request));
        assertThat(result.data().pagination().totalItems()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return failure when repository throws exception")
    void shouldReturnFailureWhenRepositoryThrowsException() {
        // GIVEN: Repository throws exception
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .build();

        when(questionQueryRepository.queryQuestions(request))
                .thenThrow(new RuntimeException("Database connection failed"));

        // WHEN: Querying questions
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Should return failure
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Database connection failed");
    }

    @Test
    @DisplayName("Should validate request is not null")
    void shouldValidateRequestIsNotNull() {
        // GIVEN: Null request

        // WHEN: Querying with null request
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(null);

        // THEN: Should return failure
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Request cannot be null");
    }

    @Test
    @DisplayName("Should call repository methods exactly once")
    void shouldCallRepositoryMethodsExactlyOnce() {
        // GIVEN: Valid request
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(List.of());
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(0L);

        // WHEN: Querying questions
        queryQuestionsService.queryQuestions(request);

        // THEN: Repository methods called exactly once
        verify(questionQueryRepository, times(1)).queryQuestions(request);
        verify(questionQueryRepository, times(1)).countQuestions(eq(12345L), eq(67890L), any());
    }

    @Test
    @DisplayName("Should handle pagination for last page correctly")
    void shouldHandlePaginationForLastPageCorrectly() {
        // GIVEN: Last page with partial results (42 total, page 2, size 20)
        List<QuestionDTO> mockQuestions = List.of(
                new QuestionDTO(1L, "Q1", "MCQ", "EASY", null, null, Instant.now(), Instant.now()),
                new QuestionDTO(2L, "Q2", "MCQ", "EASY", null, null, Instant.now(), Instant.now())
        );

        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(2) // Third page (0-indexed)
                .size(20)
                .build();

        when(questionQueryRepository.queryQuestions(request)).thenReturn(mockQuestions);
        when(questionQueryRepository.countQuestions(eq(12345L), eq(67890L), any())).thenReturn(42L);

        // WHEN: Querying last page
        Result<QueryQuestionsResponse> result = queryQuestionsService.queryQuestions(request);

        // THEN: Pagination should be correct
        assertThat(result.success()).isTrue();
        assertThat(result.data().questions()).hasSize(2);
        assertThat(result.data().pagination().currentPage()).isEqualTo(2);
        assertThat(result.data().pagination().totalPages()).isEqualTo(3); // ceil(42/20)
    }
}
