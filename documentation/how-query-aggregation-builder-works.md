# Question Query Aggregation Builder

This document explains the purpose and usage of `QuestionQueryAggregationBuilder` in the query-side module, along with the filtering and pagination semantics used to retrieve questions from MongoDB.

## Location
- Class: `com.quizfun.questionbankquery.infrastructure.persistence.repositories.QuestionQueryAggregationBuilder`
- Used by: `com.quizfun.questionbankquery.infrastructure.persistence.repositories.MongoQuestionQueryRepository`

## Responsibilities
The builder encapsulates construction of the MongoDB aggregation pipeline for querying questions:
- Build `$match` criteria for mandatory identifiers and optional filters
- Apply sorting based on allowed fields
- Apply pagination via `$skip` and `$limit`

## Filters and Semantics
- Required filters:
  - `userId` (maps to `user_id`)
  - `questionBankId` (maps to `question_bank_id`)
- Optional taxonomy filters:
  - `categories` (List<String>): AND semantics using `$all` against `taxonomy.categories`
  - `tags` (List<String>): OR semantics using `$in` against `taxonomy.tags`
  - `quizzes` (List<String>): OR semantics using `$in` against `taxonomy.quizzes`
- Optional search filter:
  - `searchText` (String):
    - By default, case-insensitive substring match on `question_text` using regex.
    - When `sortBy = relevance`, repository switches to MongoDB full-text search using `TextQuery` and sorts by score.

Notes:
- When an optional filter is null or empty, it is not included in the `$match` criteria.
- Search is implemented as a regex over `question_text` for now; we can migrate to MongoDB `$text` search when a text index and weights are introduced.

## Sorting
 Supported sort fields are validated in `QueryQuestionsRequest` and mapped to snake_case fields:
- `createdAt` → `created_at`
- `updatedAt` → `updated_at`
- `questionText` → `question_text`
  - `relevance` → Uses MongoDB text score ordering (requires a text index on `question_text`)

Sort direction accepts `asc` or `desc`.

## Pagination
- `page` (0-based) and `size` (1..100) from `QueryQuestionsRequest`
- `$skip = page * size`
- `$limit = size`

## Example
Given a request:
```
userId = 123,
questionBankId = 456,
categories = ["catA", "catB"],
tags = ["t1", "t2"],
quizzes = ["quiz-1"],
searchText = "capital",
sortBy = "questionText",
sortDirection = "asc",
page = 1,
size = 10
```
The builder will produce an aggregation pipeline equivalent to:
1. `$match` with:
   - `user_id: 123`
   - `question_bank_id: 456`
   - `taxonomy.categories: { $all: ["catA", "catB"] }`
   - `taxonomy.tags: { $in: ["t1", "t2"] }`
   - `taxonomy.quizzes: { $in: ["quiz-1"] }`
   - `question_text: { $regex: /capital/i }`
2. `$sort: { question_text: 1 }`
3. `$skip: 10`
4. `$limit: 10`

## Tests
- Integration tests (end-to-end via Testcontainers):
  - `MongoQuestionQueryRepositoryIntegrationTest` verifies filtering (AND/OR), search, pagination, sorting, count, and field mapping.
- Unit tests (builder-focused):
  - `QuestionQueryAggregationBuilderTest` verifies criteria construction, sort mapping, and pagination math.
  - Full-text behavior is covered via integration tests that create a text index at runtime for the test container.

## When to extend
- Add new filter fields to `QueryQuestionsRequest` and update `QuestionQueryAggregationBuilder.buildMatchCriteria` accordingly.
- Keep sort fields whitelisted in `QueryQuestionsRequest` to avoid injection and ensure stable mapping.
- If full-text ranking is needed, add a text index and switch `searchText` to `$text` with `$meta` score sorting.
