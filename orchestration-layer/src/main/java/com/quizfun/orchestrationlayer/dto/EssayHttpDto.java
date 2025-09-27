package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * HTTP DTO for essay data in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EssayHttpDto {

    @NotNull(message = "Essay prompt is required")
    private String prompt;

    @JsonProperty("min_words")
    @Min(value = 0, message = "Minimum words cannot be negative")
    private Integer minWords;

    @JsonProperty("max_words")
    @Min(value = 1, message = "Maximum words must be positive")
    private Integer maxWords;

    @Valid
    private List<RubricItemHttpDto> rubric;

    @JsonProperty("allow_file_upload")
    private Boolean allowFileUpload;

    public EssayHttpDto() {}

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Integer getMinWords() { return minWords; }
    public void setMinWords(Integer minWords) { this.minWords = minWords; }

    public Integer getMaxWords() { return maxWords; }
    public void setMaxWords(Integer maxWords) { this.maxWords = maxWords; }

    public List<RubricItemHttpDto> getRubric() { return rubric; }
    public void setRubric(List<RubricItemHttpDto> rubric) { this.rubric = rubric; }

    public Boolean getAllowFileUpload() { return allowFileUpload; }
    public void setAllowFileUpload(Boolean allowFileUpload) { this.allowFileUpload = allowFileUpload; }

    public static class RubricItemHttpDto {
        @NotNull(message = "Rubric criteria is required")
        private String criteria;

        @JsonProperty("max_points")
        @Min(value = 1, message = "Max points must be positive")
        private Integer maxPoints;

        private String description;

        public RubricItemHttpDto() {}

        public String getCriteria() { return criteria; }
        public void setCriteria(String criteria) { this.criteria = criteria; }

        public Integer getMaxPoints() { return maxPoints; }
        public void setMaxPoints(Integer maxPoints) { this.maxPoints = maxPoints; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}