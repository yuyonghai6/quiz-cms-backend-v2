/*
Run this k6 test with the following command:

k6 run \
  -e BASE_URL=http://localhost:8765 \
  -e USER_ID=1760085803933 \
  -e QUESTION_BANK_ID=1760085804015000 \
  -e K6_SCRIPT_PATH=performance-api-system-test/test-query-questions-perf.js \
  performance-api-system-test/test-query-questions-perf.js
*/

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js';

export const errorRate = new Rate('errors');
export const queryDuration = new Trend('query_duration');

export const options = {
  stages: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    errors: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8765';
const USER_ID = __ENV.USER_ID || '1760085803933';
const QUESTION_BANK_ID = __ENV.QUESTION_BANK_ID || '1760085804015000';

export default function () {
  scenario('basic_query', `page=0&size=20`);
  scenario('category_filter', `categories=Math&page=0&size=20`);
  scenario('text_search', `searchText=equation&page=0&size=20`);
  scenario('combined_filters', `categories=Math&tags=algebra&searchText=solve&page=0&size=10`);
  sleep(1);
}

function scenario(name, qs) {
  const url = `${BASE_URL}/api/v1/users/${USER_ID}/question-banks/${QUESTION_BANK_ID}/questions?${qs}`;
  const res = http.get(url, { tags: { scenario: name } });
  const ok = check(res, {
    [`${name}: status 200`]: (r) => r.status === 200,
  });
  errorRate.add(!ok);
  queryDuration.add(res.timings.duration);
}

export function handleSummary(data) {
  const scriptPath = __ENV.K6_SCRIPT_PATH || 'performance-api-system-test/test-query-questions-perf.js';
  const scriptName = scriptPath.split('/').pop().replace('.js', '');
  const reportDir = `performance-api-system-test/reports/${scriptName}`;

  return {
    [`${reportDir}/summary-report.html`]: htmlReport(data, {
      title: 'Query Questions API - Performance Test Report',
    }),
    [`${reportDir}/summary-data.json`]: JSON.stringify(data, null, 2),
  };
}
