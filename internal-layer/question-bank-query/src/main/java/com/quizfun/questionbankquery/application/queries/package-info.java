/**
 * CQRS Query layer for question-bank-query module.
 *
 * <p>This package contains queries and query handlers that implement the CQRS pattern
 * using the mediator pattern from global-shared-library.
 *
 * <h2>Pattern Overview</h2>
 * <pre>
 * Controller → Mediator.send(Query) → QueryHandler → Application Service → Repository
 * </pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li><strong>Query:</strong> Immutable request object implementing IQuery&lt;T&gt;</li>
 *   <li><strong>QueryHandler:</strong> Handler implementing IQueryHandler&lt;Q, T&gt;</li>
 *   <li><strong>Mediator:</strong> Routes queries to appropriate handlers</li>
 *   <li><strong>Result:</strong> Wrapper for success/failure responses</li>
 * </ul>
 *
 * <h2>Handler Registration</h2>
 * <p>Query handlers are automatically registered with the mediator:
 * <ol>
 *   <li>Handler is marked with @Service annotation</li>
 *   <li>Spring's component scanning discovers the handler</li>
 *   <li>Mediator uses reflection to extract generic type parameters</li>
 *   <li>At runtime, queries are routed to the matching handler</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // In Controller
 * QueryQuestionsRequest request = QueryQuestionsRequest.builder()
 *     .userId(userId)
 *     .questionBankId(questionBankId)
 *     .page(0)
 *     .size(20)
 *     .build();
 *
 * QueryQuestions query = new QueryQuestions(request);
 * Result&lt;QueryQuestionsResponse&gt; result = mediator.send(query);
 *
 * if (result.success()) {
 *     return ResponseEntity.ok(result.data());
 * } else {
 *     return ResponseEntity.badRequest().body(result.message());
 * }
 * </pre>
 *
 * @see com.quizfun.globalshared.mediator.IMediator
 * @see com.quizfun.globalshared.mediator.IQuery
 * @see com.quizfun.globalshared.mediator.IQueryHandler
 */
package com.quizfun.questionbankquery.application.queries;
