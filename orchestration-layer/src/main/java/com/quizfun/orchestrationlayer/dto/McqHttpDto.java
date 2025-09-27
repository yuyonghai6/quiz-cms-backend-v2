package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * HTTP DTO for MCQ data in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class McqHttpDto {

    @NotEmpty(message = "MCQ options are required")
    @Valid
    private List<McqOptionHttpDto> options;

    @JsonProperty("shuffle_options")
    private Boolean shuffleOptions;

    @JsonProperty("allow_multiple_correct")
    private Boolean allowMultipleCorrect;

    @JsonProperty("allow_partial_credit")
    private Boolean allowPartialCredit;

    @JsonProperty("time_limit_seconds")
    private Integer timeLimitSeconds;

    public McqHttpDto() {}

    public List<McqOptionHttpDto> getOptions() { return options; }
    public void setOptions(List<McqOptionHttpDto> options) { this.options = options; }

    public Boolean getShuffleOptions() { return shuffleOptions; }
    public void setShuffleOptions(Boolean shuffleOptions) { this.shuffleOptions = shuffleOptions; }

    public Boolean getAllowMultipleCorrect() { return allowMultipleCorrect; }
    public void setAllowMultipleCorrect(Boolean allowMultipleCorrect) { this.allowMultipleCorrect = allowMultipleCorrect; }

    public Boolean getAllowPartialCredit() { return allowPartialCredit; }
    public void setAllowPartialCredit(Boolean allowPartialCredit) { this.allowPartialCredit = allowPartialCredit; }

    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public static class McqOptionHttpDto {
        @NotNull(message = "Option ID is required")
        private Integer id;

        @NotNull(message = "Option text is required")
        private String text;

        @NotNull(message = "Is correct flag is required")
        @JsonProperty("is_correct")
        private Boolean isCorrect;

        private String explanation;

        public McqOptionHttpDto() {}

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public Boolean getIsCorrect() { return isCorrect; }
        public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }
    }
}