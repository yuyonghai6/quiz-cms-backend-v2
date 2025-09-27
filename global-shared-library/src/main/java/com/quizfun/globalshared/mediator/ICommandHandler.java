package com.quizfun.globalshared.mediator;

public interface ICommandHandler<TCommand extends ICommand<TResult>, TResult> {
    Result<TResult> handle(TCommand command);
}