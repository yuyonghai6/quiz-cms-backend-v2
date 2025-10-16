# User Guide: Creating a Default Question Bank on New User Registration

This guide provides instructions on how to use the API endpoint to create a default question bank for a new user. This is typically called when a new user signs up.

## Endpoint Details

-   **HTTP Method**: `POST`
-   **URL**: `/api/users/default-question-bank`

## Headers

| Header          | Value               | Description                   |
| --------------- | ------------------- | ----------------------------- |
| `Content-Type`  | `application/json`  | Specifies the request body format. |

## Request Body

The request body must be a JSON object containing the new user's information.

### Fields

| Field         | Type     | Description                                                                 | Required |
| ------------- | -------- | --------------------------------------------------------------------------- | -------- |
| `userId`      | `Number` | A unique identifier for the user. It's recommended to use a timestamp or a unique ID from your system. | Yes      |
| `userEmail`   | `String` | The email address of the user.                                              | Yes      |
| `metadata`    | `Object` | Contains metadata about the request.                                        | Yes      |
| `metadata.createdBy` | `String` | Identifier for the client or process making the request (e.g., `signup-flow`). | Yes      |
| `metadata.createdAt` | `String` | The ISO 8601 timestamp for when the request was created.                    | Yes      |
| `metadata.requestId` | `String` | A unique ID for tracking the request.                                       | Yes      |

### Sample Request Payload

```json
{
  "userId": 1678886400000,
  "userEmail": "new.user@example.com",
  "metadata": {
    "createdBy": "signup-service",
    "createdAt": "2025-10-16T12:00:00Z",
    "requestId": "req-abc-123"
  }
}
```

## Responses

### ✅ Success Response (201 Created)

On successful creation, the API returns a `201 Created` status code.

-   **Headers**: A `X-Question-Bank-ID` header is returned containing the ID of the newly created question bank.
-   **Body**: The response body contains details of the created question bank.

#### Sample Success Response Body

```json
{
    "success": true,
    "message": "Default question bank for user 1678886400000 created successfully.",
    "data": {
        "userId": 1678886400000,
        "questionBankId": 1728994374700,
        "questionBankName": "Default Question Bank",
        "description": "A default question bank for the user.",
        "active": true,
        "taxonomySetCreated": true,
        "availableTaxonomy": {
            "Difficulty": ["Easy", "Medium", "Hard"],
            "Topic": ["General Knowledge"],
            "Skill": ["Recall", "Analysis"]
        },
        "createdAt": "2025-10-16T09:32:54.700Z"
    }
}
```

### Key Fields in Response Data

| Field                 | Type     | Description                                                              |
| --------------------- | -------- | ------------------------------------------------------------------------ |
| `userId`              | `Number` | The user ID associated with this question bank.                          |
| `questionBankId`      | `Number` | The unique identifier for the newly created question bank.               |
| `questionBankName`    | `String` | The name of the bank, which is "Default Question Bank".                  |
| `active`              | `Boolean`| Indicates if the question bank is active. Defaults to `true`.            |
| `taxonomySetCreated`  | `Boolean`| Confirms that the default set of taxonomies was created.                 |
| `availableTaxonomy`   | `Object` | An object listing the default taxonomies (e.g., Difficulty, Topic, Skill).|
| `createdAt`           | `String` | The ISO 8601 timestamp for when the question bank was created.           |

### ❌ Error Response

If the request is invalid or an error occurs, the API will return a non-`201` status code (e.g., `400 Bad Request`, `500 Internal Server Error`) with a response body detailing the error.
