# Troubleshoot Tips — 1019 K6 Functional API System Test (Query Questions)

This guide consolidates common issues and fixes when running the K6 functional API system test for the Query Questions endpoint against a real MongoDB replica set with Spring Boot running in the `dev` profile.

Related docs and scripts:
- WBS: `usecases/query-list-of-questions-of-question-bank/wbs/1019.k6-functional-api-system-test.md`
- Script: `api-system-test/test-query-questions-functional.js`
- Dev config: `orchestration-layer/src/main/resources/application-dev.properties`
- Dev auto-index: `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/configuration/MongoTextIndexConfig.java`

## Quick checklist
- App started with `dev` profile and health is UP
- Real MongoDB replica set reachable, PRIMARY available
- Test data exists (userId and questionBankId)
- Text index on `questions.question_text` ensured (dev profile does this automatically)
- K6 environment variables set (BASE_URL, USER_ID, QUESTION_BANK_ID)

## Start/stop and readiness

Start the app (Terminal 1):
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```
Wait for health:
```bash
until curl -sf http://localhost:8765/actuator/health | grep -q '"status":"UP"'; do echo 'waiting for app...'; sleep 2; done; echo 'app is UP'
```
Stop stale instances:
```bash
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9
```

Run K6 (Terminal 2):
```bash
k6 run \
  -e BASE_URL=http://localhost:8765 \
  -e USER_ID=1760085803933 \
  -e QUESTION_BANK_ID=1760085804015000 \
  api-system-test/test-query-questions-functional.js
```

## Common issues and fixes

1) Error: text index required for $text query (code 27)
- Symptoms: 500 error from server; logs show IndexNotFound (code 27)
- Cause: Full-text queries require a text index on `questions.question_text`.
- Fix in dev: The app now auto-ensures the text index at startup when `profile=dev` via `MongoTextIndexConfig`.
- Manual fallback (mongo shell):
  ```javascript
  use <your_db_name>
  db.questions.createIndex({ question_text: "text" })
  db.questions.getIndexes()
  ```
- Note: Only one text index per collection is allowed. If an incompatible text index already exists, drop or recreate appropriately (dev only).

2) 401/403 Unauthorized/Forbidden
- Cause: Security validator still active.
- Fix: Ensure `-Dspring-boot.run.profiles=dev` so `security.context.validator.enabled=false` applies.

3) Connection refused/timeouts to MongoDB
- Causes: Replica set not running, ports not exposed, wrong hosts in `application-dev.properties`, firewall.
- Fixes:
  - Verify Docker Compose or external replica set is up and primary:
    ```bash
    mongosh --host <host> --port <port> --username <user> --password <pwd>
    rs.status()
    ```
  - Confirm hosts/ports/credentials match `application-dev.properties`.

4) Not primary / Read preference issues
- Symptoms: Errors when writes/reads require primary node.
- Fix: Ensure a PRIMARY exists; adjust read preference in `application-dev.properties` if needed.

5) Empty results when expecting data
- Causes: Test data not seeded for given USER_ID/QUESTION_BANK_ID; filters too restrictive.
- Fix: Confirm data exists:
  ```javascript
  use <your_db_name>
  db.questions.find({ user_id: 1760085803933, question_bank_id: 1760085804015000 }).limit(5)
  ```
- If empty is acceptable for the scenario, the test will still pass as long as response shape is valid.

6) 400 Bad Request for pagination
- Cause: Invalid page/size (e.g., negative page, size too large or zero).
- Fix: Use `page>=0` and reasonable `size` (e.g., 10–50). Note: Page is 0-based; pages beyond last return empty `questions` with valid `pagination`.

7) Port conflicts or wrong BASE_URL
- Symptom: K6 fails to connect or hits wrong service.
- Fix: Ensure app listens on `8765` and K6’s `BASE_URL` matches. Use health endpoint to verify.

8) K6 HTML reporter fetch blocked
- Symptom: Import of reporter URL fails under strict networks.
- Fix: Use network that allows GitHub raw fetch, or vendor the reporter bundle internally and import from a local path.

9) HTTPS/TLS or self-signed endpoints
- Symptom: TLS errors from K6.
- Fix: Prefer HTTP for local dev; if HTTPS is required with self-signed certs, consider `insecureSkipTLSVerify` in K6 options (functional only).

## Diagnostics

- Health check:
  ```bash
  curl -s http://localhost:8765/actuator/health | jq
  ```
- Tail app logs (in the run terminal): look for startup profile, Mongo connection string, and the index ensure log:
  - "Ensured text index on questions.question_text for dev profile"
- Inspect Mongo indexes:
  ```javascript
  use <your_db_name>
  db.questions.getIndexes()
  ```
- Verify data presence (replace IDs as needed):
  ```javascript
  db.questions.countDocuments({ user_id: 1760085803933, question_bank_id: 1760085804015000 })
  db.questions.find({ question_text: /equation/i }).limit(5)
  ```

## Known behaviors (important for validating)
- Text search semantics: AND across terms, case-insensitive; relevance sorting available when `searchText` present.
- Category filters: categories combined with AND.
- Tags/quizzes in taxonomy: combined with OR within their own set.
- Pagination: 0-based pages; pages beyond last are valid and return empty `questions` with `pagination` metadata.

## Recovery playbook
1. Kill stale app processes.
2. Clean compile (optional but useful on config changes):
   ```bash
   mvn clean compile
   ```
3. Start app with `dev` profile and wait for health UP.
4. Re-run K6 with correct env vars.
5. If text search still errors, verify the dev auto-index log and check Mongo indexes; manually create index if necessary (dev only).

## References
- WBS: `usecases/query-list-of-questions-of-question-bank/wbs/1019.k6-functional-api-system-test.md`
- Script: `api-system-test/test-query-questions-functional.js`
- Dev config: `orchestration-layer/src/main/resources/application-dev.properties`
- Auto index (dev): `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/configuration/MongoTextIndexConfig.java`