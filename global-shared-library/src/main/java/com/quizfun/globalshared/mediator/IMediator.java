package com.quizfun.globalshared.mediator;

public interface IMediator {
    <T> Result<T> send(ICommand<T> command);
    <T> Result<T> send(IQuery<T> query);
}