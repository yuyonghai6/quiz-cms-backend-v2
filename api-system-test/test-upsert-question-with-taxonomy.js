import { check, group } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
  },
};

const BASE_URL = 'http://localhost:8765';

// Use existing test data from MongoDB
const TEST_USER_ID = 1760085803933;
const TEST_QUESTION_BANK_ID = 1760085804015000;

/**
 * Generate UUID v7 format (timestamp-based)
 * Format: xxxxxxxx-xxxx-7xxx-xxxx-xxxxxxxxxxxx
 */
function generateUUIDv7() {
  const timestamp = Date.now();
  const timestampHex = timestamp.toString(16).padStart(12, '0');
  const randomPart1 = Math.random().toString(16).substring(2, 6);
  const randomPart2 = Math.random().toString(16).substring(2, 6);
  const randomPart3 = Math.random().toString(16).substring(2, 14);

  return `${timestampHex.substring(0, 8)}-${timestampHex.substring(8, 12)}-7${randomPart1.substring(0, 3)}-${randomPart2}-${randomPart3}`;
}

/**
 * Create minimal MCQ question payload with valid taxonomy
 */
function createMinimalMCQPayload(sourceQuestionId) {
  return JSON.stringify({
    source_question_id: sourceQuestionId,
    question_type: 'mcq',
    title: 'Simple MCQ Question',
    content: '<p>What is 2+2?</p>',
    status: 'draft',
    solution_explanation: '<p>Basic arithmetic</p>',
    taxonomy: {
      categories: {
        level_1: {
          id: 'general',
          name: 'General',
          slug: 'general',
          parent_id: null
        }
      },
      difficulty_level: {
        level: 'easy',
        numeric_value: 1,
        description: 'Suitable for beginners'
      }
    },
    mcq_data: {
      options: [
        { id: 1, text: '3', is_correct: false, explanation: 'Incorrect' },
        { id: 2, text: '4', is_correct: true, explanation: 'Correct!' }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    },
    metadata: {
      created_source: 'k6-functional-test',
      last_modified: new Date().toISOString(),
      version: 1,
      author_id: TEST_USER_ID
    }
  });
}

/**
 * Create full MCQ question payload with all taxonomy fields
 */
function createFullMCQPayload(sourceQuestionId) {
  return JSON.stringify({
    source_question_id: sourceQuestionId,
    question_type: 'mcq',
    title: 'Advanced MCQ with Full Taxonomy',
    content: '<p>Select the correct answer about K6 load testing</p>',
    status: 'draft',
    solution_explanation: '<p>K6 is a modern load testing tool</p>',
    points: 10,
    display_order: 1,
    taxonomy: {
      categories: {
        level_1: {
          id: 'general',
          name: 'General',
          slug: 'general',
          parent_id: null
        }
      },
      tags: [
        { id: 'beginner', name: 'Beginner', color: '#28a745' },
        { id: 'practice', name: 'Practice', color: '#007bff' }
      ],
      difficulty_level: {
        level: 'easy',
        numeric_value: 1,
        description: 'Suitable for beginners'
      }
    },
    mcq_data: {
      options: [
        { id: 1, text: 'Load testing framework', is_correct: true, explanation: 'Correct!' },
        { id: 2, text: 'Database system', is_correct: false, explanation: 'Incorrect' },
        { id: 3, text: 'Web server', is_correct: false, explanation: 'Incorrect' },
        { id: 4, text: 'Mobile app', is_correct: false, explanation: 'Incorrect' }
      ],
      shuffle_options: false,
      allow_multiple_correct: false
    },
    metadata: {
      created_source: 'k6-functional-test',
      last_modified: new Date().toISOString(),
      version: 1,
      author_id: TEST_USER_ID
    }
  });
}

export default function () {
  // ============================================================================
  // HAPPY PATH TESTS
  // ============================================================================

  group('Happy Path - Create New MCQ Question (Minimal Taxonomy)', () => {
    const uniqueSourceId = generateUUIDv7();
    const payload = createMinimalMCQPayload(uniqueSourceId);

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path - Create Minimal MCQ with UUID: ${uniqueSourceId}`);
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 200 OK': (r) => r.status === 200,
      '✓ content-type is JSON': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/json'),
      '✓ response has success field': (r) => {
        try {
          const body = r.json();
          return body.hasOwnProperty('success');
        } catch (e) {
          return false;
        }
      },
      '✓ success is true': (r) => {
        try {
          return r.json().success === true;
        } catch (e) {
          return false;
        }
      },
      '✓ message indicates success': (r) => {
        try {
          const body = r.json();
          return body.message && body.message.length > 0;
        } catch (e) {
          return false;
        }
      },
      '✓ data object exists': (r) => {
        try {
          return r.json().data !== null && typeof r.json().data === 'object';
        } catch (e) {
          return false;
        }
      },
      '✓ questionId exists': (r) => {
        try {
          const data = r.json().data;
          const questionId = data.question_id || data.questionId;
          return questionId && questionId.length > 0;
        } catch (e) {
          return false;
        }
      },
      '✓ sourceQuestionId matches request': (r) => {
        try {
          const data = r.json().data;
          const sourceId = data.source_question_id || data.sourceQuestionId;
          return sourceId === uniqueSourceId;
        } catch (e) {
          return false;
        }
      },
      '✓ operation is created': (r) => {
        try {
          return r.json().data.operation === 'created';
        } catch (e) {
          return false;
        }
      },
      '✓ taxonomyRelationshipsCount exists': (r) => {
        try {
          const data = r.json().data;
          const count = data.taxonomy_relationships_count || data.taxonomyRelationshipsCount;
          return count !== null && count !== undefined && count >= 0;
        } catch (e) {
          return false;
        }
      },
      '✓ header X-Operation exists': (r) => {
        return r.headers['X-Operation'] !== undefined;
      },
      '✓ header X-Question-Id exists': (r) => {
        return r.headers['X-Question-Id'] !== undefined;
      },
    });

    if (checks) {
      console.log('✅ Happy Path (Minimal MCQ): ALL CHECKS PASSED');
      try {
        const body = res.json();
        const data = body.data;
        console.log(`   Created questionId: ${data.question_id || data.questionId}`);
        console.log(`   Source questionId: ${data.source_question_id || data.sourceQuestionId}`);
        console.log(`   Operation: ${data.operation}`);
        console.log(`   Taxonomy relationships: ${data.taxonomy_relationships_count || data.taxonomyRelationshipsCount}`);
      } catch (e) {
        // Silent fail on logging
      }
    } else {
      console.log('❌ Happy Path (Minimal MCQ): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Happy Path - Create New MCQ Question (Full Taxonomy)', () => {
    const uniqueSourceId = generateUUIDv7();
    const payload = createFullMCQPayload(uniqueSourceId);

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path - Create Full MCQ with UUID: ${uniqueSourceId}`);
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 200 OK': (r) => r.status === 200,
      '✓ success is true': (r) => {
        try {
          return r.json().success === true;
        } catch (e) {
          return false;
        }
      },
      '✓ operation is created': (r) => {
        try {
          return r.json().data.operation === 'created';
        } catch (e) {
          return false;
        }
      },
      '✓ taxonomy relationships count > 0': (r) => {
        try {
          const data = r.json().data;
          const count = data.taxonomy_relationships_count || data.taxonomyRelationshipsCount;
          return count > 0;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Happy Path (Full MCQ): ALL CHECKS PASSED');
      try {
        const body = res.json();
        const data = body.data;
        console.log(`   Taxonomy relationships: ${data.taxonomy_relationships_count || data.taxonomyRelationshipsCount}`);
      } catch (e) {
        // Silent fail
      }
    } else {
      console.log('❌ Happy Path (Full MCQ): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Happy Path - Upsert (Update Existing Question)', () => {
    const uniqueSourceId = generateUUIDv7();

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path - Upsert with UUID: ${uniqueSourceId}`);

    // First call - CREATE
    const payload1 = createMinimalMCQPayload(uniqueSourceId);
    const res1 = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload1,
      params
    );

    const checks1 = check(res1, {
      '✓ first call - status is 200': (r) => r.status === 200,
      '✓ first call - operation is created': (r) => {
        try {
          return r.json().data.operation === 'created';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks1) {
      console.log('  ✅ First call: Question CREATED');
    } else {
      console.log('  ❌ First call: FAILED');
      console.log('  Response Status:', res1.status);
      console.log('  Response Body:', res1.body);
    }

    // Second call - UPDATE (same source_question_id)
    const updatedPayload = JSON.parse(payload1);
    updatedPayload.title = 'Updated Title After Upsert';
    updatedPayload.content = '<p>Updated content for upsert test</p>';
    const payload2 = JSON.stringify(updatedPayload);

    const res2 = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload2,
      params
    );

    const checks2 = check(res2, {
      '✓ second call - status is 200': (r) => r.status === 200,
      '✓ second call - operation is updated': (r) => {
        try {
          return r.json().data.operation === 'updated';
        } catch (e) {
          return false;
        }
      },
      '✓ second call - same sourceQuestionId': (r) => {
        try {
          const data = r.json().data;
          const sourceId = data.source_question_id || data.sourceQuestionId;
          return sourceId === uniqueSourceId;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks2) {
      console.log('  ✅ Second call: Question UPDATED');
    } else {
      console.log('  ❌ Second call: FAILED');
      console.log('  Response Status:', res2.status);
      console.log('  Response Body:', res2.body);
    }
  });

  group('Happy Path - Create True/False Question', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'true_false',
      title: 'True/False Question',
      content: '<p>K6 is a load testing tool.</p>',
      status: 'draft',
      solution_explanation: '<p>K6 is indeed a load testing tool</p>',
      taxonomy: {
        categories: {
          level_1: {
            id: 'general',
            name: 'General',
            slug: 'general',
            parent_id: null
          }
        },
        difficulty_level: {
          level: 'easy',
          numeric_value: 1,
          description: 'Suitable for beginners'
        }
      },
      true_false_data: {
        statement: 'K6 is a load testing tool',
        correct_answer: true,
        explanation: 'K6 is a modern open-source load testing tool'
      },
      metadata: {
        created_source: 'k6-functional-test',
        last_modified: new Date().toISOString(),
        version: 1,
        author_id: TEST_USER_ID
      }
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path - Create True/False with UUID: ${uniqueSourceId}`);
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 200 OK': (r) => r.status === 200,
      '✓ success is true': (r) => {
        try {
          return r.json().success === true;
        } catch (e) {
          return false;
        }
      },
      '✓ operation is created': (r) => {
        try {
          return r.json().data.operation === 'created';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Happy Path (True/False): ALL CHECKS PASSED');
    } else {
      console.log('❌ Happy Path (True/False): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Happy Path - Create Essay Question', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'essay',
      title: 'Essay Question',
      content: '<p>Explain the benefits of load testing with K6.</p>',
      status: 'draft',
      solution_explanation: '<p>K6 provides scriptable, scalable load testing</p>',
      taxonomy: {
        categories: {
          level_1: {
            id: 'general',
            name: 'General',
            slug: 'general',
            parent_id: null
          }
        },
        tags: [
          { id: 'practice', name: 'Practice', color: '#007bff' }
        ],
        difficulty_level: {
          level: 'easy',
          numeric_value: 1,
          description: 'Suitable for beginners'
        }
      },
      essay_data: {
        prompt: 'Write a detailed explanation of K6 load testing benefits',
        min_words: 100,
        max_words: 500,
        rubric: [
          {
            criteria: 'Understanding of load testing',
            max_points: 50,
            description: 'Demonstrates clear understanding'
          }
        ],
        allow_file_upload: false
      },
      metadata: {
        created_source: 'k6-functional-test',
        last_modified: new Date().toISOString(),
        version: 1,
        author_id: TEST_USER_ID
      }
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path - Create Essay with UUID: ${uniqueSourceId}`);
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 200 OK': (r) => r.status === 200,
      '✓ success is true': (r) => {
        try {
          return r.json().success === true;
        } catch (e) {
          return false;
        }
      },
      '✓ operation is created': (r) => {
        try {
          return r.json().data.operation === 'created';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Happy Path (Essay): ALL CHECKS PASSED');
    } else {
      console.log('❌ Happy Path (Essay): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  // ============================================================================
  // UNHAPPY PATH TESTS
  // ============================================================================

  group('Unhappy Path - Missing Required Field (source_question_id)', () => {
    const payload = JSON.stringify({
      // Missing source_question_id
      question_type: 'mcq',
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      taxonomy: {
        categories: {
          level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
        },
        difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
      },
      mcq_data: {
        options: [
          { id: 1, text: 'A', is_correct: true },
          { id: 2, text: 'B', is_correct: false }
        ],
        shuffle_options: false,
        allow_multiple_correct: false
      }
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Missing source_question_id');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Missing field): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Missing field): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid Question Type', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'invalid_type', // Invalid type
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      taxonomy: {
        categories: {
          level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
        },
        difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
      },
      mcq_data: {
        options: [
          { id: 1, text: 'A', is_correct: true },
          { id: 2, text: 'B', is_correct: false }
        ],
        shuffle_options: false,
        allow_multiple_correct: false
      }
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Invalid question type');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Invalid type): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Invalid type): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Type Data Mismatch (MCQ type with essay_data)', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'mcq', // Says MCQ
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      taxonomy: {
        categories: {
          level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
        },
        difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
      },
      // But provides essay_data instead of mcq_data
      essay_data: {
        prompt: 'Write an essay',
        min_words: 100,
        max_words: 500
      }
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Type data mismatch');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Type mismatch): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Type mismatch): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Missing MCQ Data', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'mcq',
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      taxonomy: {
        categories: {
          level_1: { id: 'general', name: 'General', slug: 'general', parent_id: null }
        },
        difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
      }
      // Missing mcq_data
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Missing MCQ data');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Missing MCQ data): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Missing MCQ data): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid Question Bank (Non-existent)', () => {
    const uniqueSourceId = generateUUIDv7();
    const invalidQuestionBankId = 9999999999999;

    const payload = createMinimalMCQPayload(uniqueSourceId);

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Invalid question bank');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${invalidQuestionBankId}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 422': (r) => r.status === 422,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Invalid bank): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Invalid bank): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid Taxonomy Reference', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'mcq',
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      taxonomy: {
        categories: {
          level_1: {
            id: 'nonexistent-category', // This doesn't exist
            name: 'Nonexistent',
            slug: 'nonexistent',
            parent_id: null
          }
        },
        difficulty_level: { level: 'easy', numeric_value: 1, description: 'Easy' }
      },
      mcq_data: {
        options: [
          { id: 1, text: 'A', is_correct: true },
          { id: 2, text: 'B', is_correct: false }
        ],
        shuffle_options: false,
        allow_multiple_correct: false
      }
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Invalid taxonomy reference');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 422': (r) => r.status === 422,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Invalid taxonomy): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Invalid taxonomy): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Missing Taxonomy', () => {
    const uniqueSourceId = generateUUIDv7();

    const payload = JSON.stringify({
      source_question_id: uniqueSourceId,
      question_type: 'mcq',
      title: 'Test Question',
      content: '<p>Test</p>',
      status: 'draft',
      // Missing taxonomy
      mcq_data: {
        options: [
          { id: 1, text: 'A', is_correct: true },
          { id: 2, text: 'B', is_correct: false }
        ],
        shuffle_options: false,
        allow_multiple_correct: false
      }
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Missing taxonomy');
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
      '✓ success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Missing taxonomy): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Missing taxonomy): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid Path Parameters', () => {
    const uniqueSourceId = generateUUIDv7();
    const invalidUserId = -1; // Negative userId

    const payload = createMinimalMCQPayload(uniqueSourceId);

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Invalid path parameters (negative userId)');
    const res = http.post(
      `${BASE_URL}/api/users/${invalidUserId}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    const checks = check(res, {
      '✓ status is 400': (r) => r.status === 400,
    });

    if (checks) {
      console.log('✅ Unhappy Path (Invalid params): Correctly rejected');
    } else {
      console.log('❌ Unhappy Path (Invalid params): Unexpected response');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });
}
