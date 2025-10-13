# Troubleshooting Plan: Missing Service Logs in QuestionApplicationService.upsertQuestion

**Date:** October 13, 2025  
**Issue:** The `upsertQuestion` method in `QuestionApplicationService.java` contains logger statements (e.g., `logger.warn("upsertQuestion method start in QuestionApplicationService.java")`), but these logs do not appear in Spring Boot stdout when the API endpoint is called.

**Context:**
- The orchestration layer handler `UpsertQuestionCommandHandler.handle()` correctly calls `questionApplicationService.upsertQuestion(command)`.
- Component scanning includes `com.quizfun.questionbank` in `OrchestrationLayerApplication.java`.
- Unit and integration tests for the service pass successfully (370 tests, 0 failures).
- MongoDB via Docker Compose is running.

---

## Possible Root Causes

1. **Stale Spring Boot Process**: An old instance is running with outdated code.
2. **Logging Configuration**: Log level is set too high (ERROR only) or the logger for the package is disabled.
3. **Wrong Profile/Configuration**: Running with a profile that has different logging settings.
4. **Bean Wiring Issue**: The service bean injected into the handler is not the expected instance (e.g., proxy issue, multiple bean definitions).
5. **Mediator Routing Problem**: The mediator is not correctly routing `UpsertQuestionCommand` to the handler.
6. **Code Not Compiled**: The latest code changes were not compiled/packaged into the running application.
7. **Logger Instance Issue**: The logger in the service class is misconfigured or not initialized.

---

## Diagnostic Steps

### Step 1: Verify Current Process State
**Objective:** Check if a Spring Boot instance is already running and what version/build it's using.

```bash
# Check for running OrchestrationLayerApplication process
ps aux | grep "[O]rchestrationLayerApplication"

# Check what port 8765 (or your app port) is using
lsof -i :8765

# If found, note the PID and kill it
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9
```

**Expected Outcome:** No running process or successfully killed.

---

### Step 2: Rebuild Project Dependencies
**Objective:** Ensure the latest code changes are compiled and packaged.

```bash
# Clean and compile all modules
mvn clean compile

# Optional: Verify the compiled class contains the log statement
# Look for the string in the compiled .class file (bytecode verification)
javap -c internal-layer/question-bank/target/classes/com/quizfun/questionbank/application/services/QuestionApplicationService.class | grep -A 5 "upsertQuestion"
```

**Expected Outcome:** Build SUCCESS with no compilation errors.

---

### Step 3: Verify Logging Configuration
**Objective:** Check that logging levels allow WARN messages from the service package.

**Files to check:**
- `orchestration-layer/src/main/resources/application.yml`
- `orchestration-layer/src/main/resources/application-dev.yml`
- `orchestration-layer/src/main/resources/logback-spring.xml` (if present)

**What to look for:**
```yaml
logging:
  level:
    root: INFO  # or DEBUG
    com.quizfun.questionbank: WARN  # Should be WARN, INFO, or DEBUG
    com.quizfun.orchestrationlayer: DEBUG
```

**Action if misconfigured:**
- Ensure `com.quizfun.questionbank` is set to at least `WARN` level.
- Ensure no logger for this package is explicitly set to `ERROR` or `OFF`.

---

### Step 4: Start Spring Boot with Dev Profile
**Objective:** Start the application with explicit dev profile and observe startup logs.

```bash
# Start with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

**What to watch for in startup logs:**
1. **Bean Creation:**
   ```
   Creating bean 'questionApplicationService'
   ```
2. **Component Scanning:**
   ```
   Scanning for components in package 'com.quizfun.questionbank'
   ```
3. **Handler Registration:**
   ```
   Registering command handler: UpsertQuestionCommandHandler
   ```
4. **Profile Activation:**
   ```
   The following profiles are active: dev
   ```
5. **Logger Initialization:**
   - Check for any warnings about logger configuration.

**Expected Outcome:** Application starts successfully on port 8765 (or configured port) with dev profile active.

---

### Step 5: Verify Service Bean Injection
**Objective:** Confirm the handler has the correct service instance.

**Add temporary debug logging to handler:**
In `UpsertQuestionCommandHandler.java`, add:
```java
@Override
public QuestionDto handle(UpsertQuestionCommand command) {
    logger.info("=== Handler invoked ===");
    logger.info("Service instance class: {}", questionApplicationService.getClass().getName());
    logger.info("Service instance: {}", questionApplicationService);
    
    // Existing code...
    var applicationServiceResult = questionApplicationService.upsertQuestion(command);
    // ...
}
```

**Expected Output:**
- Handler logs should appear showing the service class (might be a Spring proxy like `QuestionApplicationService$$SpringCGLIB$$0`).

---

### Step 6: Test API Endpoint
**Objective:** Trigger the upsert endpoint and observe logs in real-time.

**Prepare a test request:**
```bash
# Example curl command (adjust values as needed)
curl -X POST http://localhost:8765/api/users/test-user-123/questionbanks/qb-001/questions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer dummy-token" \
  -d '{
    "questionText": "What is 2+2?",
    "questionType": "MCQ",
    "sourceQuestionId": "source-q-001",
    "status": "DRAFT",
    "displayOrder": 1,
    "solutionExplanation": "The answer is 4",
    "difficultyLevel": "EASY",
    "options": [
      {"optionText": "3", "isCorrect": false},
      {"optionText": "4", "isCorrect": true}
    ]
  }'
```

**Monitor logs in the terminal running Spring Boot:**
- Look for: `"upsertQuestion method start in QuestionApplicationService.java"`
- Look for: Handler logs showing command processing
- Look for: Any exceptions or warnings

**Expected Outcome:**
- Service start log appears in stdout.
- Subsequent logs ("before step 2", "before step 3", etc.) appear.
- Request completes successfully or with expected validation errors.

---

### Step 7: Validate Mediator Routing (If Logs Still Missing)
**Objective:** Confirm the mediator correctly routes commands to handlers.

**Check mediator configuration:**
- Look for mediator bean definition in `global-shared-library` or orchestration config.
- Verify `UpsertQuestionCommand` is registered with `UpsertQuestionCommandHandler`.

**Add mediator-level debug logging:**
- Enable debug logs for mediator package (if available):
  ```yaml
  logging:
    level:
      com.quizfun.mediator: DEBUG  # Or whatever package the mediator uses
  ```

**Expected Outcome:**
- Mediator logs show command dispatch to the correct handler.

---

### Step 8: Check for Multiple Service Beans
**Objective:** Rule out multiple bean definitions causing autowiring confusion.

**Search for duplicate bean definitions:**
```bash
# Search for @Service or @Component on QuestionApplicationService
grep -r "@Service.*QuestionApplicationService" internal-layer/question-bank/src/

# Check Spring Boot startup logs for duplicate bean warnings
# Look for: "Overriding bean definition for bean 'questionApplicationService'"
```

**Expected Outcome:**
- Only one `@Service` annotation on `QuestionApplicationService`.
- No bean override warnings in startup logs.

---

### Step 9: Verify Logger Initialization
**Objective:** Ensure the logger is properly initialized in the service class.

**Check logger declaration in `QuestionApplicationService.java`:**
```java
private static final Logger logger = LoggerFactory.getLogger(QuestionApplicationService.class);
```

**Verify import:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

**Expected Outcome:**
- Logger is correctly declared as `private static final`.
- SLF4J imports are present (not `java.util.logging.Logger`).

---

## Quick Restart Workflow for Debugging

```bash
# 1. Kill any running Spring Boot instance
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# 2. Rebuild with latest changes
mvn clean compile

# 3. Start with dev profile and pipe to log file for analysis
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev | tee spring-boot-output.log
```

---

## Findings Log

### Run 1: [Date/Time]
- **Action Taken:** [e.g., Restarted with dev profile]
- **Observations:** [e.g., Handler logs appear, service logs do not]
- **Hypothesis:** [e.g., Logging level issue]

### Run 2: [Date/Time]
- **Action Taken:** [e.g., Updated logging config to DEBUG for com.quizfun.questionbank]
- **Observations:** [e.g., Service logs now appear]
- **Resolution:** [e.g., Logging level was set to ERROR in application-dev.yml]

---

## Resolution Checklist

- [ ] Confirmed latest code is compiled and deployed
- [ ] Verified logging configuration allows WARN level for service package
- [ ] Confirmed Spring Boot is running with correct profile
- [ ] Verified handler successfully calls service method
- [ ] Observed service method logs in stdout
- [ ] Tested API endpoint end-to-end successfully

---

## Additional Notes

- **Spring Boot Version:** 3.5.x
- **Java Version:** 21
- **Logging Framework:** SLF4J with Logback
- **Orchestration Port:** 8765 (default)
- **MongoDB:** Running via Docker Compose

---

## Related Documentation

- [How to Restart Spring Boot During Debugging](../how-to-restart-spring-boot-during-debugging.md)
- [Mediator Usage Guide](../documentation/how to use mediator library.md)
- [Orchestration Layer Configuration](../documentation/how-cross-module-configuration-classes-work.md)
