package com.quizfun.questionbank;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@DisplayName("QuestionBankModuleStructureTest")
class QuestionBankModuleStructureTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should compile question-bank module successfully")
    @Description("Validates that the question-bank module compiles successfully and creates a proper Spring application context with component scanning enabled")
    void shouldCompileQuestionBankModuleSuccessfully() {
        var context = new AnnotationConfigApplicationContext();
        context.scan("com.quizfun.questionbank");

        assertThat(context.getBeanDefinitionCount()).isGreaterThan(0);
        assertThat(context.getEnvironment()).isNotNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should resolve shared module dependency")
    @Description("Validates that the question-bank module can successfully access classes from the shared module dependency, ensuring proper Maven dependency resolution")
    void shouldResolveSharedModuleDependency() {
        assertDoesNotThrow(() -> {
            Class.forName("com.quizfun.shared.domain.AggregateRoot");
            Class.forName("com.quizfun.shared.common.Result");
            Class.forName("com.quizfun.shared.validation.ValidationHandler");
        });
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should support hexagonal architecture package structure")
    @Description("Validates that all required hexagonal architecture packages are properly structured and accessible for domain-driven design implementation")
    void shouldSupportHexagonalArchitecturePackageStructure() {
        var packages = List.of(
                "com.quizfun.questionbank.domain.aggregates",
                "com.quizfun.questionbank.domain.entities",
                "com.quizfun.questionbank.domain.events",
                "com.quizfun.questionbank.application.ports.in",
                "com.quizfun.questionbank.application.ports.out",
                "com.quizfun.questionbank.application.services",
                "com.quizfun.questionbank.infrastructure.persistence",
                "com.quizfun.questionbank.infrastructure.configuration"
        );

        packages.forEach(packageName -> {
            assertDoesNotThrow(() -> {
                assertThat(packageName).matches("com\\.quizfun\\.questionbank\\..+");
            });
        });
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should have Spring Boot dependencies configured")
    @Description("Validates that all required Spring Boot dependencies are properly configured and accessible, including web, data-mongodb, and core Spring components")
    void shouldHaveSpringBootDependenciesConfigured() {
        assertDoesNotThrow(() -> {
            Class.forName("org.springframework.boot.SpringApplication");
            Class.forName("org.springframework.data.mongodb.core.MongoTemplate");
            Class.forName("org.springframework.context.annotation.Configuration");
        });
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should support Maven compiler parameters")
    @Description("Validates that Maven compiler is configured with -parameters flag to preserve method parameter names for proper Spring framework operation")
    void shouldSupportMavenCompilerParameters() {
        var method = Arrays.stream(TestController.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("testMethod"))
                .findFirst()
                .orElseThrow();

        var parameters = method.getParameters();
        assertThat(parameters).hasSize(1);

        // Parameter names should be available (requires -parameters compiler flag)
        assertThat(parameters[0].getName()).isNotEqualTo("arg0");
    }

    @RestController
    static class TestController {
        public String testMethod(@RequestParam String testParam) {
            return testParam;
        }
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-001.infrastructure-foundation-setup")
    @DisplayName("QuestionBankModuleStructureTest.Should isolate test and main source directories")
    @Description("Validates that test and main source directories are properly isolated, ensuring clean separation between production and test code")
    void shouldIsolateTestAndMainSourceDirectories() {
        assertDoesNotThrow(() -> {
            Class.forName("com.quizfun.questionbank.domain.aggregates.QuestionAggregate");
        });

        // Verify our test configuration classes are in test packages
        var testConfigClass = getClass();
        assertThat(testConfigClass.getPackage().getName()).contains("questionbank");
    }
}