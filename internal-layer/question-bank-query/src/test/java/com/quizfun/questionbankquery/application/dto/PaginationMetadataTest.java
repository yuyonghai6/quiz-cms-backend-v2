package com.quizfun.questionbankquery.application.dto;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1011.query-dtos-and-pagination-logic")
@DisplayName("PaginationMetadata Tests")
class PaginationMetadataTest {

    @Test
    @DisplayName("Should create PaginationMetadata with valid data")
    void shouldCreatePaginationMetadataWithValidData() {
        PaginationMetadata metadata = new PaginationMetadata(2, 20, 150, 8);
        assertThat(metadata.currentPage()).isEqualTo(2);
        assertThat(metadata.pageSize()).isEqualTo(20);
        assertThat(metadata.totalItems()).isEqualTo(150);
        assertThat(metadata.totalPages()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should calculate total pages correctly")
    void shouldCalculateTotalPagesCorrectly() {
        long totalItems = 95;
        int pageSize = 20;
        PaginationMetadata metadata = new PaginationMetadata(0, pageSize, totalItems, (int) Math.ceil((double) totalItems / pageSize));
        assertThat(metadata.totalPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle zero total items")
    void shouldHandleZeroTotalItems() {
        PaginationMetadata metadata = new PaginationMetadata(0, 20, 0, 0);
        assertThat(metadata.totalPages()).isZero();
        assertThat(metadata.totalItems()).isZero();
    }

    @Test
    @DisplayName("Should create pagination metadata using factory method")
    void shouldCreatePaginationMetadataUsingFactoryMethod() {
        PaginationMetadata metadata = PaginationMetadata.of(2, 20, 95);
        assertThat(metadata.currentPage()).isEqualTo(2);
        assertThat(metadata.pageSize()).isEqualTo(20);
        assertThat(metadata.totalItems()).isEqualTo(95);
        assertThat(metadata.totalPages()).isEqualTo(5);
    }
}
