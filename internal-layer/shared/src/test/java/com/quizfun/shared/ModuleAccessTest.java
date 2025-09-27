package com.quizfun.shared;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ModuleAccessTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ModuleAccessTest.Should access shared module from question-bank module")
    @Description("Validates that classes from the shared module can be accessed and loaded from other modules, ensuring proper inter-module dependencies")
    void shouldAccessSharedModuleFromQuestionBank() {
        // This test will fail initially when shared module doesn't exist
        assertDoesNotThrow(() -> {
            Class.forName("com.quizfun.shared.domain.AggregateRoot");
        });
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ModuleAccessTest.Should resolve Maven dependencies correctly")
    @Description("Validates that Maven dependency resolution works correctly for the shared module and Spring context can be initialized without classpath issues")
    void shouldResolveMavenDependencies() {
        // Test Maven dependency resolution
        var context = new AnnotationConfigApplicationContext();
        assertThat(context.getEnvironment()).isNotNull();
    }
}