package com.quizfun.globalshared.mediator;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediatorImpl Tests")
class MediatorImplTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ICommandHandler<TestCommand, String> mockHandler;

    private MediatorImpl mediator;

    // Test command for testing
    record TestCommand(String value) implements ICommand<String> {}

    // Test query for testing
    record TestQuery(String criteria) implements IQuery<String> {}

    // Test handler implementation for testing
    static class TestCommandHandler implements ICommandHandler<TestCommand, String> {
        @Override
        public Result<String> handle(TestCommand command) {
            return Result.success("Handled: " + command.value());
        }
    }

    // Test query handler implementation for testing
    static class TestQueryHandler implements IQueryHandler<TestQuery, String> {
        @Override
        public Result<String> handle(TestQuery query) {
            return Result.success("Query result: " + query.criteria());
        }
    }

    // Exception-throwing handler for unhappy path testing
    static class ExceptionThrowingHandler implements ICommandHandler<TestCommand, String> {
        @Override
        public Result<String> handle(TestCommand command) {
            throw new RuntimeException("Handler exception");
        }
    }

    // Exception-throwing query handler for unhappy path testing
    static class ExceptionThrowingQueryHandler implements IQueryHandler<TestQuery, String> {
        @Override
        public Result<String> handle(TestQuery query) {
            throw new RuntimeException("Query handler exception");
        }
    }

    // Handler with non-class type parameter (for testing null type extraction)
    static class HandlerWithInvalidGeneric implements ICommandHandler {
        @Override
        public Result handle(ICommand command) {
            return Result.success("Should not be called");
        }
    }

    // Query handler with non-class type parameter (for testing null type extraction)
    static class QueryHandlerWithInvalidGeneric implements IQueryHandler {
        @Override
        public Result handle(IQuery query) {
            return Result.success("Should not be called");
        }
    }

    @BeforeEach
    void setUp() {
        // Setup mock ApplicationContext to return our test handlers
        Map<String, ICommandHandler> handlers = new HashMap<>();
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Happy Path: Should successfully execute command with registered handler")
    @Description("Validates that the mediator successfully routes commands to their registered handlers and returns the handler's result when execution completes normally")
    void shouldSuccessfullyExecuteCommandWithRegisteredHandler() {
        // Given
        Map<String, ICommandHandler> handlers = new HashMap<>();
        handlers.put("testHandler", new TestCommandHandler());
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);

        mediator = new MediatorImpl(applicationContext);
        TestCommand command = new TestCommand("test value");

        // When
        Result<String> result = mediator.send(command);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Operation completed successfully");
        assertThat(result.data()).isEqualTo("Handled: test value");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should return failure when no handler is registered")
    @Description("Validates that the mediator returns a failure result with an appropriate error message when no handler is registered for the given command type")
    void shouldReturnFailureWhenNoHandlerIsRegistered() {
        // Given
        TestCommand command = new TestCommand("test value");

        // When
        Result<String> result = mediator.send(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("No handler found for command: TestCommand");
        assertThat(result.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should return failure when handler throws exception")
    @Description("Validates that the mediator gracefully handles exceptions thrown by command handlers and returns a failure result with the exception message")
    void shouldReturnFailureWhenHandlerThrowsException() {
        // Given
        Map<String, ICommandHandler> handlers = new HashMap<>();
        handlers.put("exceptionHandler", new ExceptionThrowingHandler());
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);

        mediator = new MediatorImpl(applicationContext);
        TestCommand command = new TestCommand("test value");

        // When
        Result<String> result = mediator.send(command);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Error handling command: Handler exception");
        assertThat(result.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Happy Path: Should auto-discover multiple handlers")
    @Description("Validates that the mediator's auto-discovery mechanism correctly registers multiple handlers and successfully routes commands to the appropriate handler")
    void shouldAutoDiscoverMultipleHandlers() {
        // Given
        Map<String, ICommandHandler> handlers = new HashMap<>();
        handlers.put("handler1", new TestCommandHandler());
        handlers.put("handler2", new TestCommandHandler());
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);

        mediator = new MediatorImpl(applicationContext);
        TestCommand command = new TestCommand("multi-handler test");

        // When
        Result<String> result = mediator.send(command);

        // Then - Should successfully route to one of the handlers
        assertThat(result.success()).isTrue();
        assertThat(result.data()).contains("Handled: multi-handler test");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should handle null command gracefully")
    @Description("Validates that the mediator handles null command inputs gracefully, either by returning a failure result or throwing an appropriate exception")
    void shouldHandleNullCommandGracefully() {
        // Given
        Map<String, ICommandHandler> handlers = new HashMap<>();
        handlers.put("testHandler", new TestCommandHandler());
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);

        mediator = new MediatorImpl(applicationContext);

        // When & Then - This should throw NullPointerException which gets caught
        try {
            Result<String> result = mediator.send((ICommand<String>) null);
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Error handling command:");
        } catch (NullPointerException e) {
            // This is also acceptable behavior for null input
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Edge Case: Should handle handlers with invalid generic parameters")
    @Description("Validates that the mediator gracefully handles handlers with malformed generic type parameters by excluding them from registration")
    void shouldHandleHandlersWithInvalidGenericParameters() {
        // Given
        Map<String, ICommandHandler> handlers = new HashMap<>();
        handlers.put("invalidHandler", new HandlerWithInvalidGeneric());
        handlers.put("validHandler", new TestCommandHandler());
        when(applicationContext.getBeansOfType(ICommandHandler.class)).thenReturn(handlers);

        mediator = new MediatorImpl(applicationContext);
        TestCommand command = new TestCommand("test");

        // When
        Result<String> result = mediator.send(command);

        // Then - Should still work with the valid handler
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("Handled: test");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Edge Case: Should handle query handlers with invalid generic parameters")
    @Description("Validates that the mediator gracefully handles query handlers with malformed generic type parameters by excluding them from registration")
    void shouldHandleQueryHandlersWithInvalidGenericParameters() {
        // Given
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        queryHandlers.put("invalidQueryHandler", new QueryHandlerWithInvalidGeneric());
        queryHandlers.put("validQueryHandler", new TestQueryHandler());
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);
        TestQuery query = new TestQuery("test criteria");

        // When
        Result<String> result = mediator.send(query);

        // Then - Should still work with the valid handler
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo("Query result: test criteria");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Happy Path: Should successfully execute query with registered handler")
    @Description("Validates that the mediator successfully routes queries to their registered handlers and returns the handler's result when execution completes normally")
    void shouldSuccessfullyExecuteQueryWithRegisteredHandler() {
        // Given
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        queryHandlers.put("testQueryHandler", new TestQueryHandler());
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);
        TestQuery query = new TestQuery("search criteria");

        // When
        Result<String> result = mediator.send(query);

        // Then
        assertThat(result.success()).isTrue();
        assertThat(result.message()).isEqualTo("Operation completed successfully");
        assertThat(result.data()).isEqualTo("Query result: search criteria");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should return failure when no query handler is registered")
    @Description("Validates that the mediator returns a failure result with an appropriate error message when no handler is registered for the given query type")
    void shouldReturnFailureWhenNoQueryHandlerIsRegistered() {
        // Given
        TestQuery query = new TestQuery("search criteria");

        // When
        Result<String> result = mediator.send(query);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("No handler found for query: TestQuery");
        assertThat(result.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should return failure when query handler throws exception")
    @Description("Validates that the mediator gracefully handles exceptions thrown by query handlers and returns a failure result with the exception message")
    void shouldReturnFailureWhenQueryHandlerThrowsException() {
        // Given
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        queryHandlers.put("exceptionQueryHandler", new ExceptionThrowingQueryHandler());
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);
        TestQuery query = new TestQuery("search criteria");

        // When
        Result<String> result = mediator.send(query);

        // Then
        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Error handling query: Query handler exception");
        assertThat(result.data()).isNull();
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Happy Path: Should auto-discover multiple query handlers")
    @Description("Validates that the mediator's auto-discovery mechanism correctly registers multiple query handlers and successfully routes queries to the appropriate handler")
    void shouldAutoDiscoverMultipleQueryHandlers() {
        // Given
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        queryHandlers.put("queryHandler1", new TestQueryHandler());
        queryHandlers.put("queryHandler2", new TestQueryHandler());
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);
        TestQuery query = new TestQuery("multi-handler test");

        // When
        Result<String> result = mediator.send(query);

        // Then - Should successfully route to one of the handlers
        assertThat(result.success()).isTrue();
        assertThat(result.data()).contains("Query result: multi-handler test");
    }

    @Test
    @Epic("Enabler Epic-Core Infrastructure")
    @Story("story-000.shared-module-infrastructure-setup")
    @DisplayName("MediatorImplTest.Unhappy Path: Should handle null query gracefully")
    @Description("Validates that the mediator handles null query inputs gracefully, either by returning a failure result or throwing an appropriate exception")
    void shouldHandleNullQueryGracefully() {
        // Given
        Map<String, IQueryHandler> queryHandlers = new HashMap<>();
        queryHandlers.put("testQueryHandler", new TestQueryHandler());
        when(applicationContext.getBeansOfType(IQueryHandler.class)).thenReturn(queryHandlers);

        mediator = new MediatorImpl(applicationContext);

        // When & Then - This should throw NullPointerException which gets caught
        try {
            Result<String> result = mediator.send((IQuery<String>) null);
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Error handling query:");
        } catch (NullPointerException e) {
            // This is also acceptable behavior for null input
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }
}