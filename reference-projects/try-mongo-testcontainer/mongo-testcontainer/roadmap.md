# Roadmap: MongoDB + Testcontainers Integration (Spring Boot 3.x)

This roadmap follows `mongodb_testcontainer_guide_v2_claude.md` to flesh out the app with a MongoDB domain, repository, service, and comprehensive integration tests powered by Testcontainers.

## Prerequisites

- Java 21
- Spring Boot 3.1+ (project uses 3.5.5)
- Docker running (`docker info`)
- Maven (`./mvnw -v`)

## Milestones & Tasks

- [ ] M0: Verify baseline build
  - [ ] Run: `./mvnw -q -DskipTests package`
  - [ ] Ensure Docker works: `docker run --rm hello-world`

- [ ] M1: Dependencies and versions (in `pom.xml`)
  - [ ] Confirm present: `spring-boot-starter-data-mongodb`
  - [ ] Confirm present (test): `spring-boot-starter-test`, `spring-boot-testcontainers`, `org.testcontainers:junit-jupiter`, `org.testcontainers:mongodb`
  - [ ] Optional: add `spring-boot-starter-validation` if using Bean Validation in the model
  - [ ] Optional: manage Testcontainers via BOM if needed

- [ ] M2: Create domain model
  - [ ] Add `src/main/java/com/example/demo/model/Question.java` with fields: `id`, `title`, `content`, `author`, `tags`, `createdAt`, `updatedAt`
  - [ ] Use `@Document`, `@Id`, `@Indexed`, `@CreatedDate`, `@LastModifiedDate`
  - [ ] Add basic Bean Validation annotations (if validation starter is added)

- [ ] M3: Create repository
  - [ ] Add `src/main/java/com/example/demo/repository/QuestionRepository.java`
  - [ ] Extend `MongoRepository<Question, String>` and add finders: by author, by title contains (ignore case), by tag

- [ ] M4: Create service
  - [ ] Add `src/main/java/com/example/demo/service/QuestionService.java` with CRUD + finder methods delegating to repository

- [ ] M5: Enable auditing
  - [ ] Add `src/main/java/com/example/demo/config/MongoConfig.java` with `@EnableMongoAuditing`

- [ ] M6: Test properties
  - [ ] Add `src/test/resources/application-test.properties`:
    - `spring.data.mongodb.database=question_test_db`
    - `spring.data.mongodb.auto-index-creation=true`
    - Optional logging levels for `org.testcontainers`, `org.springframework.data.mongodb`, `com.example.demo`

- [ ] M7: Base Testcontainers configuration
  - [ ] Add `src/test/java/com/example/demo/config/BaseIntegrationTest.java`
  - [ ] Define static `MongoDBContainer` (e.g., `mongo:7.0`) with `@Container` and `@ServiceConnection`
  - [ ] Annotate with `@SpringBootTest` and `@ActiveProfiles("test")`

- [ ] M8: Repository tests
  - [ ] Add `src/test/java/com/example/demo/repository/QuestionRepositoryTest.java`
  - [ ] Use `@DataMongoTest` + `@Import(BaseIntegrationTest.class)` or extend base class
  - [ ] Cover save/find, author query, title contains, tag query, delete

- [ ] M9: Service tests
  - [ ] Add `src/test/java/com/example/demo/service/QuestionServiceTest.java`
  - [ ] Extend base class and cover create/read/update/delete, count, finders

- [ ] M10: Run tests and validate
  - [ ] Run: `./mvnw test`
  - [ ] Ensure Testcontainers pulls/starts `mongo` and tests pass

- [ ] M11: Optional developer ergonomics
  - [ ] Use `TestDemoApplication` (already present) to run app with Testcontainers locally
  - [ ] Align Mongo image tag used in tests (e.g., switch to `mongo:7.0` if desired)

## Commands

```bash
# Build without tests
./mvnw -q -DskipTests package

# Run all tests (starts MongoDB container automatically)
./mvnw test

# Run a specific test class
./mvnw test -Dtest="QuestionRepositoryTest"
```

## Acceptance Criteria

- Domain, repository, and service classes compile and are covered by tests
- Testcontainers starts a MongoDB container via `@ServiceConnection` with no manual URI config
- Repository and service tests pass end-to-end against real MongoDB (container)
- Auditing fields (`createdAt`, `updatedAt`) are populated where applicable

## Troubleshooting

- Docker not running: `docker info` should succeed; restart Docker if needed
- Port conflicts: Testcontainers maps random ports; conflicts are rare—check logs
- Slow first run: image pull can take time; subsequent runs are faster
- Image tag alignment: prefer a fixed tag like `mongo:7.0` for reproducibility
