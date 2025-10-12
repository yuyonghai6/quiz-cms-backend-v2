package com.quizfun.questionbankquery.application.queries;

import com.quizfun.globalshared.mediator.Result;
import com.quizfun.questionbankquery.application.dto.PaginationMetadata;
import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsRequest;
import com.quizfun.questionbankquery.application.dto.QueryQuestionsResponse;
import com.quizfun.questionbankquery.application.ports.in.IQueryQuestionsService;
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
import static org.mockito.Mockito.*;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1016.query-handler-mediator-integration")
@ExtendWith(MockitoExtension.class)
@DisplayName("Query Questions Handler Unit Tests")
class QueryQuestionsHandlerTest {

    @Mock
    private IQueryQuestionsService queryQuestionsService;

    private QueryQuestionsHandler queryQuestionsHandler;

    @BeforeEach
    void setUp() {
        queryQuestionsHandler = new QueryQuestionsHandler(queryQuestionsService);
    }

    @Test
    @DisplayName("Should handle query and return success result")
    void shouldHandleQueryAndReturnSuccessResult() {
        // GIVEN: Service returns success
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .page(0)
                .size(20)
                .build();

        List<QuestionDTO> mockQuestions = List.of(
                new QuestionDTO(1L, "Q1", "MCQ", "EASY", null, null, Instant.now(), Instant.now())
        );

        PaginationMetadata pagination = new PaginationMetadata(0, 20, 1, 1);
        QueryQuestionsResponse mockResponse = new QueryQuestionsResponse(mockQuestions, pagination);

        when(queryQuestionsService.queryQuestions(request)).thenReturn(Result.success(mockResponse));

        // WHEN: Handling query
        QueryQuestions query = new QueryQuestions(request);
        Result<QueryQuestionsResponse> result = queryQuestionsHandler.handle(query);

        // THEN: Should return success
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(mockResponse);
        verify(queryQuestionsService, times(1)).queryQuestions(request);
    }

    @Test
    @DisplayName("Should handle query and return failure result")
    void shouldHandleQueryAndReturnFailureResult() {
        // GIVEN: Service returns failure
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .build();

        when(queryQuestionsService.queryQuestions(request))
                .thenReturn(Result.failure("Database error"));

        // WHEN: Handling query
        QueryQuestions query = new QueryQuestions(request);
        Result<QueryQuestionsResponse> result = queryQuestionsHandler.handle(query);

        // THEN: Should return failure
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Database error");
    }

    @Test
    @DisplayName("Should handle query with null request gracefully")
    void shouldHandleQueryWithNullRequestGracefully() {
        // GIVEN: Query with null request
        QueryQuestions query = new QueryQuestions(null);

        when(queryQuestionsService.queryQuestions(null))
                .thenReturn(Result.failure("Request cannot be null"));

        // WHEN: Handling query
        Result<QueryQuestionsResponse> result = queryQuestionsHandler.handle(query);

        // THEN: Should return failure
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("cannot be null");
    }

    @Test
    @DisplayName("Should delegate to service without modification")
    void shouldDelegateToServiceWithoutModification() {
        // GIVEN: Valid query
        QueryQuestionsRequest request = QueryQuestionsRequest.builder()
                .userId(12345L)
                .questionBankId(67890L)
                .categories(List.of("Math"))
                .tags(List.of("algebra"))
                .page(1)
                .size(10)
                .build();

        QueryQuestionsResponse mockResponse = new QueryQuestionsResponse(
                List.of(),
                new PaginationMetadata(1, 10, 0, 0)
        );

        when(queryQuestionsService.queryQuestions(request)).thenReturn(Result.success(mockResponse));

        // WHEN: Handling query
        QueryQuestions query = new QueryQuestions(request);
        queryQuestionsHandler.handle(query);

        // THEN: Should pass exact same request to service
        verify(queryQuestionsService, times(1)).queryQuestions(request);
        verifyNoMoreInteractions(queryQuestionsService);
    }
}
