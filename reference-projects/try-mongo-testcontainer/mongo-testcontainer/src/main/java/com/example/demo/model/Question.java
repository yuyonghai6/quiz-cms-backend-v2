package com.example.demo.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document(collection = "questions")
public class Question {

	@Id
	private String id;

	@NotBlank(message = "Title cannot be blank")
	@Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
	@Indexed
	private String title;

	@NotBlank(message = "Content cannot be blank")
	@Size(min = 10, max = 2000, message = "Content must be between 10 and 2000 characters")
	private String content;

	@NotBlank(message = "Author cannot be blank")
	@Indexed
	private String author;

	private List<String> tags = new ArrayList<>();

	@CreatedDate
	private LocalDateTime createdAt;

	@LastModifiedDate
	private LocalDateTime updatedAt;

	public Question() {}

	public Question(String title, String content, String author, List<String> tags) {
		this.title = title;
		this.content = content;
		this.author = author;
		this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getContent() { return content; }
	public void setContent(String content) { this.content = content; }

	public String getAuthor() { return author; }
	public void setAuthor(String author) { this.author = author; }

	public List<String> getTags() { return tags != null ? new ArrayList<>(tags) : new ArrayList<>(); }
	public void setTags(List<String> tags) { this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>(); }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public LocalDateTime getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Question question = (Question) obj;
		return Objects.equals(id, question.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}
