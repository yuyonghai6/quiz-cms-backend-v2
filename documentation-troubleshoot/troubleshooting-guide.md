# Quiz CMS – Consolidated Troubleshooting Guide

This guide centralizes recurring issues and fixes across system tests, Spring Boot modules, and MongoDB/Testcontainers. It links out to deeper, topic-specific docs already in this repo.

Contents
- MongoDB indexes and profile isolation (tests vs production)
- Testcontainers MongoDB stability and hangs
- K6 functional and performance tests (text index, HTML reports)
- Allure reports not generating/opening
- Start/stop Spring Boot for system tests

## 1) MongoDB indexes and profile isolation (tests vs prod)

Symptoms
- E11000 duplicate key: ux_user_bank_source_id or ux_user_bank_question_taxonomy with nulls
- Command failed with error 86 (IndexKeySpecsConflict): existing index has same name but different partialFilterExpression
- ApplicationContext fails because prod MongoIndexConfig runs in tests

Root causes
- Production index config creates strict unique indexes while tests need relaxed partial unique indexes to allow seed data with null/missing fields
- Broad @ComponentScan or missing @Profile causes prod MongoIndexConfig to be created in the test context

Fix patterns that worked here
- Use Mongo Java driver to create indexes (not Spring’s deprecated APIs)
- In tests, create partial unique indexes with partialFilterExpression using $exists: true (works with MongoDB 8.0)
- Ensure production index beans are excluded from tests via profiles and component-scan filters
- Keep tests under @ActiveProfiles("test") and wire only test Mongo beans

Quick reference
- Test-side: create partial unique indexes
  - ux_user_bank_source_id on questions: { user_id: 1, question_bank_id: 1, source_question_id: 1 }, partial: each field { $exists: true }
  - ux_user_bank_question_taxonomy on question_taxonomy_relationships: { user_id, question_bank_id, question_id, taxonomy_type, taxonomy_id }, partial: each $exists: true
- Exclude in tests: MongoIndexConfig and any broad QuestionBankConfiguration that re-imports it

Related files
- internal-layer/question-bank/src/test/java/.../config/TestContainersConfig.java
- internal-layer/question-bank/src/main/java/.../infrastructure/configuration/MongoIndexConfig.java

## 2) Testcontainers MongoDB stability and hangs

Common issues
- mvn test hangs during discovery (no tests run) with Testcontainers
- Mongo container starts then disappears; connection refused to localhost:32xxx
- Flaky readiness; replica set not ready when Spring wires MongoTemplate

Root causes
- Static initialization of container (class load) blocks Maven/JUnit discovery
- Conflicting singleton lifecycle between custom container singletons and Spring contexts
- Overly complex wait strategies or parallel forks
- JaCoCo agent conflicts while Testcontainers initializes

Fixes applied
- Lazy container init with double-checked locking; no static blocks
- Prefer letting @Container + @Testcontainers manage lifecycle; or a safe singleton with reuse and a single shutdown hook
- Strong but simple wait: Wait.forListeningPort().withStartupTimeout(30–60s)
- Single fork, no parallel tests; optional container reuse
- Skip JaCoCo if it conflicts: -Djacoco.skip=true

Deep dives
- documentation-troubleshoot/troubleshoot-singleton-testcontainer-mongo.md
- documentation-troubleshoot/troubleshoot-record-testcontainer-mongodb-related-errors.md
- documentation-troubleshoot/troubleshoot-record-testcontainer-mongodb-related-errors-v2.md

## 3) K6 functional/performance tests and text index

Typical error
- text index required for $text query (code 27)

Fix
- In dev profile, ensure text index on questions.question_text at app startup (done in orchestration-layer dev config)
- Only one text index per collection; drop/recreate in dev if needed

Handy guide
- api-system-test/troubleshoot-tips-1019-question-list-functional-api-system-test.md

HTML/JSON reports and charts
- Functional and performance scripts emit HTML and JSON; if CDN import of k6-reporter fails, vendor the bundle and import locally
- Store reports under api-system-test/reports (gitignored)

Related files
- api-system-test/test-query-questions-functional.js
- performance-api-system-test/test-query-questions-perf.js

## 4) Allure report issues

Symptoms
- No report generated or serve shows nothing

Fix checklist
- Run tests in the module producing results
- Ensure allure-junit5 dependency and allure-maven plugin in that module
- Clean old allure-results and re-run tests
- Serve from the module path: mvn -pl internal-layer/question-bank io.qameta.allure:allure-maven:2.12.0:serve

Deep dive
- documentation-troubleshoot/troubleshoot-allure-report.md

## 5) Start/stop Spring Boot for system tests (dev)

Start app for K6
- mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
- Wait for: curl -sf http://localhost:8765/actuator/health | jq

Stop stale instances
- ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

Known dev settings
- Dev profile disables security validator for K6 and auto-ensures text index

## 6) Quick error signatures and one-liners

- E11000 duplicate key on ux_user_bank_source_id with nulls
  - Use partial unique index with $exists: true in tests
- IndexKeySpecsConflict (code 86) same name but different partialFilterExpression
  - Don’t load prod MongoIndexConfig in tests; ensure @Profile("!test") and exclude broad component scans
- text index required for $text query (code 27)
  - Ensure text index on questions.question_text (dev auto-ensure present)
- Maven hangs at test discovery with Testcontainers
  - Remove static container init; use lazy init and optionally -Djacoco.skip=true

## 7) Cross-references
- documentation-troubleshoot/troubleshoot-singleton-testcontainer-mongo.md
- documentation-troubleshoot/troubleshoot-record-testcontainer-mongodb-related-errors.md
- documentation-troubleshoot/troubleshoot-record-testcontainer-mongodb-related-errors-v2.md
- documentation-troubleshoot/troubleshoot-allure-report.md
- api-system-test/troubleshoot-tips-1019-question-list-functional-api-system-test.md
- api-system-test/troubleshoot-tips-upsert.md

Status
- Updated: 2025-10-12
- Maintainers: quiz-cms contributors
