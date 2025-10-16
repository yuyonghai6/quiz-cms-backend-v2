import { check, group } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
  },
};

const BASE_URL = 'http://139.180.135.117:8765';

export default function () {
  // Happy Path Test
  group('Happy Path - Create Default Question Bank', () => {
    // Generate unique userId using timestamp to avoid conflicts
    const uniqueUserId = Date.now();

    const payload = JSON.stringify({
      userId: uniqueUserId,
      userEmail: 'test.user@example.com',
      metadata: {
        createdBy: 'k6-functional-test',
        createdAt: new Date().toISOString(),
        requestId: `test-req-${uniqueUserId}`
      }
    });

    const params = {
      headers: {
        'Content-Type': 'application/json',
      },
    };

    console.log(`\n📝 Testing Happy Path with userId: ${uniqueUserId}`);
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    // Validate response
    const checks = check(res, {
      '✓ status is 201 Created': (r) => r.status === 201,
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
          return body.message && body.message.includes('created successfully');
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
      '✓ userId matches request': (r) => {
        try {
          return r.json().data.userId === uniqueUserId;
        } catch (e) {
          return false;
        }
      },
      '✓ questionBankId exists and is positive': (r) => {
        try {
          const bankId = r.json().data.questionBankId;
          return bankId && bankId > 0;
        } catch (e) {
          return false;
        }
      },
      '✓ questionBankName is "Default Question Bank"': (r) => {
        try {
          return r.json().data.questionBankName === 'Default Question Bank';
        } catch (e) {
          return false;
        }
      },
      '✓ description exists': (r) => {
        try {
          const desc = r.json().data.description;
          return desc && desc.length > 0;
        } catch (e) {
          return false;
        }
      },
      '✓ active field is true': (r) => {
        try {
          return r.json().data.active === true;
        } catch (e) {
          return false;
        }
      },
      '✓ taxonomySetCreated is true': (r) => {
        try {
          return r.json().data.taxonomySetCreated === true;
        } catch (e) {
          return false;
        }
      },
      '✓ availableTaxonomy exists': (r) => {
        try {
          return r.json().data.availableTaxonomy !== null &&
                 r.json().data.availableTaxonomy !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ createdAt timestamp exists': (r) => {
        try {
          return r.json().data.createdAt !== null;
        } catch (e) {
          return false;
        }
      },
      '✓ header X-Question-Bank-ID exists': (r) => {
        return r.headers['X-Question-Bank-Id'] !== undefined ||
               r.headers['X-Question-Bank-ID'] !== undefined;
      },
    });

    if (checks) {
      console.log('✅ Happy Path: ALL CHECKS PASSED');

      // Print response for debugging
      try {
        const body = res.json();
        console.log(`   Created questionBankId: ${body.data.questionBankId}`);
        console.log(`   Message: ${body.message}`);
      } catch (e) {
        // Silent fail on logging
      }
    } else {
      console.log('❌ Happy Path: SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });






}
