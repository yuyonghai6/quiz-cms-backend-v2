package com.quizfun.globalshared.mediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Service
public class MediatorImpl implements IMediator {

    private final Map<Class<?>, ICommandHandler<?, ?>> handlerRegistry = new HashMap<>();
    private final Map<Class<?>, IQueryHandler<?, ?>> queryHandlerRegistry = new HashMap<>();
    private final ApplicationContext applicationContext;

    @Autowired
    public MediatorImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        registerHandlers();
        registerQueryHandlers();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> send(ICommand<T> command) {
        Class<?> commandType = command.getClass();
        ICommandHandler<ICommand<T>, T> handler = (ICommandHandler<ICommand<T>, T>) handlerRegistry.get(commandType);

        if (handler == null) {
            return Result.failure("No handler found for command: " + commandType.getSimpleName());
        }

        try {
            return handler.handle(command);
        } catch (Exception e) {
            return Result.failure("Error handling command: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> send(IQuery<T> query) {
        Class<?> queryType = query.getClass();
        IQueryHandler<IQuery<T>, T> handler = (IQueryHandler<IQuery<T>, T>) queryHandlerRegistry.get(queryType);

        if (handler == null) {
            return Result.failure("No handler found for query: " + queryType.getSimpleName());
        }

        try {
            return handler.handle(query);
        } catch (Exception e) {
            return Result.failure("Error handling query: " + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private void registerHandlers() {
        Map<String, ICommandHandler> handlers = applicationContext.getBeansOfType(ICommandHandler.class);

        for (ICommandHandler<?, ?> handler : handlers.values()) {
            Class<?> commandType = getCommandTypeFromHandler(handler);
            if (commandType != null) {
                handlerRegistry.put(commandType, handler);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void registerQueryHandlers() {
        Map<String, IQueryHandler> handlers = applicationContext.getBeansOfType(IQueryHandler.class);

        for (IQueryHandler<?, ?> handler : handlers.values()) {
            Class<?> queryType = getQueryTypeFromHandler(handler);
            if (queryType != null) {
                queryHandlerRegistry.put(queryType, handler);
            }
        }
    }

    private Class<?> getCommandTypeFromHandler(ICommandHandler<?, ?> handler) {
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().equals(ICommandHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }

        return null;
    }

    private Class<?> getQueryTypeFromHandler(IQueryHandler<?, ?> handler) {
        Type[] genericInterfaces = handler.getClass().getGenericInterfaces();

        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().equals(IQueryHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                        return (Class<?>) typeArguments[0];
                    }
                }
            }
        }

        return null;
    }
}