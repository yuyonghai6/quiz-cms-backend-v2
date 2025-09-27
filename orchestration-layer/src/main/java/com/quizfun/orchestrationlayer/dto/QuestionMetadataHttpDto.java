package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP DTO for question metadata in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionMetadataHttpDto {

    @JsonProperty("created_source")
    private String createdSource;

    @JsonProperty("last_modified")
    private String lastModified;

    private Integer version;

    @JsonProperty("author_id")
    private Long authorId;

    public QuestionMetadataHttpDto() {}

    public String getCreatedSource() { return createdSource; }
    public void setCreatedSource(String createdSource) { this.createdSource = createdSource; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
}