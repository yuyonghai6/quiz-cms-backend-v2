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
  // Prepare JSON payload
  const payload = JSON.stringify({
    name: 'John Doe',
    email: 'john@example.com',
    message: 'Hello from k6!'
  });
  
  // Set headers
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };
  
  // Make POST request
  const res = http.post('https://echo.hoppscotch.io', payload, params);
  
  // Validate response
  check(res, {
    'status is 200': (r) => r.status === 200,
    'content-type is JSON': (r) => r.headers['Content-Type'].includes('application/json'),
    'response contains our data': (r) => {
      const body = r.json();
      // The API returns data as a string in the 'data' field
      // So we need to parse it again
      const innerData = JSON.parse(body.data);
      return innerData.name === 'John Doe' && innerData.email === 'john@example.com';
    },
  });
  
  console.log('POST Response:', JSON.stringify(res.json(), null, 2));
  
  // Show the parsed inner data
  const parsedData = JSON.parse(res.json().data);
  console.log('Parsed data from response:', JSON.stringify(parsedData, null, 2));
}
