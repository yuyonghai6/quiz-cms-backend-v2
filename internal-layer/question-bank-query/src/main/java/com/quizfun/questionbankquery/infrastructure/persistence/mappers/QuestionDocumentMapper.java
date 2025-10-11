package com.quizfun.questionbankquery.infrastructure.persistence.mappers;

import com.quizfun.questionbankquery.application.dto.QuestionDTO;
import com.quizfun.questionbankquery.application.dto.TaxonomyDTO;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.QuestionDocument;
import com.quizfun.questionbankquery.infrastructure.persistence.documents.TaxonomyDocument;
import org.springframework.stereotype.Component;

@Component
public class QuestionDocumentMapper {

    public QuestionDTO toDTO(QuestionDocument document) {
        if (document == null) return null;
        return new QuestionDTO(
                document.getQuestionId(),
                document.getQuestionText(),
                document.getQuestionType(),
                document.getDifficultyLevel(),
                document.getTypeSpecificData(),
                toTaxonomyDTO(document.getTaxonomy()),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private TaxonomyDTO toTaxonomyDTO(TaxonomyDocument taxonomy) {
        if (taxonomy == null) return null;
        return new TaxonomyDTO(
                taxonomy.getCategories(),
                taxonomy.getTags(),
                taxonomy.getQuizzes()
        );
    }
}
