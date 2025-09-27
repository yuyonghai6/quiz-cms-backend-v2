package com.quizfun.orchestrationlayer.cqrs;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.MediatorImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lightweight test to verify mediator configuration without full Spring Boot context.
 * This validates the basic CQRS infrastructure without business layer dependencies.
 */
class MediatorRegistrationTest {

    @Test
    void shouldCreateMediatorWithMinimalConfiguration() {
        // Given - Create and refresh Spring context for testing
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.refresh(); // IMPORTANT: Must refresh context before MediatorImpl can use it

        try {
            // When - Create mediator manually
            MediatorImpl mediator = new MediatorImpl(context);

            // Then
            assertThat(mediator).isNotNull();
            assertThat(mediator).isInstanceOf(IMediator.class);
        } finally {
            context.close(); // Clean up resources
        }
    }

    @Test
    void shouldVerifyMediatorInterfaceContract() {
        // Given
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.refresh(); // IMPORTANT: Must refresh context

        try {
            IMediator mediator = new MediatorImpl(context);

            // When/Then - Verify mediator implements the required interface
            assertThat(mediator).isNotNull();
            assertThat(mediator).isInstanceOf(IMediator.class);

            // Verify it has the required methods (will compile if methods exist)
            assertThat(mediator.getClass().getMethods())
                    .extracting("name")
                    .contains("send");
        } finally {
            context.close(); // Clean up resources
        }
    }

    @Test
    void shouldVerifyMediatorCanBeInstantiatedWithoutErrors() {
        // Given/When - Create mediator with properly initialized context
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.refresh(); // IMPORTANT: Must refresh context

        try {
            // This should not throw any exceptions
            MediatorImpl mediator = new MediatorImpl(context);

            // Then
            assertThat(mediator).isNotNull();
        } finally {
            context.close(); // Clean up resources
        }
    }
}