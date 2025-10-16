# User Guide: Upserting Questions with Taxonomy

This guide explains how to use the API endpoint to create or update (upsert) questions within a specific question bank. The API supports multiple question types, including Multiple Choice (MCQ), True/False, and Essay.

The "upsert" functionality means that if you provide a `source_question_id` that already exists in the system, the existing question will be updated. If the `source_question_id` is new, a new question will be created.

## Endpoint Details

-   **HTTP Method**: `POST`
-   **URL**: `/api/users/{userId}/questionbanks/{questionBankId}/questions`

### URL Parameters

| Parameter        | Type     | Description                               |
| ---------------- | -------- | ----------------------------------------- |
| `userId`         | `Number` | The unique identifier for the user.       |
| `questionBankId` | `Number` | The unique identifier for the question bank. |

## Headers

| Header          | Value               | Description                        |
| --------------- | ------------------- | ---------------------------------- |
| `Content-Type`  | `application/json`  | Specifies the request body format. |

## Request Body

The request body is a JSON object that defines the question to be created or updated.

### Common Fields (Applicable to all question types)

| Field                | Type     | Description                                                                                                  | Required |
| -------------------- | -------- | ------------------------------------------------------------------------------------------------------------ | -------- |
| `source_question_id` | `String` | A client-generated unique identifier (UUID v7 format recommended). This ID is used to detect and update existing questions. | Yes      |
| `question_type`      | `String` | The type of question. Supported values: `mcq`, `true_false`, `essay`.                                        | Yes      |
| `title`              | `String` | The title of the question.                                                                                   | Yes      |
| `content`            | `String` | The main body of the question, which can include HTML content.                                               | Yes      |
| `status`             | `String` | The status of the question (e.g., `draft`, `published`).                                                     | Yes      |
| `solution_explanation`| `String`| An explanation for the correct solution, can include HTML.                                                   | No       |
| `points`             | `Number` | The point value of the question.                                                                             | No       |
| `display_order`      | `Number` | The order in which this question should appear relative to others.                                           | No       |
| `taxonomy`           | `Object` | An object containing classification data like categories, tags, and difficulty.                              | Yes      |
| `metadata`           | `Object` | An object containing metadata for the question.                                                              | Yes      |

---

### 1. Multiple Choice (`mcq`) Question

For `mcq` questions, you must include the `mcq_data` object.

#### `mcq_data` Fields

| Field                  | Type      | Description                                           |
| ---------------------- | --------- | ----------------------------------------------------- |
| `options`              | `Array`   | An array of option objects.                           |
| `options[].id`         | `Number`  | A unique ID for the option within the question.       |
| `options[].text`       | `String`  | The text of the option.                               |
| `options[].is_correct` | `Boolean` | `true` if this is a correct answer, otherwise `false`.|
| `options[].explanation`| `String`  | An optional explanation for why this option is correct/incorrect. |
| `shuffle_options`      | `Boolean` | Whether to shuffle the display order of options.      |
| `allow_multiple_correct`| `Boolean`| Whether more than one option can be correct.          |

#### Sample Payload: MCQ

```json
{
  "source_question_id": "018b6a7a-1b1a-71e4-a23f-123456789abc",
  "question_type": "mcq",
  "title": "What is K6?",
  "content": "<p>Select the correct description for K6.</p>",
  "status": "draft",
  "taxonomy": {
    "difficulty_level": { "level": "easy", "numeric_value": 1 }
  },
  "mcq_data": {
    "options": [
      { "id": 1, "text": "A load testing framework", "is_correct": true },
      { "id": 2, "text": "A database system", "is_correct": false }
    ],
    "shuffle_options": false,
    "allow_multiple_correct": false
  },
  "metadata": {
    "created_source": "k6-functional-test",
    "last_modified": "2025-10-16T10:00:00Z",
    "version": 1,
    "author_id": 1760620095607
  }
}
```

---

### 2. True/False (`true_false`) Question

For `true_false` questions, you must include the `true_false_data` object.

#### `true_false_data` Fields

| Field            | Type      | Description                                      |
| ---------------- | --------- | ------------------------------------------------ |
| `statement`      | `String`  | The statement to be evaluated.                   |
| `correct_answer` | `Boolean` | The correct answer (`true` or `false`).          |
| `explanation`    | `String`  | An optional explanation for the correct answer.  |

#### Sample Payload: True/False

```json
{
  "source_question_id": "018b6a7b-2c2b-72f5-b34a-987654321def",
  "question_type": "true_false",
  "title": "K6 Usage",
  "content": "<p>Is K6 a load testing tool?</p>",
  "status": "draft",
  "taxonomy": {
    "difficulty_level": { "level": "easy", "numeric_value": 1 }
  },
  "true_false_data": {
    "statement": "K6 is a popular open-source load testing tool.",
    "correct_answer": true
  },
  "metadata": {
    "created_source": "k6-functional-test",
    "last_modified": "2025-10-16T10:05:00Z",
    "version": 1,
    "author_id": 1760620095607
  }
}
```

---

### 3. Essay (`essay`) Question

For `essay` questions, you must include the `essay_data` object.

#### `essay_data` Fields

| Field               | Type      | Description                                      |
| ------------------- | --------- | ------------------------------------------------ |
| `prompt`            | `String`  | The prompt or question for the essay response.   |
| `min_words`         | `Number`  | The minimum required word count.                 |
| `max_words`         | `Number`  | The maximum allowed word count.                  |
| `rubric`            | `Array`   | An array of grading criteria objects.            |
| `allow_file_upload` | `Boolean` | Whether to allow file uploads as part of the answer. |

#### Sample Payload: Essay

```json
{
  "source_question_id": "018b6a7c-3d3c-73a6-c45b-abcdef123456",
  "question_type": "essay",
  "title": "Benefits of Load Testing",
  "content": "<p>Explain the primary benefits of load testing.</p>",
  "status": "draft",
  "taxonomy": {
    "difficulty_level": { "level": "medium", "numeric_value": 2 }
  },
  "essay_data": {
    "prompt": "In 100-200 words, explain the key benefits of performing load tests.",
    "min_words": 100,
    "max_words": 200,
    "allow_file_upload": false
  },
  "metadata": {
    "created_source": "k6-functional-test",
    "last_modified": "2025-10-16T10:10:00Z",
    "version": 1,
    "author_id": 1760620095607
  }
}
```

## Responses

### ✅ Success Response (`200 OK`)

On a successful upsert, the API returns a `200 OK` status code.

-   **Headers**:
    -   `X-Operation`: Indicates the operation performed (`created` or `updated`).
    -   `X-Question-Id`: The unique system-generated ID for the question.
-   **Body**: The response body confirms the success and provides key identifiers.

#### Sample Success Response Body

```json
{
    "success": true,
    "message": "Question with source ID 018b6a7a-1b1a-71e4-a23f-123456789abc processed successfully.",
    "data": {
        "question_id": "671234567890123456",
        "source_question_id": "018b6a7a-1b1a-71e4-a23f-123456789abc",
        "operation": "created",
        "taxonomy_relationships_count": 2
    }
}
```

### ❌ Error Response

If the request is invalid or an error occurs, the API will return a non-`200` status code (e.g., `400 Bad Request`, `404 Not Found`, `500 Internal Server Error`) with a response body detailing the error.
