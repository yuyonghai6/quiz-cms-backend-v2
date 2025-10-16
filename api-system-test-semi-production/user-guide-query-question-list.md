# User Guide: Querying a List of Questions

This guide explains how to use the API endpoint to query, filter, and paginate through a list of questions within a specific question bank.

## Endpoint Details

-   **HTTP Method**: `GET`
-   **URL**: `/api/v1/users/{userId}/question-banks/{questionBankId}/questions`

### URL Parameters

| Parameter        | Type     | Description                               |
| ---------------- | -------- | ----------------------------------------- |
| `userId`         | `Number` | The unique identifier for the user.       |
| `questionBankId` | `Number` | The unique identifier for the question bank. |

## Query Parameters

You can filter and paginate the results using the following query parameters.

| Parameter    | Type     | Description                                                                                                | Example                               |
| ------------ | -------- | ---------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `page`       | `Number` | The page number to retrieve (0-indexed). Defaults to `0`.                                                  | `page=1`                              |
| `size`       | `Number` | The number of questions to return per page. Defaults to `20`.                                              | `size=10`                             |
| `categories` | `String` | A comma-separated list of category slugs to filter by.                                                     | `categories=math,science`             |
| `tags`       | `String` | A comma-separated list of tag slugs to filter by.                                                          | `tags=algebra,calculus`               |
| `searchText` | `String` | A search term to find in the question's title or content.                                                  | `searchText=solve%20for%20x`          |

## Example Usage

Below are some `curl` examples demonstrating how to use the endpoint.

### 1. Basic Query with Pagination

This retrieves the first page of 20 questions.

```bash
curl "http://139.180.135.117:8765/api/v1/users/1760620095607/question-banks/1760620095622000/questions?page=0&size=20"
```

### 2. Filter by Category

This retrieves questions categorized under "Math".

```bash
curl "http://139.180.135.117:8765/api/v1/users/1760620095607/question-banks/1760620095622000/questions?categories=Math"
```

### 3. Search by Text

This retrieves questions containing the word "equation".

```bash
curl "http://139.180.135.117:8765/api/v1/users/1760620095607/question-banks/1760620095622000/questions?searchText=equation"
```

### 4. Combined Filters

This retrieves questions in the "Math" category, tagged with "algebra", and containing the word "solve". It returns a page size of 10.

```bash
curl "http://139.180.135.117:8765/api/v1/users/1760620095607/question-banks/1760620095622000/questions?categories=Math&tags=algebra&searchText=solve&page=0&size=10"
```

## Responses

### ✅ Success Response (`200 OK`)

On a successful query, the API returns a `200 OK` status code with a JSON body containing the list of questions and pagination details.

#### Sample Success Response Body

```json
{
    "questions": [
        {
            "id": "671234567890123456",
            "source_question_id": "018b6a7a-1b1a-71e4-a23f-123456789abc",
            "title": "What is K6?",
            "question_type": "mcq",
            "status": "published",
            "taxonomy": {
                "categories": [
                    { "name": "Technology", "slug": "technology" }
                ],
                "tags": [
                    { "name": "Testing", "slug": "testing" }
                ]
            },
            "last_modified": "2025-10-16T10:00:00Z"
        }
        // ... other question objects
    ],
    "pagination": {
        "currentPage": 0,
        "totalPages": 5,
        "totalElements": 95,
        "size": 20,
        "isFirst": true,
        "isLast": false
    }
}
```

### ❌ Error Response

If the request is invalid or an error occurs, the API will return a non-`200` status code (e.g., `400 Bad Request`, `404 Not Found`) with a response body detailing the error.
