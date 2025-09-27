package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.entities.EssayRubric;

import java.util.ArrayList;
import java.util.List;

public class EssayRubricDocument {
    private String description;
    private List<String> criteria;
    private Integer maxPoints;

    public static EssayRubricDocument fromValueObject(EssayRubric value) {
        EssayRubricDocument doc = new EssayRubricDocument();
        doc.description = value.getDescription();
        doc.criteria = new ArrayList<>(value.getCriteria());
        doc.maxPoints = value.getMaxPoints();
        return doc;
    }

    public EssayRubric toValueObject() {
        List<String> copy = criteria != null ? new ArrayList<>(criteria) : new ArrayList<>();
        return new EssayRubric(description, copy, maxPoints);
    }
}


