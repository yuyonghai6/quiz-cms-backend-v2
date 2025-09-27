# 🚀 Cross-Module Service Integration Guide

## 🎯 What You'll Build

By following this guide, you'll create:
- ✅ A validation service in the `internal-layer` module
- ✅ An orchestration service that uses the internal service
- ✅ A REST API that returns "validate success" messages
- ✅ Complete cross-module dependency injection

## 🛠️ Prerequisites

- ✅ Maven multi-module project with `internal-layer` and `orchestration-layer` modules
- ✅ Spring Boot 3.x
- ✅ Java 21

## 🧩 Architecture Overview

```
📦 internal-layer
└── 🔧 UserValidationService (validates emails)

📦 orchestration-layer
├── 🔧 UserService (uses internal service)
├── 🌐 UserController (REST API)
└── 🔒 SecurityConfig (for testing)
```

**Request Flow:** Client → Controller → Orchestration Service → Internal Service → Response

---

## 🔧 Step 1: Connect the Modules

### 1️⃣ Enable Spring in Internal Layer

**File:** `internal-layer/pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

### 2️⃣ Connect Orchestration to Internal

**File:** `orchestration-layer/pom.xml`

```xml
<dependencies>
    <!-- This connects the modules -->
    <dependency>
        <groupId>com.quizfun</groupId>
        <artifactId>internal-layer</artifactId>
        <version>${project.version}</version>
    </dependency>

    <!-- Your existing dependencies... -->
</dependencies>
```

### 3️⃣ Fix Parameter Names (Critical!)

**File:** `orchestration-layer/pom.xml`

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
        <!-- Your other settings... -->
    </configuration>
</plugin>
```

> **💡 Important:** Without this, `@RequestParam` won't work!

---

## 📝 Step 2: Create the Internal Service

### 1️⃣ Create Package Structure

```bash
mkdir -p internal-layer/src/main/java/com/quizfun/internallayer/service
```

### 2️⃣ Create UserValidationService

**File:** `internal-layer/src/main/java/com/quizfun/internallayer/service/UserValidationService.java`

```java
package com.quizfun.internallayer.service;

import org.springframework.stereotype.Service;

@Service
public class UserValidationService {

    public boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    public boolean validateUserData(String username, String email) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return validateEmail(email);
    }
}
```

---

## 🎭 Step 3: Create the Orchestration Service

### 1️⃣ Create Package Structure

```bash
mkdir -p orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/service
```

### 2️⃣ Create UserService

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/service/UserService.java`

```java
package com.quizfun.orchestrationlayer.service;

import com.quizfun.internallayer.service.UserValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserValidationService userValidationService;

    @Autowired
    public UserService(UserValidationService userValidationService) {
        this.userValidationService = userValidationService;
    }

    public ValidationResult validateUser(String username, String email) {
        boolean isValid = userValidationService.validateUserData(username, email);

        if (isValid) {
            return new ValidationResult(true, "validate success", "User data is valid");
        } else {
            return new ValidationResult(false, "validation failed", "Invalid username or email format");
        }
    }

    // Simple result class
    public static class ValidationResult {
        private boolean valid;
        private String status;
        private String message;

        public ValidationResult(boolean valid, String status, String message) {
            this.valid = valid;
            this.status = status;
            this.message = message;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }

        public void setValid(boolean valid) { this.valid = valid; }
        public void setStatus(String status) { this.status = status; }
        public void setMessage(String message) { this.message = message; }
    }
}
```

> **🔗 Magic happens here:** The `@Autowired` constructor automatically injects the internal service!

---

## ⚙️ Step 4: Enable Cross-Module Scanning

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/OrchestrationLayerApplication.java`

```java
package com.quizfun.orchestrationlayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer"
})
public class OrchestrationLayerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrchestrationLayerApplication.class, args);
    }
}
```

> **🔍 Key Point:** `scanBasePackages` tells Spring to look for services in both modules!

---

## 🌐 Step 5: Create the REST API

### 1️⃣ Create Controller Package

```bash
mkdir -p orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controller
```

### 2️⃣ Create UserController

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/controller/UserController.java`

```java
package com.quizfun.orchestrationlayer.controller;

import com.quizfun.orchestrationlayer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/validate")
    public ResponseEntity<UserService.ValidationResult> validateUser(
            @RequestParam String username,
            @RequestParam String email) {

        UserService.ValidationResult result = userService.validateUser(username, email);
        return ResponseEntity.ok(result);
    }
}
```

---

## 🔒 Step 6: Disable Security (For Testing)

**File:** `orchestration-layer/src/main/java/com/quizfun/orchestrationlayer/config/SecurityConfig.java`

```java
package com.quizfun.orchestrationlayer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
```

---

## 🚀 Step 7: Build and Test

### 1️⃣ Build Everything

```bash
mvn clean install
```

### 2️⃣ Start the Application

```bash
mvn spring-boot:run -pl orchestration-layer
```

### 3️⃣ Test the API

**Valid request:**
```bash
curl "http://localhost:8080/api/users/validate?username=john&email=john@example.com"
```

**Expected response:**
```json
{
    "valid": true,
    "status": "validate success",
    "message": "User data is valid"
}
```

**Invalid request:**
```bash
curl "http://localhost:8080/api/users/validate?username=john&email=bad-email"
```

**Expected response:**
```json
{
    "valid": false,
    "status": "validation failed",
    "message": "Invalid username or email format"
}
```

---

## 🎯 What Just Happened?

1. **Request comes in** → `UserController` receives it
2. **Controller calls service** → `UserService.validateUser()`
3. **Service uses internal logic** → `UserValidationService.validateUserData()`
4. **Internal service validates** → Checks email format with regex
5. **Result bubbles back up** → Returns "validate success" or error

## 🛠️ Key Concepts

### Maven Dependencies
- Internal layer needs Spring Boot starter
- Orchestration layer needs internal layer as dependency
- Use `${project.version}` for version consistency

### Spring Component Scanning
- `@SpringBootApplication(scanBasePackages)` is crucial
- Without it, Spring can't find services in other modules
- Alternative: Use `@ComponentScan` annotation

### Cross-Module Injection
- `@Autowired` works across modules seamlessly
- Constructor injection is preferred
- Spring's IoC container handles the complexity

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| "Could not resolve dependencies" | Check Maven dependency versions match |
| "No qualifying bean" | Verify component scanning includes all packages |
| "Parameter name not available" | Add `-parameters` to compiler configuration |
| Port conflicts | Change `server.port` in `application.properties` |

## ✅ Success!

You now have a working cross-module Spring Boot application! The orchestration layer successfully uses services from the internal layer, demonstrating proper separation of concerns and dependency injection across Maven modules.