import { check } from 'k6';
import http from 'k6/http';

// Functional testing configuration
export const options = {
  vus: 1,              // 1 virtual user
  iterations: 1,       // Run once
  thresholds: {
    checks: ['rate == 1.00'],  // All checks must pass
  },
};

export default function () {
  // Make GET request to echo API
  const res = http.get('https://echo.hoppscotch.io');
  
  // Validate the response
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has body': (r) => r.body.length > 0,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });
  
  // Print response for inspection
  console.log('Response status:', res.status);
  console.log('Response body:', res.body);
}
