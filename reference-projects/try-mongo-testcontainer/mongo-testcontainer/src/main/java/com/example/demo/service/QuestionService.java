package com.example.demo.service;

import com.example.demo.model.Question;
import com.example.demo.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

	private final QuestionRepository questionRepository;

	@Autowired
	public QuestionService(QuestionRepository questionRepository) {
		this.questionRepository = questionRepository;
	}

	public Question save(Question question) {
		return questionRepository.save(question);
	}

	public Optional<Question> findById(String id) {
		return questionRepository.findById(id);
	}

	public List<Question> findAll() {
		return questionRepository.findAll();
	}

	public List<Question> findByAuthor(String author) {
		return questionRepository.findByAuthor(author);
	}

	public List<Question> findByTitle(String title) {
		return questionRepository.findByTitleContainingIgnoreCase(title);
	}

	public List<Question> findByTag(String tag) {
		return questionRepository.findByTagsContaining(tag);
	}

	public void deleteById(String id) {
		questionRepository.deleteById(id);
	}

	public long count() {
		return questionRepository.count();
	}
}
