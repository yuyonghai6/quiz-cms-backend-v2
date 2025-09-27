package com.quizfun.shared.validation;

import com.quizfun.shared.common.Result;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationHandlerTest {

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ValidationHandlerTest.Should link validation handlers in chain")
    @Description("Validates that validation handlers can be properly linked in a chain of responsibility pattern using setNext() method")
    void shouldLinkValidationHandlersInChain() {
        var firstHandler = new TestValidationHandler("first");
        var secondHandler = new TestValidationHandler("second");
        var thirdHandler = new TestValidationHandler("third");

        firstHandler.setNext(secondHandler).setNext(thirdHandler);

        assertThat(firstHandler.getNext()).isEqualTo(secondHandler);
        assertThat(secondHandler.getNext()).isEqualTo(thirdHandler);
        assertThat(thirdHandler.getNext()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ValidationHandlerTest.Should execute validation chain in correct order")
    @Description("Validates that chained validation handlers execute in the correct sequential order when processing validation requests")
    void shouldExecuteValidationChainInCorrectOrder() {
        var executionOrder = new ArrayList<String>();

        var firstHandler = new RecordingValidationHandler("first", executionOrder, true);
        var secondHandler = new RecordingValidationHandler("second", executionOrder, true);
        var thirdHandler = new RecordingValidationHandler("third", executionOrder, true);

        firstHandler.setNext(secondHandler).setNext(thirdHandler);

        var result = firstHandler.validate(new Object());

        assertThat(result.isSuccess()).isTrue();
        assertThat(executionOrder).containsExactly("first", "second", "third");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ValidationHandlerTest.Should implement fail-fast behavior")
    @Description("Validates that the validation chain stops execution immediately when a handler fails, implementing fail-fast behavior to avoid unnecessary processing")
    void shouldImplementFailFastBehavior() {
        var executionOrder = new ArrayList<String>();

        var firstHandler = new RecordingValidationHandler("first", executionOrder, true);
        var secondHandler = new RecordingValidationHandler("second", executionOrder, false);
        var thirdHandler = new RecordingValidationHandler("third", executionOrder, true);

        firstHandler.setNext(secondHandler).setNext(thirdHandler);

        var result = firstHandler.validate(new Object());

        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).contains("second validation failed");
        assertThat(executionOrder).containsExactly("first", "second");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ValidationHandlerTest.Should handle empty chain gracefully")
    @Description("Validates that a single validation handler without any chained handlers can process validation requests successfully")
    void shouldHandleEmptyChainGracefully() {
        var handler = new TestValidationHandler("only");

        var result = handler.validate(new Object());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("ValidationHandlerTest.Should support concurrent validation requests")
    @Description("Validates that validation handlers are thread-safe and can handle multiple concurrent validation requests without race conditions or data corruption")
    void shouldSupportConcurrentValidationRequests() throws InterruptedException {
        var handler = new ConcurrentTestValidationHandler();
        var executor = Executors.newFixedThreadPool(10);
        var results = new ConcurrentLinkedQueue<Result<Void>>();
        var latch = new CountDownLatch(100);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    var result = handler.validate(new Object());
                    results.add(result);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertThat(results).hasSize(100);
        assertThat(results.stream().allMatch(Result::isSuccess)).isTrue();
    }

    private static class TestValidationHandler extends ValidationHandler {
        private final String name;

        public TestValidationHandler(String name) {
            this.name = name;
        }

        @Override
        public Result<Void> validate(Object command) {
            return checkNext(command);
        }
    }

    private static class RecordingValidationHandler extends ValidationHandler {
        private final String name;
        private final List<String> executionOrder;
        private final boolean shouldSucceed;

        public RecordingValidationHandler(String name, List<String> executionOrder, boolean shouldSucceed) {
            this.name = name;
            this.executionOrder = executionOrder;
            this.shouldSucceed = shouldSucceed;
        }

        @Override
        public Result<Void> validate(Object command) {
            executionOrder.add(name);

            if (!shouldSucceed) {
                return Result.failure("VALIDATION_ERROR", name + " validation failed");
            }

            return checkNext(command);
        }
    }

    private static class ConcurrentTestValidationHandler extends ValidationHandler {
        @Override
        public Result<Void> validate(Object command) {
            return checkNext(command);
        }
    }
}


