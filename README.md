Sub Project of Neo and Jacelyn on quiz cms backend

## Running K6 Tests with HTML Reports

This project includes K6 API system tests with HTML report generation for visual analysis of test results.

### Prerequisites
- K6 installed (`brew install k6` on macOS, see [k6.io](https://k6.io/docs/get-started/installation/) for other platforms)
- Spring Boot application running on port 8765
- MongoDB running and accessible

### Running Tests

Execute K6 tests and generate HTML reports:

```bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js
```

### Generated Reports

After running tests, two files are generated in the project root:

- **`summary-report.html`** - Visual HTML report with:
  - Test summary and pass/fail rates
  - Performance metrics and charts
  - Detailed check results (42 checks total)
  - HTTP response time statistics
  - Suitable for sharing with team members

- **`summary-data.json`** - Raw JSON data containing:
  - All test metrics
  - Check results and groupings
  - Performance statistics
  - Useful for CI/CD integration and custom analysis

### Viewing Reports

Open the HTML report in your browser:

```bash
# macOS
open summary-report.html

# Linux
xdg-open summary-report.html

# Windows
start summary-report.html
```

### Expected Results

When all tests pass successfully:
- **Total Checks**: 42/42 ✅
- **Success Rate**: 100%
- **Test Categories**:
  - Happy Path: MCQ, Essay, True/False questions, Upsert operations
  - Unhappy Path: Validation errors, invalid data, business rule violations

### Troubleshooting

If tests fail or reports are not generated, see [api-system-test/troubleshoot-tips-upsert.md](api-system-test/troubleshoot-tips-upsert.md) for detailed troubleshooting steps.
