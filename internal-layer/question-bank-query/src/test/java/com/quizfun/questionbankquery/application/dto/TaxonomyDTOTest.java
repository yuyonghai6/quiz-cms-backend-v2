package com.quizfun.questionbankquery.application.dto;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Use Case Query List of Questions of Question Bank")
@Story("1011.query-dtos-and-pagination-logic")
@DisplayName("TaxonomyDTO Tests")
class TaxonomyDTOTest {

    @Test
    @DisplayName("Should create TaxonomyDTO with all fields")
    void shouldCreateTaxonomyDTOWithAllFields() {
        List<String> categories = List.of("Math", "Algebra", "Linear Equations");
        List<String> tags = List.of("equations", "solve-for-x");
        List<String> quizzes = List.of("midterm-2024", "practice-set-1");

        TaxonomyDTO dto = new TaxonomyDTO(categories, tags, quizzes);

        assertThat(dto.categories()).isEqualTo(categories);
        assertThat(dto.tags()).isEqualTo(tags);
        assertThat(dto.quizzes()).isEqualTo(quizzes);
    }

    @Test
    @DisplayName("Should create TaxonomyDTO with empty lists")
    void shouldCreateTaxonomyDTOWithEmptyLists() {
        List<String> empty = List.of();

        TaxonomyDTO dto = new TaxonomyDTO(empty, empty, empty);

        assertThat(dto.categories()).isEmpty();
        assertThat(dto.tags()).isEmpty();
        assertThat(dto.quizzes()).isEmpty();
    }
}
