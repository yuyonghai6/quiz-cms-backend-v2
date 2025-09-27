package com.quizfun.shared.common;

import java.util.function.Consumer;
import java.util.function.Function;

public class Result<T> {
    private final boolean isSuccess;
    private final T value;
    private final String error;
    private final String errorCode;

    private Result(boolean isSuccess, T value, String error, String errorCode) {
        this.isSuccess = isSuccess;
        this.value = value;
        this.error = error;
        this.errorCode = errorCode;
    }

    // Factory methods
    public static <T> Result<T> success(T value) {
        return new Result<>(true, value, null, null);
    }

    public static <T> Result<T> failure(String error) {
        return new Result<>(false, null, error, null);
    }

    public static <T> Result<T> failure(String errorCode, String error) {
        return new Result<>(false, null, error, errorCode);
    }

    // State accessors
    public boolean isSuccess() {
        return isSuccess;
    }

    public boolean isFailure() {
        return !isSuccess;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // Fluent operations
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isFailure()) {
            return new Result<>(false, null, error, errorCode);
        }
        try {
            U mappedValue = mapper.apply(value);
            return Result.success(mappedValue);
        } catch (Exception e) {
            return Result.failure("MAPPING_ERROR", "Error during mapping: " + e.getMessage());
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isFailure()) {
            return new Result<>(false, null, error, errorCode);
        }
        try {
            return mapper.apply(value);
        } catch (Exception e) {
            return Result.failure("FLATMAP_ERROR", "Error during flat mapping: " + e.getMessage());
        }
    }

    // Utility methods
    public T orElse(T defaultValue) {
        return isSuccess() ? value : defaultValue;
    }

    public Result<T> ifSuccess(Consumer<T> action) {
        if (isSuccess() && value != null) {
            action.accept(value);
        }
        return this;
    }

    public Result<T> ifFailure(Consumer<String> action) {
        if (isFailure() && error != null) {
            action.accept(error);
        }
        return this;
    }
}