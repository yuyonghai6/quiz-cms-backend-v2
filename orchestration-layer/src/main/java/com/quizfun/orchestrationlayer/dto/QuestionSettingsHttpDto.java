package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP DTO for question settings in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionSettingsHttpDto {

    @JsonProperty("randomize_display")
    private Boolean randomizeDisplay;

    @JsonProperty("show_explanation_immediately")
    private Boolean showExplanationImmediately;

    @JsonProperty("allow_review")
    private Boolean allowReview;

    public QuestionSettingsHttpDto() {}

    public Boolean getRandomizeDisplay() { return randomizeDisplay; }
    public void setRandomizeDisplay(Boolean randomizeDisplay) { this.randomizeDisplay = randomizeDisplay; }

    public Boolean getShowExplanationImmediately() { return showExplanationImmediately; }
    public void setShowExplanationImmediately(Boolean showExplanationImmediately) { this.showExplanationImmediately = showExplanationImmediately; }

    public Boolean getAllowReview() { return allowReview; }
    public void setAllowReview(Boolean allowReview) { this.allowReview = allowReview; }
}