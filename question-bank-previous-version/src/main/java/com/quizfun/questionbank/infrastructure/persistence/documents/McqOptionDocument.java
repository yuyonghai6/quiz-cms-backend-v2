package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.entities.McqOption;
import org.springframework.data.mongodb.core.mapping.Field;

public class McqOptionDocument {
    private String key;
    private String text;
    @Field("is_correct")
    private boolean correct;
    private double points;

    public static McqOptionDocument fromValueObject(McqOption value) {
        McqOptionDocument doc = new McqOptionDocument();
        doc.key = value.getKey();
        doc.text = value.getText();
        doc.correct = value.isCorrect();
        doc.points = value.getPoints();
        return doc;
    }

    public McqOption toValueObject() {
        return new McqOption(key, text, correct, points);
    }
}


