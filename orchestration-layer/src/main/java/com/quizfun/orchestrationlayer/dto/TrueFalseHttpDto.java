package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * HTTP DTO for true/false data in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrueFalseHttpDto {

    @NotNull(message = "True/False statement is required")
    private String statement;

    @NotNull(message = "Correct answer is required")
    @JsonProperty("correct_answer")
    private Boolean correctAnswer;

    private String explanation;

    public TrueFalseHttpDto() {}

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public Boolean getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(Boolean correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}