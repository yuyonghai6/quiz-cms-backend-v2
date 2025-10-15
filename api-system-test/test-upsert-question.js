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

export default function () {
  // Happy Path Test - Create New MCQ Question
  group('Happy Path - Create New MCQ Question (Insert)', () => {
    // Generate unique source_question_id using timestamp
    const uniqueSourceQuestionId = `test-mcq-${Date.now()}`;

    const payload = JSON.stringify({
      source_question_id: uniqueSourceQuestionId,
      question_type: 'mcq',
      title: 'Capital of France',
      content: '<p>What is the capital of France?</p>',
      status: 'draft',
      solution_explanation: '<p>Paris is the capital and largest city of France.</p>',
      mcq_data: {
        options: [
          { id: 1, text: 'London', is_correct: false, explanation: 'London is the capital of the UK' },
          { id: 2, text: 'Berlin', is_correct: false, explanation: 'Berlin is the capital of Germany' },
          { id: 3, text: 'Paris', is_correct: true, explanation: 'Paris is the capital of France' },
          { id: 4, text: 'Madrid', is_correct: false, explanation: 'Madrid is the capital of Spain' }
        ],
        shuffle_options: false,
        allow_multiple_correct: false
      },
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
          { id: 'practice', name: 'Practice', color: '#17a2b8' }
        ],
        difficulty_level: {
          level: 'easy',
          numeric_value: 1,
          description: 'Suitable for beginners'
        }
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

    console.log(`\n📝 Testing Happy Path - Create MCQ with source_question_id: ${uniqueSourceQuestionId}`);
    const res = http.post(
      `${BASE_URL}/api/users/${TEST_USER_ID}/questionbanks/${TEST_QUESTION_BANK_ID}/questions`,
      payload,
      params
    );

    // Validate response
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
          const questionId = r.json().data.questionId;
          return questionId && questionId.length > 0;
        } catch (e) {
          return false;
        }
      },
      '✓ sourceQuestionId matches request': (r) => {
        try {
          return r.json().data.sourceQuestionId === uniqueSourceQuestionId;
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
          const count = r.json().data.taxonomyRelationshipsCount;
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
      console.log('✅ Happy Path (Create MCQ): ALL CHECKS PASSED');

      // Print response for debugging
      try {
        const body = res.json();
        console.log(`   Created questionId: ${body.data.questionId}`);
        console.log(`   Source questionId: ${body.data.sourceQuestionId}`);
        console.log(`   Operation: ${body.data.operation}`);
        console.log(`   Taxonomy relationships: ${body.data.taxonomyRelationshipsCount}`);
        console.log(`   Message: ${body.message}`);
      } catch (e) {
        // Silent fail on logging
      }
    } else {
      console.log('❌ Happy Path (Create MCQ): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });
}
