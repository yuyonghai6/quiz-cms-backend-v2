package com.quizfun.orchestrationlayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot application for the orchestration layer.
 * This layer serves as the HTTP API entry point and orchestrates CQRS commands
 * using the mediator pattern to coordinate business operations across internal modules.
 *
 * Cross-module component scanning enables dependency injection from:
 * - orchestrationlayer: HTTP controllers and command handlers
 * - internallayer: Application services and domain logic (includes transaction management)
 * - globalshared: Mediator pattern implementation and shared utilities
 * - questionbank: Question-specific business logic components
 */
@SpringBootApplication(scanBasePackages = {
    "com.quizfun.orchestrationlayer",
    "com.quizfun.internallayer",
    "com.quizfun.globalshared",
    "com.quizfun.questionbank",
    "com.quizfun.questionbankquery"  // Query module for read operations
},
exclude = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
}
)
public class OrchestrationLayerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrchestrationLayerApplication.class, args);
    }
}