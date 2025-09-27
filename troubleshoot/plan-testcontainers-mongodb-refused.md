## Plan: Fix Testcontainers MongoDB "connection refused" during `mvn test`

### Findings (current vs previous)

- Previous config (simple, working):

```java
@Container
static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:8.0")
        .withExposedPorts(27017)
        .withReuse(false);

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    mongoContainer.start();
    registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
    registry.add("spring.data.mongodb.database", () -> "quiz_cms_test");
}
```

- Current config (complex, failing):
  - Uses a singleton container with `withReuse(true)` and a JVM shutdown hook to stop it.
  - Starts the container in multiple places and mixes manual starts with JUnit lifecycle (`@Container`).
  - Custom `MongoClientSettings` with aggressive timeouts; extra wait/ping loop.

```java
// SingletonMongoContainer
INSTANCE = new MongoDBContainer("mongo:8.0")
  .withExposedPorts(27017)
  .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)))
  .withReuse(true);
INSTANCE.start();
// shutdown hook calls INSTANCE.stop()

// TestContainersConfig
@Container
static MongoDBContainer mongoContainer = SingletonMongoContainer.getInstance();

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
  if (!mongoContainer.isRunning()) mongoContainer.start();
  waitForMongoReady(mongoContainer.getReplicaSetUrl());
  registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl);
}

// Test class also declares:
@Container
static MongoDBContainer mongoContainer = TestContainersConfig.getMongoContainer();
```

### Likely root causes

1) Conflicting container lifecycle management (JUnit `@Container` vs manual start vs singleton) leads to race/duplication and intermittent refused connections.
2) `withReuse(true)` without globally enabling reuse, combined with a shutdown hook that stops the container, undermines stability and predictability.
3) Overly aggressive client timeouts and extra readiness loops can cause premature failures under load/CI.

### Remediation plan (phased)

Phase 1 — Simplify to a known-good baseline (recommended now)
1. Remove `SingletonMongoContainer` usage and delete the shutdown hook. Do not use `withReuse(true)`.
2. In `TestContainersConfig`, define one container like the previous version:
   - `@Container static MongoDBContainer new MongoDBContainer("mongo:8.0").withExposedPorts(27017).withReuse(false);`
3. Choose a single lifecycle owner:
   - EITHER: keep manual `start()` only in `@DynamicPropertySource` and do not annotate/declare `@Container` elsewhere;
   - OR: keep `@Container` on the field and remove all manual `start()` calls. (Pick one; do not mix.)
   - Recommendation: keep manual start in `@DynamicPropertySource` (mirrors the working version) and remove `@Container` from tests referencing the same instance.
4. Drop the custom `MongoClientSettings`; construct `MongoTemplate` from `MongoClients.create(mongoContainer.getReplicaSetUrl())` as before.
5. Keep properties wired via `registry.add("spring.data.mongodb.uri", mongoContainer::getReplicaSetUrl)` and database name `quiz_cms_test`.
6. Optional: increase startup timeout or add `waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 1).withStartupTimeout(Duration.ofSeconds(90)))` if startup is slow in CI.

Phase 2 — Hardening (only if needed after Phase 1 is green)
1. If faster runs are needed via reuse, enable it properly by creating `~/.testcontainers.properties` with `testcontainers.reuse.enable=true`, then set `.withReuse(true)` and remove any explicit stop hooks.
2. If tuning client timeouts, use conservative values (e.g., connectTimeout 10s, readTimeout 15s) and avoid duplicative readiness checks.
3. Add logging of container host/port and `getReplicaSetUrl()` at startup for easier diagnostics.
4. Verify Testcontainers and Docker versions are compatible with `mongo:8.0`.

Phase 3 — CI/local environment checks
1. Ensure Docker is reachable in the environment (`docker info`).
2. Clean up any orphaned containers/images only if reuse was previously enabled or failed half-starts are present.

### Rollback plan

If issues persist, temporarily revert `TestContainersConfig` to the previous working variant verbatim and re-run `mvn test` to confirm stability while investigating further.

### Acceptance criteria

- `mvn test` reliably starts a single MongoDB Testcontainer without connection refused errors.
- `MongoTemplate` can `ping` successfully; tests pass locally and in CI across repeated runs.

