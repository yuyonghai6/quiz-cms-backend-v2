import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
  },
};

export default function () {
  // GET request with query parameters
  const res = http.get('https://echo.hoppscotch.io?user=john&id=123&action=test');
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'query params in response': (r) => {
      const body = r.json();
      // Try 'args' instead of 'query'
      return body.args && 
             body.args.user === 'john' &&
             body.args.id === '123';
    },
  });
  
  console.log('Full response:', JSON.stringify(res.json(), null, 2));
  console.log('Query parameters echoed:', res.json().args);
}
