package com.quizfun.orchestrationlayer.configuration;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.questionbank.application.ports.out.QuestionBanksPerUserRepository;
import com.quizfun.questionbank.application.ports.out.QuestionRepository;
import com.quizfun.questionbank.application.ports.out.TaxonomySetRepository;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MediatorConfig.
 * Validates mediator bean creation and configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MediatorConfigTest")
@Epic("Orchestration Layer")
@Story("Configuration")
class MediatorConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private QuestionBanksPerUserRepository questionBanksPerUserRepository;

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private TaxonomySetRepository taxonomySetRepository;

    @Test
    @DisplayName("Should create mediator bean successfully")
    @Description("Validates that the mediator bean is created and available in the application context")
    void shouldCreateMediatorBeanSuccessfully() {
        // Assert - Check that the bean exists (use bean name to avoid ambiguity)
        assertThat(applicationContext.containsBean("mediator")).isTrue();
        IMediator mediator = (IMediator) applicationContext.getBean("mediator");
        assertThat(mediator).isNotNull();
    }

    @Test
    @DisplayName("Should have mediator config bean in context")
    @Description("Validates that MediatorConfig is registered as a Spring bean")
    void shouldHaveMediatorConfigBeanInContext() {
        // Assert
        assertThat(applicationContext.containsBean("mediatorConfig")).isTrue();
        MediatorConfig config = applicationContext.getBean(MediatorConfig.class);
        assertThat(config).isNotNull();
    }

    @Test
    @DisplayName("Should create mediator with application context")
    @Description("Validates that mediator is initialized with the Spring application context")
    void shouldCreateMediatorWithApplicationContext() {
        // Act - Create a new instance directly (simulating what the config does)
        var config = new MediatorConfig();
        IMediator testMediator = config.mediator(applicationContext);

        // Assert
        assertThat(testMediator).isNotNull();
    }

    @Test
    @DisplayName("Should trigger application ready event listener")
    @Description("Validates that the application ready event listener is configured")
    void shouldTriggerApplicationReadyEventListener() {
        // This test validates that the event listener method exists and doesn't throw
        var config = applicationContext.getBean(MediatorConfig.class);

        // Act & Assert - Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> {
            config.logMediatorStatus();
        });
    }
}
