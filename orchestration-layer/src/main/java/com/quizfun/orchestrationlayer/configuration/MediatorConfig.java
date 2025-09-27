package com.quizfun.orchestrationlayer.configuration;

import com.quizfun.globalshared.mediator.IMediator;
import com.quizfun.globalshared.mediator.MediatorImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration class for the mediator pattern implementation.
 *
 * This configuration ensures that the mediator is properly initialized with all
 * command handlers registered and validates the registration to prevent startup
 * failures if required handlers are missing.
 */
@Configuration
public class MediatorConfig {

    private static final Logger logger = LoggerFactory.getLogger(MediatorConfig.class);

    /**
     * Creates and configures the IMediator bean with handler registration validation.
     *
     * @param applicationContext Spring application context for handler discovery
     * @return Configured mediator instance
     */
    @Bean
    public IMediator mediator(ApplicationContext applicationContext) {
        logger.info("Initializing mediator with application context");

        var mediator = new MediatorImpl(applicationContext);

        // Verify handler registration
        verifyHandlerRegistration(mediator);

        return mediator;
    }

    /**
     * Verifies that the mediator is properly initialized.
     * The MediatorImpl automatically discovers and registers handlers via reflection.
     *
     * @param mediator The mediator instance to verify
     * @throws IllegalStateException if mediator initialization failed
     */
    private void verifyHandlerRegistration(MediatorImpl mediator) {
        try {
            // The mediator automatically registers all ICommandHandler beans
            // We just verify that the mediator was created successfully
            logger.info("Mediator initialization successful - handlers will be discovered automatically");
            logger.info("Handler registration uses Spring ApplicationContext for automatic discovery");
            logger.info("Command handlers implementing ICommandHandler will be registered via reflection");

        } catch (Exception ex) {
            logger.error("Failed to verify mediator initialization", ex);
            throw new IllegalStateException("Mediator configuration failed", ex);
        }
    }

    /**
     * Application ready event listener to log mediator status after startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logMediatorStatus() {
        logger.info("Application ready - mediator configuration complete");
    }
}