package com.example.demo.repository;

import com.example.demo.model.Question;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends MongoRepository<Question, String> {

	List<Question> findByAuthor(String author);

	List<Question> findByTitleContainingIgnoreCase(String title);

	@Query("{'tags': { $in: [?0] }}")
	List<Question> findByTagsContaining(String tag);
}
