package com.quizfun.questionbank.infrastructure.persistence.repositories;

import com.quizfun.questionbank.infrastructure.persistence.documents.QuestionBanksPerUserDocument;
import com.quizfun.shared.common.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MongoQuestionBanksPerUserRepositoryTest")
class MongoQuestionBanksPerUserRepositoryTest {

    @Mock
    MongoTemplate mongoTemplate;

    @InjectMocks
    MongoQuestionBanksPerUserRepository repository;

    @Nested
    @DisplayName("validateOwnership")
    class ValidateOwnership {
        @Test
        void returnsFalseWhenIdsNull() {
            Result<Boolean> r1 = repository.validateOwnership(null, 2002L);
            Result<Boolean> r2 = repository.validateOwnership(1001L, null);
            assertThat(r1.isSuccess()).isTrue();
            assertThat(r1.getValue()).isFalse();
            assertThat(r2.isSuccess()).isTrue();
            assertThat(r2.getValue()).isFalse();
        }

        @Test
        void returnsTrueWhenDocumentExists() {
            when(mongoTemplate.exists(any(Query.class), eq(QuestionBanksPerUserDocument.class))).thenReturn(true);
            var res = repository.validateOwnership(1001L, 2002L);
            assertThat(res.isSuccess()).isTrue();
            assertThat(res.getValue()).isTrue();
        }

        @Test
        void handlesDatabaseExceptionGracefully() {
            when(mongoTemplate.exists(any(Query.class), eq(QuestionBanksPerUserDocument.class)))
                    .thenThrow(new DataAccessResourceFailureException("DB down"));
            var res = repository.validateOwnership(1001L, 2002L);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("DATABASE_ERROR");
            assertThat(res.getError()).contains("Failed to validate ownership");
        }
    }

    @Nested
    @DisplayName("getDefaultQuestionBankId")
    class GetDefaultQuestionBankId {
        @Test
        void failsOnNullUserId() {
            var res = repository.getDefaultQuestionBankId(null);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("INVALID_INPUT");
        }

        @Test
        void failsWhenNoDocumentFound() {
            when(mongoTemplate.findOne(any(Query.class), eq(QuestionBanksPerUserDocument.class))).thenReturn(null);
            var res = repository.getDefaultQuestionBankId(1001L);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("NOT_FOUND");
        }

        @Test
        void failsWhenDefaultIdNull() {
            var doc = new QuestionBanksPerUserDocument();
            doc.setUserId(1001L);
            doc.setDefaultQuestionBankId(null);
            when(mongoTemplate.findOne(any(Query.class), eq(QuestionBanksPerUserDocument.class))).thenReturn(doc);
            var res = repository.getDefaultQuestionBankId(1001L);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("NOT_FOUND");
        }

        @Test
        void succeedsWhenDefaultIdPresent() {
            var doc = new QuestionBanksPerUserDocument();
            doc.setUserId(1001L);
            doc.setDefaultQuestionBankId(2002L);
            when(mongoTemplate.findOne(any(Query.class), eq(QuestionBanksPerUserDocument.class))).thenReturn(doc);
            var res = repository.getDefaultQuestionBankId(1001L);
            assertThat(res.isSuccess()).isTrue();
            assertThat(res.getValue()).isEqualTo(2002L);
        }

        @Test
        void handlesDatabaseExceptionGracefully() {
            when(mongoTemplate.findOne(any(Query.class), eq(QuestionBanksPerUserDocument.class)))
                    .thenThrow(new DataAccessResourceFailureException("DB error"));
            var res = repository.getDefaultQuestionBankId(1001L);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("DATABASE_ERROR");
        }
    }

    @Nested
    @DisplayName("isQuestionBankActive")
    class IsQuestionBankActive {
        @Test
        void returnsFalseWhenIdsNull() {
            var r1 = repository.isQuestionBankActive(null, 2002L);
            var r2 = repository.isQuestionBankActive(1001L, null);
            assertThat(r1.isSuccess()).isTrue();
            assertThat(r1.getValue()).isFalse();
            assertThat(r2.isSuccess()).isTrue();
            assertThat(r2.getValue()).isFalse();
        }

        @Test
        void returnsTrueWhenExists() {
            when(mongoTemplate.exists(any(Query.class), eq(QuestionBanksPerUserDocument.class))).thenReturn(true);
            var res = repository.isQuestionBankActive(1001L, 2002L);
            assertThat(res.isSuccess()).isTrue();
            assertThat(res.getValue()).isTrue();
        }

        @Test
        void handlesDatabaseExceptionGracefully() {
            when(mongoTemplate.exists(any(Query.class), eq(QuestionBanksPerUserDocument.class)))
                    .thenThrow(new DataAccessResourceFailureException("DB down"));
            var res = repository.isQuestionBankActive(1001L, 2002L);
            assertThat(res.isFailure()).isTrue();
            assertThat(res.getErrorCode()).isEqualTo("DATABASE_ERROR");
        }
    }
}
