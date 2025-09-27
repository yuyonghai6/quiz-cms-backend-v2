package com.quizfun.globalshared.mediator;

public record Result<T>(
    boolean success,
    String message,
    T data
) {
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "Operation completed successfully", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(false, message, null);
    }

    public static <T> Result<T> failure(String message, T data) {
        return new Result<>(false, message, data);
    }
}