# Quick Reference: Spring Boot Dev Restart Workflow

**Purpose:** Ensure latest code changes are running when debugging multi-module Maven projects.

---

## ⚡ Quick Commands

### Full Restart (After Code Changes)
```bash
# 1. Kill running instance
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# 2. Full rebuild (includes internal-layer dependencies)
mvn clean install -DskipTests

# 3. Start with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### Restart Without Rebuild (Config Changes Only)
```bash
# 1. Kill running instance
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9

# 2. Start with dev profile
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```

### Background Mode with Log File
```bash
# Kill + Rebuild + Start in background
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9 && \
mvn clean install -DskipTests && \
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev > spring-boot.log 2>&1 &

# Monitor logs in real-time
tail -f spring-boot.log | grep -E "(WARN|ERROR|INFO)"
```

---

## 📋 Decision Matrix

| Scenario | Command | Why |
|----------|---------|-----|
| Changed code in `internal-layer/question-bank` | `mvn clean install -DskipTests` | Rebuilds JARs and installs to local repo |
| Changed only `orchestration-layer` code | `mvn clean compile` | Faster; only compiles orchestration-layer |
| Changed `application.properties` | Just restart Spring Boot | No compilation needed |
| Changed `pom.xml` dependencies | `mvn clean install -DskipTests` | Updates dependency resolution |
| Added new logger statements | `mvn clean install -DskipTests` | Ensures .class files updated |
| Unsure what changed | `mvn clean install -DskipTests` | Safe default; rebuilds everything |

---

## 🔍 Verification Steps

### 1. Confirm Process is Running
```bash
ps aux | grep "[O]rchestrationLayerApplication"
```
**Expected:** Should show one Java process with PID

### 2. Verify Compilation Timestamp
```bash
# Check when service class was last compiled
ls -lh internal-layer/question-bank/target/classes/com/quizfun/questionbank/application/services/QuestionApplicationService.class

# Compare with current time
date
```
**Expected:** Timestamp should be recent (within last few minutes)

### 3. Check Application Started
```bash
# Check if listening on port 8765
lsof -i :8765
```
**Expected:** Should show Java process listening on 8765

### 4. Verify Profile Active
```bash
# Search logs for active profile
grep "The following .* profile is active" spring-boot.log
```
**Expected:** Should show `"dev"` profile

### 5. Test Health Endpoint
```bash
curl -s http://localhost:8765/actuator/health | jq
```
**Expected:**
```json
{
  "status": "UP"
}
```

---

## ⚠️ Common Mistakes

### ❌ Mistake 1: Using `mvn spring-boot:run` After Internal Layer Changes
```bash
# This does NOT rebuild internal-layer dependencies
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```
**Problem:** orchestration-layer uses stale JARs from local Maven repo.

✅ **Fix:** Use `mvn clean install -DskipTests` first.

---

### ❌ Mistake 2: Forgetting to Kill Old Process
```bash
# Starting new instance without killing old one
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
```
**Problem:** Port 8765 already in use; new instance fails to start.

✅ **Fix:** Always kill first with `ps aux | grep` command.

---

### ❌ Mistake 3: Using `mvn compile` Instead of `mvn install`
```bash
# This compiles but does NOT update local Maven repo
mvn clean compile
```
**Problem:** orchestration-layer still references old JARs in `~/.m2/repository/`.

✅ **Fix:** Use `mvn clean install -DskipTests` to install JARs to local repo.

---

### ❌ Mistake 4: Not Waiting for Startup
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev &
k6 run api-system-test/test-upsert-question-with-taxonomy.js  # Runs immediately
```
**Problem:** Tests run before Spring Boot finishes initialization.

✅ **Fix:** Add `sleep 20 && k6 run ...` or check health endpoint first.

---

## 🎯 Best Practices

### 1. Always Use Full Rebuild for Critical Debugging
When troubleshooting missing logs or unexpected behavior:
```bash
mvn clean install -DskipTests
```

### 2. Redirect Logs to File for Analysis
```bash
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev > spring-boot.log 2>&1 &
```
Benefits:
- Logs persist after restart
- Can grep/search easily
- Terminal remains free for other commands

### 3. Use Explicit Profile
Always specify `-Dspring-boot.run.profiles=dev` to ensure:
- Security validator is bypassed for testing
- DEBUG logging is enabled
- Dev MongoDB connection string is used

### 4. Create Convenience Script
Save as `restart-dev.sh`:
```bash
#!/bin/bash
set -e

echo "🛑 Killing existing Spring Boot instance..."
ps aux | grep "[O]rchestrationLayerApplication" | awk '{print $2}' | xargs -r kill -9 || true

echo "🔨 Rebuilding all modules..."
mvn clean install -DskipTests -q

echo "🚀 Starting Spring Boot with dev profile..."
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev > spring-boot.log 2>&1 &

echo "⏳ Waiting for startup..."
sleep 15

echo "✅ Health check..."
curl -s http://localhost:8765/actuator/health | jq

echo "📝 Tailing logs (Ctrl+C to stop)..."
tail -f spring-boot.log
```

Make executable: `chmod +x restart-dev.sh`

---

## 🐛 Troubleshooting

### Problem: Port 8765 Already in Use
```bash
# Find process using the port
lsof -i :8765

# Kill by port
lsof -ti :8765 | xargs -r kill -9
```

### Problem: MongoDB Connection Refused
```bash
# Check if MongoDB is running
docker ps | grep mongo

# Or check local MongoDB
sudo systemctl status mongod
```

### Problem: Logs Not Appearing
1. Check logging configuration in `application-dev.properties`:
   ```properties
   logging.level.com.quizfun.questionbank=DEBUG
   ```
2. Verify dev profile is active (check startup logs)
3. Confirm `.class` file is recent: `ls -lh internal-layer/question-bank/target/classes/.../QuestionApplicationService.class`

### Problem: Tests Pass but Logs Missing
- You're probably using stale compiled code
- Solution: `mvn clean install -DskipTests` and restart

---

## 📚 Related Documentation

- [Complete Fix Summary](./complete-fix-summary-missing-fields-mongodb.md)
- [Resolution Guide](./resolution-missing-service-logs-upsert-question.md)
- [Troubleshooting Plan](./troubleshoot-missing-service-logs-upsert-question.md)

---

## 💡 Remember

> **"When in doubt, `mvn clean install -DskipTests`"**

Multi-module Maven projects require careful attention to dependency updates. When debugging runtime behavior, always ensure you're running the latest compiled code by doing a full rebuild and install.
