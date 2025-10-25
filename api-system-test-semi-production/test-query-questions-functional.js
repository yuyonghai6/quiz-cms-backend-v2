import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js';

export const errorRate = new Rate('errors');

// Functional (not performance): single VU, single iteration, all checks must pass
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate == 1.00'],
    http_req_failed: ['rate==0'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://139.180.135.117:8765';
const USER_ID = __ENV.USER_ID || '1760620095607';
const QUESTION_BANK_ID = __ENV.QUESTION_BANK_ID || '1760620095622000';

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
    [`${name}: has questions or empty array`]: (r) => {
      try { const b = JSON.parse(r.body); return Array.isArray(b.questions); } catch { return false; }
    },
    [`${name}: has pagination`]: (r) => {
      try { const b = JSON.parse(r.body); return typeof b.pagination === 'object'; } catch { return false; }
    },
  });

  errorRate.add(!ok);
}

export function handleSummary(data) {
  const scriptPath = __ENV.K6_SCRIPT_PATH || 'api-system-test/test-query-questions-functional.js';
  const scriptName = scriptPath.split('/').pop().replace('.js', '');
  const reportDir = `api-system-test/reports/${scriptName}`;

  return {
    [`${reportDir}/summary-report.html`]: htmlReport(data, { title: 'K6 Functional - Query Questions' }),
    [`${reportDir}/summary-data.json`]: JSON.stringify(data, null, 2),
  };
}
