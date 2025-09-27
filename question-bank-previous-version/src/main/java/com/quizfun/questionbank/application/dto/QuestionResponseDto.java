package com.quizfun.questionbank.application.dto;

import java.time.Instant;

public class QuestionResponseDto {
    private String id;
    private String sourceQuestionId;
    private String questionType;
    private String title;
    private String content;
    private Integer points;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public QuestionResponseDto() {}

    public QuestionResponseDto(String id, String sourceQuestionId, String questionType,
                              String title, String content, Integer points, String status,
                              Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sourceQuestionId = sourceQuestionId;
        this.questionType = questionType;
        this.title = title;
        this.content = content;
        this.points = points;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceQuestionId() {
        return sourceQuestionId;
    }

    public void setSourceQuestionId(String sourceQuestionId) {
        this.sourceQuestionId = sourceQuestionId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}