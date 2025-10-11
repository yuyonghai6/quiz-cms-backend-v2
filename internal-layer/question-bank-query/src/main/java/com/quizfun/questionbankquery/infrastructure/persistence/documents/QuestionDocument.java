package com.quizfun.questionbankquery.infrastructure.persistence.documents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questions")
public class QuestionDocument {

    @Id
    private String id;

    @Field("question_id")
    private Long questionId;

    @Field("user_id")
    private Long userId;

    @Field("question_bank_id")
    private Long questionBankId;

    @Field("question_text")
    private String questionText;

    @Field("question_type")
    private String questionType;

    @Field("difficulty_level")
    private String difficultyLevel;

    @Field("type_specific_data")
    private Map<String, Object> typeSpecificData;

    @Field("taxonomy")
    private TaxonomyDocument taxonomy;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;
}
