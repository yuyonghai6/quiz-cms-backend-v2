package com.example.demo.service;

import com.example.demo.config.BaseIntegrationTest;
import com.example.demo.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
class QuestionServiceTest extends BaseIntegrationTest {

	@Autowired
	private QuestionService questionService;

	@BeforeEach
	void setUp() {
		questionService.findAll().forEach(q -> questionService.deleteById(q.getId()));
	}

	@Test
	void shouldCreateAndRetrieveQuestion() {
		Question question = new Question(
			"How to setup MongoDB TestContainers?",
			"I want to create integration tests for my Spring Boot app using MongoDB TestContainers. Can someone provide a step-by-step guide?",
			"developer123",
			Arrays.asList("mongodb", "testcontainers", "spring-boot", "integration-testing")
		);

		Question savedQuestion = questionService.save(question);

		assertThat(savedQuestion.getId()).isNotNull();
		assertThat(savedQuestion.getTitle()).isEqualTo(question.getTitle());
		assertThat(savedQuestion.getAuthor()).isEqualTo(question.getAuthor());
		assertThat(savedQuestion.getTags()).hasSize(4);
		assertThat(savedQuestion.getCreatedAt()).isNotNull();

		Optional<Question> retrievedQuestion = questionService.findById(savedQuestion.getId());
		assertThat(retrievedQuestion).isPresent();
		Question retrieved = retrievedQuestion.get();
		assertThat(retrieved.getTitle()).isEqualTo(savedQuestion.getTitle());
		assertThat(retrieved.getContent()).isEqualTo(savedQuestion.getContent());
		assertThat(retrieved.getAuthor()).isEqualTo(savedQuestion.getAuthor());
		assertThat(retrieved.getTags()).containsExactlyElementsOf(savedQuestion.getTags());
	}

	@Test
	void shouldUpdateQuestion() {
		Question originalQuestion = new Question(
			"Original Title",
			"Original Content",
			"author",
			Arrays.asList("original")
		);
		Question savedQuestion = questionService.save(originalQuestion);

		savedQuestion.setTitle("Updated Title");
		savedQuestion.setContent("Updated Content with more comprehensive details");
		savedQuestion.setTags(Arrays.asList("updated", "modified", "enhanced"));

		Question updatedQuestion = questionService.save(savedQuestion);

		assertThat(updatedQuestion.getId()).isEqualTo(savedQuestion.getId());
		assertThat(updatedQuestion.getTitle()).isEqualTo("Updated Title");
		assertThat(updatedQuestion.getContent()).isEqualTo("Updated Content with more comprehensive details");
		assertThat(updatedQuestion.getTags()).containsExactly("updated", "modified", "enhanced");
		assertThat(updatedQuestion.getUpdatedAt()).isAfter(updatedQuestion.getCreatedAt());
	}

	@Test
	void shouldFindByAuthorAndTitleAndTag() {
		List<Question> questions = Arrays.asList(
			new Question("Java Concurrency", "Deep dive into Java concurrency", "expert_dev", Arrays.asList("java", "concurrency")),
			new Question("Spring Boot Guide", "Enterprise Spring Boot guidelines", "senior_dev", Arrays.asList("spring-boot", "best-practices")),
			new Question("MongoDB Performance", "Optimizing MongoDB performance", "db_admin", Arrays.asList("mongodb", "performance"))
		);
		questions.forEach(questionService::save);

		assertThat(questionService.findByAuthor("expert_dev")).hasSize(1);
		assertThat(questionService.findByTitle("spring")).hasSize(1);
		assertThat(questionService.findByTag("mongodb")).hasSize(1);
		assertThat(questionService.count()).isEqualTo(3);
	}

	@Test
	void shouldDeleteQuestion() {
		Question question = new Question("To be deleted", "Content", "author", Arrays.asList("temp"));
		Question savedQuestion = questionService.save(question);

		assertThat(questionService.count()).isEqualTo(1);
		assertThat(questionService.findById(savedQuestion.getId())).isPresent();

		questionService.deleteById(savedQuestion.getId());

		assertThat(questionService.count()).isEqualTo(0);
		assertThat(questionService.findById(savedQuestion.getId())).isEmpty();
	}
}
