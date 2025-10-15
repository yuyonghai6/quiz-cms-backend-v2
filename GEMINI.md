# Gemini Project Context: Quiz CMS Backend

This document provides a comprehensive overview of the Quiz CMS backend project, its architecture, technologies, and development conventions.

## 1. Project Overview

This is a multi-module Spring Boot application that serves as the backend for a Quiz Content Management System (CMS). It is built with Java 21 and uses Maven for dependency management. The backend database is MongoDB.

The project follows a clean, modular architecture that separates concerns into distinct layers, making it scalable and maintainable.

## 2. Architecture

The application is built on a set of modern architectural patterns designed for robustness and clarity.

### 2.1. Multi-Module Maven Structure

The project is divided into several Maven modules, each with a specific responsibility:

*   `orchestration-layer`: The main application module. It contains the Spring Boot application entry point, REST controllers, and orchestrates calls to the internal layers. It depends on all other modules.
*   `internal-layer`: Contains the core business logic. This layer is further subdivided into domain-specific modules like `question-bank` and `question-bank-query`.
*   `global-shared-library`: A crucial module providing common, cross-cutting concerns and implementing core architectural patterns.
*   `external-service-proxy`: A module intended for handling communication with external services.

### 2.2. Core Design Patterns (from `global-shared-library`)

The `global-shared-library` establishes several key conventions:

*   **CQRS with Mediator Pattern**: The application separates Commands (write operations) from Queries (read operations). A central `IMediator` interface is used to dispatch commands and queries to their respective handlers. This decouples controllers from the business logic.
    *   **Commands**: Implement `ICommand<T>` and are handled by a corresponding `ICommandHandler<C, T>`.
    *   **Queries**: Implement `IQuery<T>` and are handled by a corresponding `IQueryHandler<Q, T>`.
*   **Functional `Result<T>` Pattern**: Instead of throwing exceptions for business errors, handlers return a `Result<T>` record. This wrapper contains the operation's outcome (success/failure), a message, and the data payload, allowing for clean, predictable error handling in the calling code.
*   **Hybrid ID Generation Strategy**: The system uses two types of IDs for different purposes:
    *   `LongIdGenerator`: Generates high-performance, sequential, numeric `Long` IDs for internal database entities (e.g., `question_bank_id`).
    *   `UUIDv7Generator`: Generates time-ordered `UUID v7` strings for external identifiers (`source_question_id`) or domain events, ensuring global uniqueness.

## 3. Key Technologies

*   **Backend**: Java 21, Spring Boot 3.5.6
*   **Database**: MongoDB
*   **Build**: Apache Maven
*   **Testing**:
    *   **Unit/Integration**: JUnit 5, Mockito
    *   **API System/Perf**: K6 (JavaScript-based)
    *   **DB Testing**: Testcontainers
*   **Reporting**: Allure (for JUnit tests), k6-reporter (for K6 tests)
*   **Code Quality**: JaCoCo (for test coverage, 70% minimum)
*   **Utilities**: Lombok, SpringDoc (OpenAPI)

## 4. Building and Running

### 4.1. Prerequisites

*   Java 21 SDK
*   Apache Maven
*   Docker (for Testcontainers)
*   K6 (for API system tests)

### 4.2. Running the Application

The main application is in the `orchestration-layer`.

1.  **Navigate to the orchestration module**:
    ```bash
    cd orchestration-layer
    ```
2.  **Run the Spring Boot application**:
    ```bash
    mvn spring-boot:run
    ```
The application will start on port `8765` as specified in the `README.md`.

## 5. Testing

The project has a multi-layered testing strategy.

### 5.1. Backend Unit & Integration Tests (Maven)

These tests use JUnit 5 and are run with Maven. The `Makefile` provides convenient commands.

*   **Run all backend tests**:
    ```bash
    make test
    # or
    mvn clean test
    ```
*   **Run tests and generate/view the Allure report**:
    ```bash
    make allure
    ```
    This command runs the tests, generates the Allure report, and opens it in your browser. Test results from all relevant modules (`question-bank`, `shared`, `global-shared-library`) are aggregated.

### 5.2. API System Tests (K6)

These tests are written in JavaScript and are located in the `api-system-test/` directory. They test the running application from an external perspective.

1.  **Ensure the application is running** (see section 4.2).
2.  **Run a specific K6 test script**:
    ```bash
    k6 run api-system-test/test-upsert-question-with-taxonomy.js
    ```
3.  **View Reports**: K6 tests generate an HTML report (e.g., `summary-report.html`) in the project root for easy visualization.
