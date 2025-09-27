package com.quizfun.globalshared.mediator;

public interface IQueryHandler<TQuery extends IQuery<TResult>, TResult> {
    Result<TResult> handle(TQuery query);
}