package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.entities.EssayData;
import com.quizfun.questionbank.domain.entities.EssayRubric;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;

public class EssayDataDocument {
    @Field("min_words")
    private Integer minWordCount;
    @Field("max_words")
    private Integer maxWordCount;
    @Field("allow_rich_text")
    private boolean allowRichText;
    private EssayRubricDocument rubric;

    public static EssayDataDocument fromValueObject(EssayData value) {
        EssayDataDocument doc = new EssayDataDocument();
        doc.minWordCount = value.getMinWordCount();
        doc.maxWordCount = value.getMaxWordCount();
        doc.allowRichText = value.isAllowRichText();
        if (value.getRubric() != null) {
            doc.rubric = EssayRubricDocument.fromValueObject(value.getRubric());
        }
        return doc;
    }

    public EssayData toValueObject() {
        EssayRubric rubricVo = rubric != null ? rubric.toValueObject() : null;
        return new EssayData(minWordCount, maxWordCount, allowRichText, rubricVo);
    }
}


