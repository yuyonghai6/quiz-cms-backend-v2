# Resolution: Missing Service Logs in QuestionApplicationService.upsertQuestion

**Date:** October 13, 2025  
**Status:** ✅ **RESOLVED**

---

## Problem Summary

The `upsertQuestion` method in `QuestionApplicationService.java` contained logger statements (e.g., `logger.warn("upsertQuestion method start in QuestionApplicationService.java")`), but these logs did not appear in Spring Boot stdout when the API endpoint was called via k6 tests.

---

## Root Cause

**Incomplete Build Process**: When running `mvn spring-boot:run -pl orchestration-layer`, Maven only recompiles the orchestration-layer module itself, **not its dependencies** in internal-layer/question-bank. 

The source code contained the log statements, but:
1. The internal-layer/question-bank module was not recompiled after adding the logs.
2. The orchestration-layer was using an older JAR of internal-layer/question-bank from the local Maven repository.
3. The compiled `.class` file timestamp (14:38) was earlier than the Spring Boot start time (14:43), confirming stale code was being used.

---

## Solution

### Step 1: Enhanced Logging Configuration
Updated `orchestration-layer/src/main/resources/application-dev.properties`:

```properties
# Enable DEBUG logs for question bank service to troubleshoot invocation
logging.level.com.quizfun.questionbank=DEBUG
logging.level.com.quizfun.orchestrationlayer=DEBUG
```

### Step 2: Full Rebuild with Dependencies
Instead of:
```bash
mvn clean compile
```

Use:
```bash
mvn clean install -DskipTests
```

This ensures:
- All modules (including internal-layer) are recompiled
- JARs are installed to the local Maven repository
- orchestration-layer picks up the latest dependencies

### Step 3: Restart Spring Boot
```bash
# Kill any running instance
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# Start with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

---

## Verification

### Test Execution
```bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js
```

### Log Output Confirmed
```
2025-10-13T14:48:18.107+08:00  WARN 434387 --- [orchestration-layer] [nio-8765-exec-1] c.q.q.a.s.QuestionApplicationService     : upsertQuestion method start in QuestionApplicationService.java
2025-10-13T14:48:18.108+08:00  INFO 434387 --- [orchestration-layer] [nio-8765-exec-1] c.q.q.a.s.QuestionApplicationService     : Starting question upsert process for source ID: 0199dc53-b6cf-7165-ad2f-5f7512e97bdd by user: 1760085903963 in question bank: 1760085904098000
2025-10-13T14:48:18.267+08:00  WARN 434387 --- [orchestration-layer] [nio-8765-exec-1] c.q.q.a.s.QuestionApplicationService     : upsertQuestion before step 2 in QuestionApplicationService.java
2025-10-13T14:48:18.280+08:00  WARN 434387 --- [orchestration-layer] [nio-8765-exec-1] c.q.q.a.s.QuestionApplicationService     : upsertQuestion before step 3 in QuestionApplicationService.java
```

### MongoDB Data Confirmed
```bash
mongosh "mongodb://root:***@localhost:27017/quizfun?authSource=admin" --quiet --eval \
  'db.questions.findOne({source_question_id: "0199dc53-b6cf-7165-ad2f-5f7512e97bdd"}, 
   {_id:1, source_question_id:1, display_order:1, solution_explanation:1, title:1})'
```

**Result:**
```json
{
  "_id": ObjectId("68eca0b2b8c2382b68b1355e"),
  "source_question_id": "0199dc53-b6cf-7165-ad2f-5f7512e97bdd",
  "title": "Simple MCQ Question",
  "display_order": 1,
  "solution_explanation": "<p>Basic arithmetic</p>"
}
```

✅ **Both `display_order` and `solution_explanation` are now persisted correctly!**

---

## Key Learnings

### 1. Multi-Module Build Pitfalls
- `mvn spring-boot:run -pl <module>` does **not** rebuild dependencies.
- Always use `mvn clean install` or `mvn clean install -DskipTests` when changes are made to shared libraries or internal modules.
- Check `.class` file timestamps to verify code is freshly compiled.

### 2. Logging Configuration
- Spring Boot dev profile should have explicit DEBUG/WARN logging for custom packages.
- Use package-level logging configuration:
  ```properties
  logging.level.com.quizfun.questionbank=DEBUG
  ```
- Default root logging (`WARN`) can suppress important application logs.

### 3. Service Invocation Was Always Working
- The handler **was** correctly calling the service method.
- The service method **was** executing successfully.
- The issue was purely about **log visibility** due to stale compiled code.

### 4. Verification Strategy
- Don't rely solely on log presence—also check:
  - Handler logs (appeared correctly even with stale internal layer)
  - Database persistence (fields were actually being saved)
  - HTTP responses (k6 tests passed)
- Logs are a diagnostic tool, not the source of truth for functionality.

---

## Recommended Workflow for Future Debugging

### Quick Restart During Development
```bash
# 1. Kill stale process
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# 2. Rebuild all modules (critical!)
mvn clean install -DskipTests

# 3. Start with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### Verify Latest Code is Running
```bash
# Check compilation timestamp
ls -lh internal-layer/question-bank/target/classes/com/quizfun/questionbank/application/services/QuestionApplicationService.class

# Compare with current time
date
```

### Monitor Logs in Real-Time
```bash
# Option 1: Redirect to file and tail
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev > spring-boot.log 2>&1 &
tail -f spring-boot.log | grep -E "(WARN|ERROR|upsertQuestion)"

# Option 2: Use grep filter directly
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev 2>&1 | grep -E "(upsertQuestion|QuestionApplicationService)"
```

---

## Related Documentation

- [Troubleshooting Plan](./troubleshoot-missing-service-logs-upsert-question.md) - Comprehensive diagnostic guide
- [How to Restart Spring Boot During Debugging](../documentation/how-to-restart-spring-boot.md) - Step-by-step restart workflow
- [Multi-Module Maven Configuration](../documentation/how-cross-module-configuration-classes-work.md) - Understanding module dependencies

---

## Resolution Checklist

- [x] Identified root cause (stale compiled code)
- [x] Enhanced logging configuration for dev profile
- [x] Rebuilt all modules with `mvn clean install`
- [x] Restarted Spring Boot with dev profile
- [x] Verified service method logs appear in stdout
- [x] Confirmed `display_order` and `solution_explanation` persisted in MongoDB
- [x] Documented resolution and learnings
- [x] Created reusable troubleshooting workflow

---

## Conclusion

The issue was **not** a code problem, mediator wiring issue, or bean injection problem. It was a **build process oversight** where the orchestration layer was running against an older version of the internal layer that did not contain the new log statements.

**Resolution Time:** ~30 minutes  
**Key Action:** `mvn clean install -DskipTests` before restarting Spring Boot

This highlights the importance of understanding multi-module Maven projects and ensuring all dependencies are up-to-date when debugging runtime behavior.
