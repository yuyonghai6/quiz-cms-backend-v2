# How to Create Unit Tests for Mediator Library and Get Test Coverage Report

This guide provides a comprehensive step-by-step approach to creating unit tests for the mediator library and generating test coverage reports in a Maven multi-module project.

## Prerequisites

- Maven multi-module project structure
- Spring Boot framework
- JUnit 5 and Mockito for testing
- JaCoCo for code coverage

## Step 1: Configure Test Dependencies

### 1.1 Add Test Dependencies to Parent POM

Ensure your parent `pom.xml` includes these test dependencies in `<dependencyManagement>`:

```xml
<dependencyManagement>
    <dependencies>
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 1.2 Add Dependencies to Submodule POM

In your submodule (e.g., `global-shared-library/pom.xml`):

```xml
<dependencies>
    <!-- Test Dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Step 2: Configure JaCoCo for Code Coverage

### 2.1 Add JaCoCo Plugin to Parent POM

Add to parent `pom.xml` in `<build><pluginManagement><plugins>`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <configuration>
        <excludes>
            <exclude>**/*Application.*</exclude>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2.2 Enable JaCoCo in Submodules

Add to each submodule's `pom.xml` in `<build><plugins>`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
</plugin>
```

## Step 3: Create Test Directory Structure

Create the proper test directory structure:

```bash
mkdir -p global-shared-library/src/test/java/com/quizfun/globalshared/mediator
```

## Step 4: Write Unit Tests for Core Components

### 4.1 Test the Result Record Class

Create `ResultTest.java`:

```java
package com.quizfun.globalshared.mediator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Result Record Tests")
class ResultTest {

    @Test
    @DisplayName("Happy Path: Should create success result with data")
    void shouldCreateSuccessResultWithData() {
        // Given
        String testData = "test data";

        // When
        Result<String> result = Result.success(testData);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Operation completed successfully");
        assertThat(result.data()).isEqualTo(testData);
    }

    @Test
    @DisplayName("Unhappy Path: Should create failure result with message")
    void shouldCreateFailureResultWithMessage() {
        // Given
        String errorMessage = "Something went wrong";

        // When
        Result<String> result = Result.failure(errorMessage);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo(errorMessage);
        assertThat(result.data()).isNull();
    }

    // Add more test methods for different scenarios...
}
```

### 4.2 Test the MediatorImpl Class

Create `MediatorImplTest.java`:

```java
package com.quizfun.globalshared.mediator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediatorImpl Tests")
class MediatorImplTest {

    @Mock
    private ApplicationContext applicationContext;

    private MediatorImpl mediator;

    @BeforeEach
    void setUp() {
        mediator = new MediatorImpl(applicationContext);
    }

    @Test
    @DisplayName("Happy Path: Should route command to correct handler")
    void shouldRouteCommandToCorrectHandler() {
        // Given
        TestCommand command = new TestCommand("test");
        TestCommandHandler handler = new TestCommandHandler();

        when(applicationContext.getBeansOfType(ICommandHandler.class))
            .thenReturn(Map.of("testHandler", handler));

        // When
        Result<String> result = mediator.send(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("Processed: test");
    }

    @Test
    @DisplayName("Unhappy Path: Should throw exception when no handler found")
    void shouldThrowExceptionWhenNoHandlerFound() {
        // Given
        TestCommand command = new TestCommand("test");
        when(applicationContext.getBeansOfType(ICommandHandler.class))
            .thenReturn(Map.of());

        // When & Then
        assertThatThrownBy(() -> mediator.send(command))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No handler found for command");
    }

    // Test classes for demonstration
    record TestCommand(String data) implements ICommand<String> {}

    static class TestCommandHandler implements ICommandHandler<TestCommand, String> {
        @Override
        public Result<String> handle(TestCommand command) {
            return Result.success("Processed: " + command.data());
        }
    }
}
```

## Step 5: Test Command Handlers

### 5.1 Create Command Handler Tests

Create tests for your command handlers with mocked dependencies:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateUserCommandHandler Tests")
class CreateUserCommandHandlerTest {

    @Mock
    private UserValidationService userValidationService;

    private CreateUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateUserCommandHandler(userValidationService);
    }

    @Test
    @DisplayName("Happy Path: Should create user successfully with valid data")
    void shouldCreateUserSuccessfullyWithValidData() {
        // Given
        CreateUserCommand command = new CreateUserCommand("john", "john@example.com");
        when(userValidationService.validateUserData("john", "john@example.com")).thenReturn(true);

        // When
        Result<CreateUserResult> result = handler.handle(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("User created successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().username()).isEqualTo("john");
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Unhappy Path: Should fail with invalid data")
    void shouldFailWithInvalidData() {
        // Given
        CreateUserCommand command = new CreateUserCommand("", "john@example.com");
        when(userValidationService.validateUserData("", "john@example.com")).thenReturn(false);

        // When
        Result<CreateUserResult> result = handler.handle(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("Invalid user data");
        assertThat(result.data()).isNull();
    }
}
```

## Step 6: Run Tests and Generate Coverage Reports

### 6.1 Run Tests for All Modules

```bash
# From project root
mvn clean test

# For specific module
mvn clean test -pl global-shared-library
```

### 6.2 Run Tests with Coverage

```bash
# Generate coverage report for specific module (recommended)
mvn clean test jacoco:report -pl global-shared-library

# Run with coverage check for specific module (recommended)
mvn clean test jacoco:check -pl global-shared-library

# Generate coverage report for all modules (may fail if other modules don't meet thresholds)
mvn clean test jacoco:report

# Run with coverage check for all modules (will fail if any module below 70% coverage)
mvn clean test jacoco:check
```

**Note**: Use the `-pl global-shared-library` flag to run coverage specifically for the global-shared-library module. Running on all modules may fail if other modules (like orchestration-layer) don't meet the 70% coverage threshold.

### 6.3 View Coverage Reports

Coverage reports are generated in each module's target directory:

- **Global Shared Library**: `global-shared-library/target/site/jacoco/index.html`
- **Orchestration Layer**: `orchestration-layer/target/site/jacoco/index.html`

## Step 7: Best Practices for Testing

### 7.1 Test Naming Conventions

- Use `@DisplayName` for descriptive test names
- Follow pattern: "Happy Path: Should..." or "Unhappy Path: Should..."
- Include context about what scenario you're testing

### 7.2 Test Structure (Given-When-Then)

```java
@Test
void shouldDoSomething() {
    // Given - Set up test data and mocks
    TestCommand command = new TestCommand("test");
    when(mockService.validate()).thenReturn(true);

    // When - Execute the code under test
    Result<String> result = handler.handle(command);

    // Then - Verify the outcomes
    assertThat(result.success()).isTrue();
}
```

### 7.3 Coverage Goals

- **Line Coverage**: Aim for 80%+ (we achieved 97%)
- **Branch Coverage**: Aim for 70%+ (we achieved 62%)
- **Method Coverage**: Aim for 90%+ (we achieved 100%)

### 7.4 Test Categories

1. **Happy Path Tests**: Normal successful scenarios
2. **Unhappy Path Tests**: Error conditions and edge cases
3. **Boundary Tests**: Edge cases with null/empty values
4. **Integration Tests**: Cross-component interactions

## Step 8: Troubleshooting Common Issues

### 8.1 Maven Build Issues

```bash
# Clean and rebuild if dependencies are stale
mvn clean install

# Skip tests if needed during development
mvn clean install -DskipTests
```

### 8.2 Coverage Report Not Generated

Check that:
- JaCoCo plugin is properly configured in both parent and submodule POMs
- Tests are actually running (check surefire reports)
- No syntax errors in test classes

### 8.3 Low Coverage Numbers

- Add tests for missed branches (if/else conditions)
- Test exception scenarios
- Consider excluding configuration classes from coverage

## Step 9: Continuous Integration

### 9.1 Maven Command for CI

```bash
# Full build with coverage
mvn clean verify jacoco:report

# With coverage check that fails build if below threshold
mvn clean verify jacoco:check
```

### 9.2 Coverage Thresholds

Adjust coverage thresholds in JaCoCo configuration based on your project needs:

```xml
<configuration>
    <rules>
        <rule>
            <limits>
                <limit>
                    <counter>LINE</counter>
                    <value>COVEREDRATIO</value>
                    <minimum>0.80</minimum> <!-- 80% line coverage -->
                </limit>
            </limits>
        </rule>
    </rules>
</configuration>
```

## Conclusion

Following this guide, you should achieve:
- Comprehensive unit test coverage for your mediator library
- Per-submodule coverage reports
- Automated coverage validation in your build pipeline
- High-quality, maintainable test code

The mediator library testing approach ensures that your auto-registration mechanism, command routing, and error handling all work correctly while maintaining high code quality standards.