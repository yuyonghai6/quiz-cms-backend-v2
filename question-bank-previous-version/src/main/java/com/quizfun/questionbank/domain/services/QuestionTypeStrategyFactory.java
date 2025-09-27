package com.quizfun.questionbank.domain.services;

import com.quizfun.questionbank.domain.entities.QuestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for managing and selecting question type strategies.
 * Uses Spring's dependency injection to auto-discover all available strategies
 * and provides type-safe strategy selection.
 */
@Component
public class QuestionTypeStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(QuestionTypeStrategyFactory.class);
    private final Map<QuestionType, QuestionTypeStrategy> strategies;

    /**
     * Constructor that receives all strategy implementations via Spring dependency injection.
     * Automatically registers strategies by their supported question types.
     *
     * @param strategyList All available strategy implementations discovered by Spring
     */
    public QuestionTypeStrategyFactory(List<QuestionTypeStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                this::findSupportedType,
                Function.identity()
            ));

        logger.info("Registered question type strategies: {}",
                   strategies.keySet().stream()
                       .map(QuestionType::getValue)
                       .collect(Collectors.joining(", ")));

        logger.debug("Strategy details: {}",
                    strategies.entrySet().stream()
                        .map(entry -> entry.getKey().getValue() + " -> " + entry.getValue().getStrategyName())
                        .collect(Collectors.joining(", ")));
    }

    /**
     * Gets the appropriate strategy for the given question type.
     *
     * @param type The question type to get a strategy for
     * @return The strategy that can handle the given type
     * @throws UnsupportedQuestionTypeException if no strategy is found for the type
     */
    public QuestionTypeStrategy getStrategy(QuestionType type) {
        var strategy = strategies.get(type);
        if (strategy == null) {
            logger.error("No strategy found for question type: {}. Available types: {}",
                        type.getValue(),
                        getSupportedTypes().stream()
                            .map(QuestionType::getValue)
                            .collect(Collectors.joining(", ")));

            throw new UnsupportedQuestionTypeException(
                String.format("No strategy found for question type: %s", type.getValue())
            );
        }

        logger.debug("Selected strategy '{}' for question type '{}'",
                    strategy.getStrategyName(), type.getValue());

        return strategy;
    }

    /**
     * Returns all question types that have registered strategies.
     *
     * @return Set of supported question types
     */
    public Set<QuestionType> getSupportedTypes() {
        return strategies.keySet();
    }

    /**
     * Checks if a question type is supported by any registered strategy.
     *
     * @param type The question type to check
     * @return true if the type is supported, false otherwise
     */
    public boolean isSupported(QuestionType type) {
        return strategies.containsKey(type);
    }

    /**
     * Finds the question type that a strategy supports.
     * Each strategy must support exactly one question type.
     *
     * @param strategy The strategy to analyze
     * @return The question type supported by the strategy
     * @throws IllegalStateException if the strategy doesn't support exactly one type
     */
    private QuestionType findSupportedType(QuestionTypeStrategy strategy) {
        var supportedTypes = Arrays.stream(QuestionType.values())
            .filter(strategy::supports)
            .collect(Collectors.toList());

        if (supportedTypes.isEmpty()) {
            throw new IllegalStateException(
                String.format("Strategy '%s' must support at least one question type",
                            strategy.getClass().getSimpleName())
            );
        }

        if (supportedTypes.size() > 1) {
            throw new IllegalStateException(
                String.format("Strategy '%s' must support exactly one question type, but supports: %s",
                            strategy.getClass().getSimpleName(),
                            supportedTypes.stream()
                                .map(QuestionType::getValue)
                                .collect(Collectors.joining(", ")))
            );
        }

        var supportedType = supportedTypes.get(0);
        logger.debug("Strategy '{}' registered for question type '{}'",
                    strategy.getClass().getSimpleName(), supportedType.getValue());

        return supportedType;
    }
}