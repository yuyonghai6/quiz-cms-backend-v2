import { check } from 'k6';
import http from 'k6/http';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
    http_req_failed: ['rate == 0.00'],
  },
};

export default function () {
  // Test your Spring Boot health endpoint
  const res = http.get('http://localhost:8765/actuator/health');
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'application is UP': (r) => {
      try {
        const body = r.json();
        return body.status === 'UP';
      } catch (e) {
        return false;
      }
    },
  });
  
  console.log('Health check response:', res.body);
}
