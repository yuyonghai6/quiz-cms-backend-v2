package com.quizfun.questionbankquery.infrastructure.persistence.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxonomyDocument {
    private List<String> categories;
    private List<String> tags;
    private List<String> quizzes;
}
