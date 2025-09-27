package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.entities.TrueFalseData;
import org.springframework.data.mongodb.core.mapping.Field;

public class TrueFalseDataDocument {
    @Field("correct_answer")
    private boolean correctAnswer;
    private String explanation;
    @Field("time_limit_seconds")
    private Integer timeLimitSeconds;

    public static TrueFalseDataDocument fromValueObject(TrueFalseData value) {
        TrueFalseDataDocument doc = new TrueFalseDataDocument();
        doc.correctAnswer = value.getCorrectAnswer();
        doc.explanation = value.getExplanation();
        doc.timeLimitSeconds = value.getTimeLimitSeconds();
        return doc;
    }

    public TrueFalseData toValueObject() {
        return new TrueFalseData(correctAnswer, explanation, timeLimitSeconds);
    }
}


