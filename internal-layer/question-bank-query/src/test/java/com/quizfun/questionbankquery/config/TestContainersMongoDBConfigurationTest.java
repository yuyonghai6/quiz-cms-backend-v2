package com.quizfun.questionbankquery.config;

import com.mongodb.ReadPreference;
import com.quizfun.questionbankquery.TestQuestionBankQueryApplication;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that validate the Testcontainers MongoDB setup for the query module.
 *
 * <p>These tests confirm that the container boots correctly, Spring Boot auto-configures
 * {@link MongoTemplate}, dynamic properties are applied, read preference remains primary,
 * and basic CRUD operations succeed against the ephemeral database.</p>
 */
@Epic("Use Case Query List of Questions of Question Bank")
@Story("1010.testcontainer-mongodb-setup-and-configuration")
@Testcontainers
@SpringBootTest(classes = TestQuestionBankQueryApplication.class)
@ActiveProfiles("test")
@DisplayName("Testcontainer MongoDB Configuration Tests")
class TestContainersMongoDBConfigurationTest {

    private static final String MONGO_IMAGE_VERSION = "mongo:8.0";
    private static final int MONGO_INTERNAL_PORT = 27017;
    private static final String TEST_COLLECTION_NAME = "test_collection";
    private static final String TEST_FIELD_NAME = "testField";
    private static final String TEST_FIELD_VALUE = "testValue";

    @Container
    @SuppressWarnings("resource")
    private static final MongoDBContainer mongoContainer = new MongoDBContainer(MONGO_IMAGE_VERSION)
        .withReuse(false);

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.read-preference", () -> "primary");
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    @DisplayName("Should start MongoDB container successfully")
    void shouldStartMongoDBContainerSuccessfully() {
        assertThat(mongoContainer.isRunning()).isTrue();
    }

    @Test
    @DisplayName("Should connect to MongoDB with correct URI")
    void shouldConnectToMongoDBWithCorrectURI() {
        String replicaSetUrl = mongoContainer.getReplicaSetUrl();
        assertThat(replicaSetUrl).isNotNull().contains("mongodb://");
    }

    @Test
    @DisplayName("Should auto-configure MongoTemplate bean")
    void shouldAutoConfigureMongoTemplateBean() {
        assertThat(mongoTemplate).isNotNull();
    }

    @Test
    @DisplayName("Should perform basic CRUD operations on test collection")
    void shouldPerformBasicCrudOperationsOnTestCollection() {
        Document testDocument = new Document(TEST_FIELD_NAME, TEST_FIELD_VALUE)
                .append("timestamp", System.currentTimeMillis());

        mongoTemplate.insert(testDocument, TEST_COLLECTION_NAME);

        Document retrievedDoc = mongoTemplate.findOne(
                Query.query(Criteria.where(TEST_FIELD_NAME).is(TEST_FIELD_VALUE)),
                Document.class,
                TEST_COLLECTION_NAME
        );

        assertThat(retrievedDoc).isNotNull();
        assertThat(retrievedDoc.getString(TEST_FIELD_NAME)).isEqualTo(TEST_FIELD_VALUE);

        mongoTemplate.dropCollection(TEST_COLLECTION_NAME);
    }

    @Test
    @DisplayName("Should verify read preference is set to primary")
    void shouldVerifyReadPreferenceIsSetToPrimary() {
        ReadPreference readPreference = mongoTemplate.getDb().getReadPreference();
        assertThat(readPreference).isNotNull();
        assertThat(readPreference.getName()).isEqualTo(ReadPreference.primary().getName());

        assertThat(mongoTemplate.getCollectionNames()).isNotNull();
    }

    @Test
    @DisplayName("Should dynamically map container ports")
    void shouldDynamicallyMapContainerPorts() {
        Integer mappedPort = mongoContainer.getFirstMappedPort();
        assertThat(mappedPort).isNotNull().isGreaterThan(0);
        assertThat(mongoContainer.getExposedPorts()).contains(MONGO_INTERNAL_PORT);
    }
}
