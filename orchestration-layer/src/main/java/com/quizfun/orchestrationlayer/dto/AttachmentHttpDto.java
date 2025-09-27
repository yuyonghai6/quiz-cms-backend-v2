package com.quizfun.orchestrationlayer.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP DTO for attachment data in question upsert requests.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttachmentHttpDto {

    private String id;
    private String type;
    private String filename;
    private String url;
    private Long size;

    @JsonProperty("mime_type")
    private String mimeType;

    public AttachmentHttpDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
}