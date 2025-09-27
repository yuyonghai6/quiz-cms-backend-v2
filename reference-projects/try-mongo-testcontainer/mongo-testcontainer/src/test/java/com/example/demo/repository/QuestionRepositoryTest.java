package com.example.demo.repository;

import com.example.demo.model.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@Testcontainers
@ActiveProfiles("test")
class QuestionRepositoryTest {

	@Container
	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:8.0"));

	@Autowired
	private QuestionRepository questionRepository;

	@BeforeEach
	void setUp() {
		questionRepository.deleteAll();
	}

	@Test
	void shouldSaveAndFindQuestion() {
		Question question = new Question(
			"How to use TestContainers?",
			"I need help setting up TestContainers with MongoDB for Spring Boot testing",
			"john.doe",
			Arrays.asList("testcontainers", "mongodb", "spring-boot")
		);

		Question savedQuestion = questionRepository.save(question);

		assertThat(savedQuestion.getId()).isNotNull();
		assertThat(savedQuestion.getTitle()).isEqualTo("How to use TestContainers?");
		assertThat(savedQuestion.getAuthor()).isEqualTo("john.doe");
		assertThat(savedQuestion.getTags()).containsExactly("testcontainers", "mongodb", "spring-boot");
	}

	@Test
	void shouldFindByAuthor() {
		Question question1 = new Question("Question 1", "Content 1", "author1", Arrays.asList("tag1"));
		Question question2 = new Question("Question 2", "Content 2", "author2", Arrays.asList("tag2"));
		Question question3 = new Question("Question 3", "Content 3", "author1", Arrays.asList("tag3"));

		questionRepository.saveAll(Arrays.asList(question1, question2, question3));

		List<Question> questions = questionRepository.findByAuthor("author1");

		assertThat(questions).hasSize(2);
		assertThat(questions)
				.extracting(Question::getTitle)
				.containsExactlyInAnyOrder("Question 1", "Question 3");
	}

	@Test
	void shouldFindByTitleIgnoreCase() {
		Question question1 = new Question("Spring Boot Tutorial", "Content", "author", Arrays.asList("spring"));
		Question question2 = new Question("MongoDB Guide", "Content", "author", Arrays.asList("mongo"));
		Question question3 = new Question("Advanced Spring Topics", "Content", "author", Arrays.asList("spring"));

		questionRepository.saveAll(Arrays.asList(question1, question2, question3));

		List<Question> questions = questionRepository.findByTitleContainingIgnoreCase("spring");

		assertThat(questions).hasSize(2);
		assertThat(questions)
				.extracting(Question::getTitle)
				.containsExactlyInAnyOrder("Spring Boot Tutorial", "Advanced Spring Topics");
	}

	@Test
	void shouldFindByTag() {
		Question question1 = new Question("Q1", "Content", "author", Arrays.asList("java", "spring"));
		Question question2 = new Question("Q2", "Content", "author", Arrays.asList("mongodb", "database"));
		Question question3 = new Question("Q3", "Content", "author", Arrays.asList("java", "testing"));

		questionRepository.saveAll(Arrays.asList(question1, question2, question3));

		List<Question> questions = questionRepository.findByTagsContaining("java");

		assertThat(questions).hasSize(2);
		assertThat(questions)
				.extracting(Question::getTitle)
				.containsExactlyInAnyOrder("Q1", "Q3");
	}

	@Test
	void shouldDeleteQuestion() {
		Question question = new Question("To Delete", "Content", "author", Arrays.asList("temp"));
		Question savedQuestion = questionRepository.save(question);

		questionRepository.deleteById(savedQuestion.getId());

		Optional<Question> deletedQuestion = questionRepository.findById(savedQuestion.getId());
		assertTrue(deletedQuestion.isEmpty());
	}
}
