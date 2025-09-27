package com.quizfun.questionbank.infrastructure.persistence.documents;

import com.quizfun.questionbank.domain.entities.McqData;
import com.quizfun.questionbank.domain.entities.McqOption;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

public class McqDataDocument {
    private List<McqOptionDocument> options;

    @Field("shuffle_options")
    private boolean shuffleOptions;

    @Field("allow_multiple_correct")
    private boolean allowMultipleCorrect;

    @Field("allow_partial_credit")
    private boolean allowPartialCredit;

    @Field("time_limit_seconds")
    private Integer timeLimitSeconds;

    public static McqDataDocument fromValueObject(McqData value) {
        McqDataDocument doc = new McqDataDocument();
        doc.options = new ArrayList<>();
        for (McqOption option : value.getOptions()) {
            doc.options.add(McqOptionDocument.fromValueObject(option));
        }
        doc.shuffleOptions = value.isShuffleOptions();
        doc.allowMultipleCorrect = value.isAllowMultipleCorrect();
        doc.allowPartialCredit = value.isAllowPartialCredit();
        doc.timeLimitSeconds = value.getTimeLimitSeconds();
        return doc;
    }

    public McqData toValueObject() {
        List<McqOption> optionsVo = new ArrayList<>();
        if (options != null) {
            for (McqOptionDocument o : options) {
                optionsVo.add(o.toValueObject());
            }
        }
        return new McqData(optionsVo, shuffleOptions, allowMultipleCorrect, allowPartialCredit, timeLimitSeconds);
    }
}


