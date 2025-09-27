package com.quizfun.shared.validation;

import com.quizfun.shared.common.Result;

public abstract class ValidationHandler {
    protected ValidationHandler next;

    public ValidationHandler setNext(ValidationHandler handler) {
        this.next = handler;
        return handler;
    }

    public abstract Result<Void> validate(Object command);

    protected Result<Void> checkNext(Object command) {
        if (next == null) {
            return Result.success(null);
        }
        return next.validate(command);
    }

    public ValidationHandler getNext() {
        return next;
    }
}


