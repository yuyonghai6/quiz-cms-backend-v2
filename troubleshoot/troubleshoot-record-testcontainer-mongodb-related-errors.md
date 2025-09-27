### Quick triage checklist

- [ ] Docker works: `docker info` and `docker run --rm hello-world`
- [ ] No local Mongo on `localhost:27017`
- [ ] Tests use container URI from `@DynamicPropertySource` (no hardcoded localhost)
- [ ] Strong wait strategy in `TestContainersConfig` (listening port + readiness log)
- [ ] Prod Mongo auto-config excluded in tests; test `MongoTemplate`/`MongoTransactionManager` are `@Primary`
- [ ] Surefire single fork and no parallel tests (`forkCount=1`, `parallel=none`)
- [ ] `@SpringBootTest(webEnvironment = NONE)` and `@ActiveProfiles("test")` on config-heavy tests
- [ ] Singleton `MongoDBContainer` in use (avoid multiple concurrent containers)
- [ ] Container logs visible (optional `Slf4jLogConsumer`) when diagnosing
- [ ] Data setup/teardown uses the same `MongoTemplate` and doesn’t race readiness

### Testcontainers MongoDB troubleshooting record

This document consolidates what we applied that worked, typical root causes seen, and what is worth exploring next to fully stabilize MongoDB via Testcontainers across tests.

### Symptoms observed

- Connection refused/timeout to mapped host ports (e.g., `localhost:3277x`) during tests
- Spring `ApplicationContext` fails to boot (missing `MongoDatabaseFactory`, conflicting Mongo config)
- Flaky first-boot timing (replica set not ready when Spring wires `MongoTemplate`)
- ClassCastException in integration tests when asserting `_id` (`String` vs `ObjectId`)

### Things we implemented that helped

- **Strong container readiness**
  - Added wait strategy for listening port and readiness log message.
  - Increased startup timeouts to allow replica-set initiation to complete.
  - File: `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/TestContainersConfig.java`.

- **Bind Spring explicitly to the container URI**
  - Used `@DynamicPropertySource` to set `spring.data.mongodb.uri` from `mongoContainer.getReplicaSetUrl()` and set the test database name (e.g., `quiz_cms_test`).
  - Ensures no test uses `localhost:27017`.

- **Provide test-scoped Mongo beans and exclude prod auto-config**
  - Declared test `MongoTemplate` and `MongoTransactionManager` as `@Bean` and `@Primary` so they always override prod.
  - Excluded Spring Boot `MongoAutoConfiguration` and `MongoDataAutoConfiguration` in tests to prevent accidental prod wiring.
  - Excluded prod `MongoTransactionConfig` from component scanning in tests.
  - File: `TestContainersConfig.java`.

- **Stubbed missing collaborators to allow context boot**
  - Added test stubs for `QuestionBanksPerUserRepository` and `TaxonomySetRepository` so validators can build without external dependencies.

- **Stabilized test execution model**
  - Configured Surefire to run tests in a single fork and disabled JUnit parallelism to avoid container/context race conditions.
  - File: `internal-layer/question-bank/pom.xml` (Surefire `forkCount=1`, `parallel=none`).

- **Avoided ID casting issues in integration assertions**
  - Updated assertions to accept `_id` as `ObjectId` or `String` consistently.
  - File: `QuestionApplicationServiceIntegrationTest.java`.

- **Optional container reuse (when desired)**
  - Enabled `.withReuse(true)` in tests (and Surefire property), which can reduce churn between classes.
  - Note: global reuse requires `~/.testcontainers.properties` with `testcontainers.reuse.enable=true` and careful resource cleanup.

- **Singleton container (reduce multiple spin-ups)**
  - Introduced `SingletonMongoContainer` and used it from `TestContainersConfig` to minimize multiple starts/stops across config tests.
  - Files: `SingletonMongoContainer.java`, `TestContainersConfig.java`.

### Environmental prerequisites to verify

- **Docker up and accessible**
  - `docker info` succeeds, user is in `docker` group (or run with sudo).
  - `docker run --rm hello-world` works.

- **No stray local MongoDB**
  - Nothing listening on `localhost:27017` that could conflict or confuse driver selection.

### What likely caused earlier failures

- Docker daemon not running → Testcontainers couldn’t start Mongo.
- Spring booted prod Mongo auto-config instead of the Testcontainers beans → `MongoDatabaseFactory` missing or miswired.
- Driver attempting connection before replica set became primary → timeouts/connection refused.
- Tests connecting to `localhost:27017` instead of container URI (driver logs showed an extra client to `27017`).
- Race conditions when multiple contexts start containers concurrently (parallel test forks/contexts).

### Next items worth exploring/improving

- **Finish eliminating residual context clashes**
  - Keep `webEnvironment = NONE` and `@ActiveProfiles("test")` on config-heavy tests like `TestContainersMongoDBConfigurationTest`.
  - Ensure only `TestContainersConfig` provides Mongo beans in tests (no other config classes import prod Mongo beans).

- **Pin a stable Mongo version if driver/replica-set is brittle**
  - Try `mongo:6.0` vs `mongo:8.0` if you see intermittent readiness or wire-version mismatches.

- **Attach container log consumer for diagnostics**
  - We already used `Slf4jLogConsumer` in `TestContainersConfig` to surface Mongo logs when debugging.

- **Consider container reuse globally (optional)**
  - Add `~/.testcontainers.properties` with `testcontainers.reuse.enable=true` for developer machines to shorten cycles (ensure test data cleanup is robust).

- **Further test isolation / speed**
  - Prefer fewer `@SpringBootTest` contexts; favor slice tests or direct bean tests where possible.
  - Cache the Spring context across tests (default) but avoid rebuilding it with different config combinations.

- **Data loader hygiene**
  - Confirm `BaseTestConfiguration` uses the same `MongoTemplate` (from container) and data teardown never runs before container is ready.
  - If cleanup intermittently fails due to container restarts, move cleanup to per-class and/or guard with retry.

### Quick verification commands

```bash
# Docker health
docker info
docker run --rm hello-world

# Run a single config test class with logs
cd /home/joyfulday/nus-proj/quiz-cms/internal-layer/question-bank
mvn -Dtest=com.quizfun.questionbank.config.TestContainersMongoDBConfigurationTest -DskipITs test

# Run the main integration test that now passes
mvn -Dtest=com.quizfun.questionbank.application.services.QuestionApplicationServiceIntegrationTest -DskipITs test
```

### Key files updated

- `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/TestContainersConfig.java`
- `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/config/SingletonMongoContainer.java`
- `internal-layer/question-bank/pom.xml` (Surefire config)
- `internal-layer/question-bank/src/test/java/com/quizfun/questionbank/application/services/QuestionApplicationServiceIntegrationTest.java`

### Current status

- Targeted integration test against Testcontainers passes (operation reported as `created` on first persist).
- Remaining work is focused on making the Testcontainers configuration tests boot the Spring context reliably every time (ensuring only test Mongo beans load, no prod beans leak in, and one container lifecycle is used during the suite).


