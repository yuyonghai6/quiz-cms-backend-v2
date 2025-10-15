# K6 HTML Report Generation - User Guide

## Document Information
- **Version**: 1.0
- **Date**: 2025-10-11
- **Audience**: Developers, QA Engineers, Technical Stakeholders
- **Related Documents**: [DESIGN.md](./DESIGN.md), [ROADMAP.md](./ROADMAP.md)

---

## Quick Start (30 Seconds)

### 1. Run Tests
```bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js
```

### 2. Open Report
```bash
# macOS
open summary-report.html

# Linux
xdg-open summary-report.html

# Windows
start summary-report.html
```

**That's it!** You now have a visual HTML report of your test results.

---

## Prerequisites

Before running tests with HTML reports, ensure:

- ✅ **K6 Installed**
  ```bash
  # macOS
  brew install k6

  # Linux (Debian/Ubuntu)
  sudo gpg -k
  sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
  echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
  sudo apt-get update
  sudo apt-get install k6

  # Windows (Chocolatey)
  choco install k6

  # Or download from: https://k6.io/docs/get-started/installation/
  ```

- ✅ **Spring Boot Application Running**
  ```bash
  mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev
  ```

- ✅ **MongoDB Running**
  - Default connection: `mongodb://localhost:27017/quizfun`
  - Verify: Check Spring Boot logs for MongoDB connection status

- ✅ **Internet Connection (First Run Only)**
  - Required to download k6-reporter library from CDN
  - Subsequent runs use cached version

---

## Understanding the Reports

### HTML Report (`summary-report.html`)

**File Size**: ~42KB
**Purpose**: Visual dashboard for humans

**Sections:**

1. **Test Overview** (Top Section)
   - Total checks: 42/42 ✅
   - Success rate: 100%
   - Test duration
   - Timestamp

2. **Checks & Groups** (Middle Section)
   - Hierarchical view of all test scenarios
   - ✅ Green checkmarks: Passing checks
   - ❌ Red X's: Failing checks (when tests fail)
   - Organized by:
     - Happy Path (Create, Update, Upsert operations)
     - Unhappy Path (Validation errors, invalid data)

3. **HTTP Metrics** (Bottom Section)
   - **Request Duration Statistics:**
     - `min`: Fastest request
     - `max`: Slowest request
     - `avg`: Average response time
     - `p90`: 90th percentile (90% of requests faster than this)
     - `p95`: 95th percentile (95% of requests faster than this)
   - **Request Rate**: Requests per second
   - **Failure Rate**: Percentage of failed requests

4. **Performance Charts**
   - Timeline graph showing response times over test duration
   - Helps identify performance spikes or degradation

### JSON Data (`summary-data.json`)

**File Size**: ~20KB
**Purpose**: Machine-readable data for automation

**Use Cases:**
- CI/CD pipeline integration
- Custom analysis scripts
- Historical trend tracking
- Automated alerting based on thresholds

**Example JSON Query:**
```bash
# Extract average response time
cat summary-data.json | jq '.metrics.http_req_duration.values.avg'

# Extract all check pass rates
cat summary-data.json | jq '.metrics.checks'

# Extract total number of HTTP requests
cat summary-data.json | jq '.metrics.http_reqs.values.count'
```

---

## Common Workflows

### Workflow 1: Daily Development Testing

**Scenario**: You've made code changes and want to verify API functionality.

**Steps:**
```bash
# 1. Start Spring Boot (if not running)
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev &

# 2. Wait for application to start (check health)
curl http://localhost:8765/actuator/health

# 3. Run tests
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# 4. Check console output for quick status
# Look for: "✓ [100%] 1 VUs ... 42/42 checks passed"

# 5. Open HTML report for detailed analysis
open summary-report.html
```

**Expected Result**: All 42 checks passing, green dashboard in HTML report.

---

### Workflow 2: Debugging Failed Tests

**Scenario**: Tests are failing and you need to identify the root cause.

**Steps:**
```bash
# 1. Run tests (will generate report even with failures)
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# 2. Open HTML report
open summary-report.html

# 3. Navigate to "Checks & Groups" section
# - Identify failing checks (marked with ❌)
# - Note which test group failed (Happy Path vs Unhappy Path)

# 4. Check Spring Boot logs for detailed errors
tail -f orchestration-layer/logs/application.log

# 5. Inspect JSON data for specific error details
cat summary-data.json | jq '.root_group.checks[] | select(.passes == 0)'
```

**Troubleshooting Tips:**
- ❌ **Status 500 errors**: Check Spring Boot application logs
- ❌ **Status 422 errors**: Business rule violations (see troubleshoot-tips-upsert.md)
- ❌ **Status 400 errors**: Invalid request data or validation failures
- ❌ **Connection errors**: Ensure Spring Boot is running on port 8765

---

### Workflow 3: Performance Analysis

**Scenario**: You want to assess API performance and identify bottlenecks.

**Steps:**
```bash
# 1. Run tests
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# 2. Open HTML report and review HTTP Metrics section
open summary-report.html

# 3. Check key performance indicators:
# - p95 < 500ms? ✅ Good performance
# - p95 > 1000ms? ⚠️ Performance issue - investigate
# - avg < 200ms? ✅ Excellent baseline

# 4. Extract performance data for tracking
jq '.metrics.http_req_duration.values' summary-data.json > perf-$(date +%Y%m%d).json

# 5. Compare with previous runs
jq '.avg' perf-20251010.json
jq '.avg' perf-20251011.json
```

**Performance Baselines:**
- **Excellent**: avg < 100ms, p95 < 300ms
- **Good**: avg < 200ms, p95 < 500ms
- **Acceptable**: avg < 300ms, p95 < 1000ms
- **Needs Investigation**: p95 > 1000ms

---

### Workflow 4: Sharing Results with Team

**Scenario**: You need to demonstrate test results to your team or stakeholders.

**For Non-Technical Stakeholders:**
```bash
# 1. Run tests
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# 2. Share HTML report via email/Slack/Teams
# File: summary-report.html (42KB - easy to attach)

# Key talking points:
# - "42 out of 42 tests passing" ✅
# - "All API endpoints validated"
# - "Performance within acceptable range (avg: XYms)"
```

**For Technical Team:**
```bash
# 1. Archive both reports with timestamp
mkdir -p test-results-$(date +%Y%m%d)
cp summary-report.html test-results-$(date +%Y%m%d)/
cp summary-data.json test-results-$(date +%Y%m%d)/

# 2. Zip and share
zip -r test-results-$(date +%Y%m%d).zip test-results-$(date +%Y%m%d)/

# 3. Share via file sharing platform
# Team can analyze both visual and raw data
```

---

## Tips and Best Practices

### 1. Clean Test Data Before Running

For consistent results, clean MongoDB test data:
```bash
# Connect to MongoDB
mongosh quizfun

# Delete test questions
db.questions.deleteMany({"metadata.created_source": "k6-functional-test"})

# Delete test relationships
db.question_taxonomy_relationships.deleteMany({})
```

### 2. Archive Important Reports

Save reports from significant test runs:
```bash
# Create timestamped archive
cp summary-report.html reports/report-$(date +%Y%m%d-%H%M%S).html
cp summary-data.json reports/data-$(date +%Y%m%d-%H%M%S).json

# Clean old archives (keep last 10)
cd reports
ls -t report-*.html | tail -n +11 | xargs rm -f
ls -t data-*.json | tail -n +11 | xargs rm -f
```

### 3. Compare Performance Across Runs

Track performance trends:
```bash
# Extract key metrics from multiple runs
for file in reports/data-*.json; do
  echo -n "$(basename $file): "
  jq '.metrics.http_req_duration.values.avg' $file
done

# Output example:
# data-20251010-143022.json: 45.23
# data-20251011-092015.json: 43.87
# data-20251011-120437.json: 46.12
```

### 4. Use Reports in Pull Request Reviews

Include test reports in PR descriptions:
```markdown
## Test Results

✅ All 42 API checks passing
📊 Report: [summary-report.html](link-to-report)

**Performance:**
- Average response time: 45ms
- p95 response time: 120ms
- All endpoints within SLA targets
```

---

## Troubleshooting

### Issue: No HTML Report Generated

**Symptoms:**
- Test runs successfully
- Only JSON file created
- Missing `summary-report.html`

**Cause**: CDN timeout or network issue preventing k6-reporter download

**Solution:**
```bash
# Option 1: Retry test (CDN may be temporarily unavailable)
k6 run api-system-test/test-upsert-question-with-taxonomy.js

# Option 2: Download library locally (permanent fix)
mkdir -p api-system-test/lib
curl -o api-system-test/lib/k6-reporter-bundle.js \
  https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js

# Then update import in test file (line 3):
# FROM: import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js';
# TO:   import { htmlReport } from './lib/k6-reporter-bundle.js';
```

---

### Issue: Report Shows Blank Page in Browser

**Symptoms:**
- HTML file exists and has content
- Browser shows blank page or security errors

**Cause**: Browser security policy blocking local file JavaScript

**Solution:**
```bash
# Option 1: Try different browser
open -a "Google Chrome" summary-report.html

# Option 2: Use local HTTP server
python3 -m http.server 8000
# Then open: http://localhost:8000/summary-report.html

# Option 3: Use Node.js http-server
npx http-server -p 8000
# Then open: http://localhost:8000/summary-report.html
```

---

### Issue: Tests Fail with Connection Errors

**Symptoms:**
- Error: "connection refused" or "ECONNREFUSED"
- Tests can't reach http://localhost:8765

**Cause**: Spring Boot application not running

**Solution:**
```bash
# 1. Check if Spring Boot is running
curl http://localhost:8765/actuator/health

# 2. If not running, start it
mvn spring-boot:run -pl orchestration-layer -Dspring-boot.run.profiles=dev

# 3. Wait for startup (look for "Started OrchestrationLayerApplication")

# 4. Verify with health check
curl http://localhost:8765/actuator/health
# Expected: {"status":"UP", ...}

# 5. Re-run tests
k6 run api-system-test/test-upsert-question-with-taxonomy.js
```

---

## Advanced Usage

### Custom Report Titles

Modify the `handleSummary()` function in test file (line 947-954):

```javascript
export function handleSummary(data) {
  return {
    'summary-report.html': htmlReport(data, {
      title: 'Sprint 23 - API Regression Tests',  // Custom title
      debug: false  // Set to true to include raw JSON in HTML
    }),
    'summary-data.json': JSON.stringify(data, null, 2),
  };
}
```

### Timestamped Reports

Automatically create timestamped archives:

```javascript
export function handleSummary(data) {
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  return {
    [`reports/report-${timestamp}.html`]: htmlReport(data, {
      title: `API Test Report - ${new Date().toLocaleString()}`
    }),
    'summary-report.html': htmlReport(data, { title: 'Latest Test Results' }),
    [`reports/data-${timestamp}.json`]: JSON.stringify(data, null, 2),
    'summary-data.json': JSON.stringify(data, null, 2),
  };
}
```

### CI/CD Integration

Example Jenkins pipeline step:

```groovy
stage('Run K6 Tests') {
  steps {
    sh 'k6 run api-system-test/test-upsert-question-with-taxonomy.js'

    // Archive reports
    archiveArtifacts artifacts: 'summary-report.html,summary-data.json',
                     fingerprint: true

    // Publish HTML report
    publishHTML([
      reportDir: '.',
      reportFiles: 'summary-report.html',
      reportName: 'K6 API Test Report'
    ])
  }
}
```

---

## FAQ

### Q: Do I need to install any dependencies?

**A:** No! k6-reporter is loaded from CDN automatically. You only need k6 installed.

---

### Q: Can I customize the report appearance?

**A:** The k6-reporter library provides several themes. You can modify the `theme` option in `handleSummary()`:
```javascript
htmlReport(data, {
  title: 'My Tests',
  theme: 'default'  // Options: 'default', 'classic', 'bootstrap'
})
```

For advanced customization, download the library locally and modify the CSS.

---

### Q: How do I integrate this with CI/CD?

**A:**
1. Run k6 tests in your CI pipeline
2. Archive both `summary-report.html` and `summary-data.json` as build artifacts
3. Use the JSON data for automated threshold checks
4. Publish the HTML report for team visibility

Example GitHub Actions:
```yaml
- name: Run K6 Tests
  run: k6 run api-system-test/test-upsert-question-with-taxonomy.js

- name: Upload Reports
  uses: actions/upload-artifact@v3
  with:
    name: k6-reports
    path: |
      summary-report.html
      summary-data.json
```

---

### Q: Can I run tests in parallel?

**A:** The current test configuration uses 1 VU (Virtual User) with 1 iteration. To run parallel tests, modify the `options` in the test file:

```javascript
export const options = {
  vus: 10,        // 10 virtual users
  duration: '30s', // Run for 30 seconds
  thresholds: {
    checks: ['rate == 1.00'],
  },
};
```

Note: This will create multiple concurrent API requests, which may require adjusting the test data strategy.

---

### Q: What browsers are supported for viewing reports?

**A:**
- ✅ Chrome/Edge 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Opera 76+
- ❌ Internet Explorer (not supported)

---

### Q: How do I compare reports from different test runs?

**A:** Use the JSON data with `jq`:

```bash
# Compare average response times
diff <(jq '.metrics.http_req_duration.values' reports/data-old.json) \
     <(jq '.metrics.http_req_duration.values' reports/data-new.json)

# Extract specific metrics
jq -s '{"old": .[0].metrics.http_req_duration.values.avg,
        "new": .[1].metrics.http_req_duration.values.avg}' \
   reports/data-old.json reports/data-new.json
```

---

## Additional Resources

- **Full Design Documentation**: [DESIGN.md](./DESIGN.md)
- **Implementation Roadmap**: [ROADMAP.md](./ROADMAP.md)
- **Troubleshooting Guide**: [../api-system-test/troubleshoot-tips-upsert.md](../../api-system-test/troubleshoot-tips-upsert.md)
- **K6 Official Docs**: https://k6.io/docs/
- **k6-reporter Library**: https://github.com/benc-uk/k6-reporter

---

**Document Version**: 1.0
**Last Updated**: 2025-10-11
**Feedback**: For issues or suggestions, update this guide or consult the development team
