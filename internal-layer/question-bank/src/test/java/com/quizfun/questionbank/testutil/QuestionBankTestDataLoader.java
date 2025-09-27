package com.quizfun.questionbank.testutil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.bson.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.io.IOException;
import java.util.List;

public class QuestionBankTestDataLoader {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public QuestionBankTestDataLoader(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void loadTestData() {
        loadQuestionBanksPerUser();
        loadTaxonomySets();
        loadExistingQuestions();
        loadExistingQuestionTaxonomyRelationships();
    }

    private void loadQuestionBanksPerUser() {
        try {
            var resource = new ClassPathResource("question-banks-per-user.json");
            if (resource.exists()) {
                var documents = objectMapper.readValue(resource.getInputStream(), List.class);
                for (Object doc : documents) {
                    var document = Document.parse(objectMapper.writeValueAsString(doc));
                    mongoTemplate.save(document, "question_banks_per_user");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load question banks test data", e);
        }
    }

    private void loadTaxonomySets() {
        try {
            var resource = new ClassPathResource("taxonomy-sets.json");
            if (resource.exists()) {
                var documents = objectMapper.readValue(resource.getInputStream(), List.class);
                for (Object doc : documents) {
                    var document = Document.parse(objectMapper.writeValueAsString(doc));
                    mongoTemplate.save(document, "taxonomy_sets");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load taxonomy sets test data", e);
        }
    }

    private void loadExistingQuestions() {
        try {
            var resource = new ClassPathResource("existing-questions.json");
            if (resource.exists()) {
                var documents = objectMapper.readValue(resource.getInputStream(), List.class);
                for (Object doc : documents) {
                    var document = Document.parse(objectMapper.writeValueAsString(doc));
                    mongoTemplate.save(document, "questions");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load existing questions test data", e);
        }
    }

    private void loadExistingQuestionTaxonomyRelationships() {
        try {
            var resource = new ClassPathResource("existing-question-taxonomy-relationships.json");
            if (resource.exists()) {
                var documents = objectMapper.readValue(resource.getInputStream(), List.class);
                for (Object doc : documents) {
                    var document = Document.parse(objectMapper.writeValueAsString(doc));
                    mongoTemplate.save(document, "question_taxonomy_relationships");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load question taxonomy relationships test data", e);
        }
    }

    public void cleanupTestData() {
        mongoTemplate.getCollection("questions").deleteMany(new Document());
        mongoTemplate.getCollection("taxonomy_sets").deleteMany(new Document());
        mongoTemplate.getCollection("question_banks_per_user").deleteMany(new Document());
        mongoTemplate.getCollection("question_taxonomy_relationships").deleteMany(new Document());
    }
}