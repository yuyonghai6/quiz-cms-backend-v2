package com.quizfun.shared.config;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class SharedConfigurationTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("SharedConfigurationTest.Should load Spring context with shared configuration")
    @Description("Validates that the SharedConfiguration class can be loaded by Spring context and properly registers bean definitions for dependency injection")
    void shouldLoadSpringContextWithSharedConfiguration() {
        try (var context = new AnnotationConfigApplicationContext(SharedConfiguration.class)) {
            assertThat(context).isNotNull();
            assertThat(context.getBeanDefinitionCount()).isGreaterThan(0);
        }
    }
}


