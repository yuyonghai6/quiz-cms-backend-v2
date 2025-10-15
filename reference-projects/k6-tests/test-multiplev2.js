import { check, group, sleep } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
  },
};

export default function () {
  // Test 1: Simple GET
  group('GET request', () => {
    const res = http.get('https://echo.hoppscotch.io');
    check(res, {
      'GET status is 200': (r) => r.status === 200,
    });
    console.log('✓ GET test passed');
  });
  
  sleep(1);  // Wait 1 second between tests
  
  // Test 2: POST with JSON
  group('POST request', () => {
    const payload = JSON.stringify({
      test: 'data',
      timestamp: Date.now()
    });
    
    const res = http.post('https://echo.hoppscotch.io', payload, {
      headers: { 'Content-Type': 'application/json' },
    });
    
    check(res, {
      'POST status is 200': (r) => r.status === 200,
      'POST echoed data': (r) => {
        const body = r.json();
        // Parse the 'data' field which contains our JSON as a string
        const innerData = JSON.parse(body.data);
        return innerData.test === 'data';
      },
    });
    console.log('✓ POST test passed');
  });
  
  sleep(1);
  
  // Test 3: Headers test
  group('Custom headers', () => {
    const res = http.get('https://echo.hoppscotch.io', {
      headers: {
        'X-Custom-Header': 'my-value',
        'User-Agent': 'k6-test-agent',
      },
    });
    
    check(res, {
      'Custom header echoed': (r) => {
        const body = r.json();
        return body.headers && 
               body.headers['x-custom-header'] === 'my-value';
      },
    });
    console.log('✓ Headers test passed');
  });
}
