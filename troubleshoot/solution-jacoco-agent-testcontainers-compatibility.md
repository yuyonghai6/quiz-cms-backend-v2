# JaCoCo Agent & Testcontainers Compatibility Issues

## Problem Summary

JaCoCo code coverage agent can cause test execution to hang indefinitely when combined with Testcontainers, particularly in Spring Boot projects with complex agent configurations (JaCoCo + AspectJ for Allure reporting).

## Symptoms

### Primary Issue:
- Tests hang indefinitely after Maven compiles and starts the test phase
- Last visible output: `[INFO] --- maven-surefire-plugin:3.2.5:test (default-test) @ question-bank ---`
- Hanging occurs even for simple unit tests that don't use Testcontainers
- Issue persists across different test classes and test types

### JVM Behavior:
- JVM process starts but never progresses beyond agent initialization
- No error messages or stack traces
- Memory usage remains low (not an OOM issue)
- Process must be killed manually (Ctrl+C timeout)

### Environment Context:
- Java 21 with Spring Boot 3.5.6
- JaCoCo 0.8.12 with default configuration
- Testcontainers 1.21.3 with MongoDB
- AspectJ weaver for Allure test reporting
- Maven Surefire 3.2.5

## Root Cause Analysis

### Agent Loading Order Conflicts:
The parent POM configures multiple Java agents with complex `argLine`:
```xml
<argLine>
    @{argLine} -javaagent:${user.home}/.m2/repository/org/aspectj/aspectjweaver/${aspectj.version}/aspectjweaver-${aspectj.version}.jar -XX:+EnableDynamicAgentLoading
</argLine>
```

Where `@{argLine}` is populated by JaCoCo's `prepare-agent` goal with:
```
-javaagent:/path/to/jacoco.agent.jar=destfile=/path/to/jacoco.exec
```

### The Problem:
1. **Agent Loading Timing**: JaCoCo agent initialization during JVM startup conflicts with Testcontainers' Docker client initialization
2. **Complex Agent Chain**: Multiple agents (JaCoCo + AspectJ) create initialization dependencies
3. **Resource Contention**: Agents compete for JVM instrumentation resources during class loading

## Solutions

### Solution 1: Skip JaCoCo During Testing (WORKING - Immediate Fix)

**Implementation:**
```bash
# Skip JaCoCo for individual test runs
mvn test -pl internal-layer/question-bank -Djacoco.skip=true

# Skip JaCoCo for specific test classes
mvn -Dtest=TestClassName test -pl internal-layer/question-bank -Djacoco.skip=true

# Skip JaCoCo for clean builds
mvn clean test -pl internal-layer/question-bank -Djacoco.skip=true
```

**Results:**
- ✅ Tests execute successfully in ~1:14 minutes for full suite
- ✅ All 253 tests run without hanging
- ✅ Testcontainers startup works properly
- ❌ No code coverage reporting

**When to Use:**
- Development and debugging phases
- CI/CD pipelines where coverage is optional
- Quick verification of test functionality
- Emergency situations when coverage is blocking development

### Solution 2: Separated Agent Configuration (ATTEMPTED - Partial Success)

**Implementation:**
Modified `pom.xml` to separate JaCoCo agent initialization:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <append>false</append>
        <excludes>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
            <configuration>
                <propertyName>surefireArgLine</propertyName>
            </configuration>
        </execution>
    </executions>
</plugin>

<!-- Updated Surefire configuration -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>${surefireArgLine} -XX:+EnableDynamicAgentLoading -XX:MaxMetaspaceSize=512m</argLine>
        <!-- other config -->
    </configuration>
</plugin>
```

**Results:**
- ❌ Still hangs during test execution
- ✅ Agent loading order is more controlled
- ⚠️ Configuration complexity increased

### Solution 3: JVM Tuning Parameters (ATTEMPTED - No Improvement)

**Implementation:**
```xml
<argLine>@{argLine} -XX:+EnableDynamicAgentLoading -XX:MaxMetaspaceSize=512m</argLine>
```

**Results:**
- ❌ No improvement in hanging behavior
- ✅ Better JVM memory management
- ❌ Agent conflicts persist

## Workaround Strategies

### Development Workflow:
```bash
# For regular development and testing
mvn test -pl internal-layer/question-bank -Djacoco.skip=true

# For coverage reports (run separately when needed)
mvn clean test jacoco:report -pl internal-layer/question-bank
# Note: This may still hang, use with caution
```

### CI/CD Pipeline Strategy:
```yaml
# Example pipeline approach
test_phase:
  script:
    # Run tests without coverage for speed and reliability
    - mvn test -pl internal-layer/question-bank -Djacoco.skip=true

coverage_phase:
  # Separate stage for coverage (if needed)
  # May require different container or configuration
  script:
    - mvn test jacoco:report -pl internal-layer/question-bank
  allow_failure: true  # Don't block pipeline if coverage fails
```

### IDE Integration:
- Configure IDE test runners to use `-Djacoco.skip=true` by default
- Create separate run configurations for coverage analysis
- Use IDE's built-in coverage tools instead of JaCoCo when possible

## Investigation Results

### What Was Tried:
1. ✅ **Agent separation** - Partial improvement, but core issue remains
2. ✅ **JVM tuning** - No significant impact on hanging
3. ✅ **Configuration simplification** - Reduced complexity but didn't resolve conflict
4. ✅ **Timeout adjustments** - No effect (issue is blocking, not timeout-related)
5. ❌ **Alternative coverage tools** - Not attempted (out of scope)
6. ❌ **JaCoCo version downgrade** - Not attempted

### What Didn't Work:
- Custom `argLine` configurations
- Memory limit adjustments
- Agent loading order modifications
- Timeout parameter tuning

### What Worked:
- Completely disabling JaCoCo during test execution
- Running tests and coverage in separate Maven executions

## Technical Details

### Agent Initialization Sequence:
1. **JVM Starts** → **JaCoCo Agent Loads** → **AspectJ Weaver Loads**
2. **Spring Boot Context Starts** → **Testcontainers Initializes** → **Docker Client Connects**
3. **Conflict Point**: Agent instrumentation conflicts with Testcontainers' Docker client initialization

### Log Evidence:
```
[INFO] --- jacoco-maven-plugin:0.8.12:prepare-agent (default) @ question-bank ---
[INFO] argLine set to -javaagent:/path/to/jacoco.agent.jar=destfile=/path/to/jacoco.exec
[INFO] --- maven-surefire-plugin:3.2.5:test (default-test) @ question-bank ---
[INFO] Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[HANGS HERE - NO FURTHER OUTPUT]
```

### Resource Monitor Observations:
- CPU usage: Low (1-2%)
- Memory usage: Normal (~200-300MB)
- No disk I/O activity
- No network activity
- JVM threads: Minimal activity, likely waiting on locks

## Future Solutions

### Option 1: Alternative Coverage Tools
- Consider **JCov** (OpenJDK's coverage tool)
- Evaluate **IntelliJ IDEA's coverage** for development
- Explore **Testcontainers-compatible coverage tools**

### Option 2: JaCoCo Version Management
- Test with **JaCoCo 0.8.10** or **0.8.11** (older versions)
- Monitor JaCoCo release notes for Testcontainers compatibility fixes
- Consider beta/snapshot versions if available

### Option 3: Build Process Separation
- **Separate test execution and coverage analysis** into distinct Maven phases
- Run tests first, then apply coverage instrumentation to successful builds
- Use **offline instrumentation** instead of agent-based coverage

### Option 4: Conditional Coverage
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
            <configuration>
                <skip>${skipCoverage}</skip>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Usage:
```bash
# Normal testing (no coverage)
mvn test -DskipCoverage=true

# Coverage testing (when working)
mvn test -DskipCoverage=false
```

## Best Practices

### For Development:
1. **Default to coverage-disabled testing** for speed and reliability
2. **Enable coverage only when specifically needed** (pre-commit, releases)
3. **Use IDE coverage tools** for quick feedback during development
4. **Set up coverage CI/CD separately** from main test pipeline

### For CI/CD:
1. **Separate test reliability from coverage requirements**
2. **Allow coverage failures** without blocking the build
3. **Run coverage on dedicated agents/containers** if possible
4. **Cache coverage data** to avoid repeated analysis

### For Troubleshooting:
1. **Always test with `-Djacoco.skip=true` first** when investigating test issues
2. **Isolate coverage problems** from functional test problems
3. **Monitor agent loading order** in verbose logs
4. **Check for updates** to both JaCoCo and Testcontainers regularly

## Quick Reference

### Commands That Work:
```bash
# Reliable test execution
mvn test -pl internal-layer/question-bank -Djacoco.skip=true
mvn -Dtest=SpecificTest test -pl internal-layer/question-bank -Djacoco.skip=true
mvn clean test -pl internal-layer/question-bank -Djacoco.skip=true
```

### Commands That Hang:
```bash
# These will hang indefinitely:
mvn test -pl internal-layer/question-bank
mvn -Dtest=SpecificTest test -pl internal-layer/question-bank
mvn clean test -pl internal-layer/question-bank
```

### Emergency Recovery:
```bash
# If tests are hanging:
# 1. Kill the Maven process (Ctrl+C, may need force kill)
# 2. Clean the project:
mvn clean -pl internal-layer/question-bank
# 3. Restart with JaCoCo disabled:
mvn test -pl internal-layer/question-bank -Djacoco.skip=true
```

---

**Status**: Issue identified and mitigated with workaround
**Severity**: High (blocks development without workaround)
**Impact**: Development workflow (coverage analysis requires separate process)
**Workaround Available**: Yes (disable JaCoCo during testing)
**Permanent Fix**: Under investigation

**Document Version**: 1.0
**Created**: September 25, 2025
**Last Updated**: September 25, 2025