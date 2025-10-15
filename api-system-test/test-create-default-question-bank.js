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

  // Unhappy Path Tests
  group('Unhappy Path - Missing userId', () => {
    const payload = JSON.stringify({
      userEmail: 'test@example.com'
      // userId is missing
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Missing userId');
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const checks = check(res, {
      '✓ status is 400 Bad Request': (r) => r.status === 400,
      '✓ response has error field': (r) => {
        try {
          return r.json().error !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ error is "Bad Request"': (r) => {
        try {
          return r.json().error === 'Bad Request';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Missing userId): ALL CHECKS PASSED');
    } else {
      console.log('❌ Unhappy Path (Missing userId): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid userId (null)', () => {
    const payload = JSON.stringify({
      userId: null,
      userEmail: 'test@example.com'
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: userId is null');
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const checks = check(res, {
      '✓ status is 400 Bad Request': (r) => r.status === 400,
      '✓ response has error field': (r) => {
        try {
          return r.json().error !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ error is "Bad Request"': (r) => {
        try {
          return r.json().error === 'Bad Request';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (userId null): ALL CHECKS PASSED');
    } else {
      console.log('❌ Unhappy Path (userId null): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid userId (zero)', () => {
    const payload = JSON.stringify({
      userId: 0,
      userEmail: 'test@example.com'
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: userId is zero');
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const checks = check(res, {
      '✓ status is 400 Bad Request': (r) => r.status === 400,
      '✓ response has error field': (r) => {
        try {
          return r.json().error !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ error is "Bad Request"': (r) => {
        try {
          return r.json().error === 'Bad Request';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (userId zero): ALL CHECKS PASSED');
    } else {
      console.log('❌ Unhappy Path (userId zero): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid userId (negative)', () => {
    const payload = JSON.stringify({
      userId: -123456,
      userEmail: 'test@example.com'
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: userId is negative');
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const checks = check(res, {
      '✓ status is 400 Bad Request': (r) => r.status === 400,
      '✓ response has error field': (r) => {
        try {
          return r.json().error !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ error is "Bad Request"': (r) => {
        try {
          return r.json().error === 'Bad Request';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (userId negative): ALL CHECKS PASSED');
    } else {
      console.log('❌ Unhappy Path (userId negative): SOME CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Invalid email format', () => {
    const uniqueUserId = Date.now() + 5000; // Unique userId

    const payload = JSON.stringify({
      userId: uniqueUserId,
      userEmail: 'invalid-email-format'  // Missing @ and domain
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log('\n📝 Testing Unhappy Path: Invalid email format');
    const res = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const checks = check(res, {
      '✓ status is 400 Bad Request': (r) => r.status === 400,
      '✓ response has error field': (r) => {
        try {
          return r.json().error !== undefined;
        } catch (e) {
          return false;
        }
      },
      '✓ error is "Bad Request"': (r) => {
        try {
          return r.json().error === 'Bad Request';
        } catch (e) {
          return false;
        }
      },
    });

    if (checks) {
      console.log('✅ Unhappy Path (Invalid email): ALL CHECKS PASSED');
    } else {
      console.log('❌ Unhappy Path (Invalid email): ALL CHECKS FAILED');
      console.log('Response Status:', res.status);
      console.log('Response Body:', res.body);
    }
  });

  group('Unhappy Path - Duplicate User (409 Conflict)', () => {
    // Use a unique userId and create twice
    const duplicateUserId = Date.now() + 10000;

    const payload = JSON.stringify({
      userId: duplicateUserId,
      userEmail: 'duplicate.user@example.com'
    });

    const params = {
      headers: { 'Content-Type': 'application/json' }
    };

    console.log(`\n📝 Testing Unhappy Path: Duplicate user with userId ${duplicateUserId}`);

    // First request - should succeed (201 Created)
    const res1 = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const firstChecks = check(res1, {
      '✓ first request: status is 201 Created': (r) => r.status === 201,
      '✓ first request: success is true': (r) => {
        try {
          return r.json().success === true;
        } catch (e) {
          return false;
        }
      },
    });

    if (firstChecks) {
      console.log('  ✅ First request succeeded (201 Created)');
    } else {
      console.log('  ❌ First request failed');
      console.log('  Response Status:', res1.status);
      console.log('  Response Body:', res1.body);
    }

    // Second request - should fail with 409 Conflict
    const res2 = http.post(`${BASE_URL}/api/users/default-question-bank`, payload, params);

    const secondChecks = check(res2, {
      '✓ second request: status is 409 Conflict': (r) => r.status === 409,
      '✓ second request: success is false': (r) => {
        try {
          return r.json().success === false;
        } catch (e) {
          return false;
        }
      },
      '✓ second request: message contains DUPLICATE_USER': (r) => {
        try {
          const msg = r.json().message;
          return msg && msg.includes('DUPLICATE_USER');
        } catch (e) {
          return false;
        }
      },
    });

    if (secondChecks) {
      console.log('  ✅ Second request correctly rejected (409 Conflict)');
    } else {
      console.log('  ❌ Second request did not return expected 409 Conflict');
      console.log('  Response Status:', res2.status);
      console.log('  Response Body:', res2.body);
    }
  });
}
