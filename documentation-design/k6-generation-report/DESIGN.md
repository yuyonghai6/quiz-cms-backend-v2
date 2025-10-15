# K6 HTML Report Generation - Design Document

## Document Information
- **Version**: 1.0
- **Date**: 2025-10-11
- **Status**: Proposed
- **Author**: System Architecture Team

---

## 1. Overview

### 1.1 Background
The quiz CMS API system currently uses k6 for API system testing, providing console-based output that displays test results in text format. While functional, this approach has limitations:
- Test results are ephemeral and difficult to share
- No visual representation of performance metrics
- Limited historical tracking capabilities
- Difficult to analyze trends across multiple test runs
- Not suitable for stakeholder presentations

### 1.2 Objectives
Implement HTML report generation for k6 tests to provide:
- **Visual Reporting**: Charts and graphs for performance metrics
- **Shareability**: Standalone HTML files that can be distributed to team members
- **Persistence**: Saved reports for historical comparison
- **Professional Presentation**: Polished reports suitable for stakeholders
- **Dual Output**: Both human-readable (HTML) and machine-readable (JSON) formats

### 1.3 Scope
**In Scope:**
- HTML report generation for existing k6 test suite
- Integration with `test-upsert-question-with-taxonomy.js`
- Configuration of report styling and branding
- Documentation and usage guidelines

**Out of Scope:**
- CI/CD pipeline integration (future enhancement)
- Real-time dashboard monitoring (separate effort)
- Custom report templating engine
- Multi-test aggregation reports

---

## 2. Solution Architecture

### 2.1 High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                    K6 Test Execution                         │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  test-upsert-question-with-taxonomy.js              │   │
│  │                                                       │   │
│  │  1. Import k6-reporter library (CDN)                │   │
│  │  2. Execute test scenarios (42 checks)              │   │
│  │  3. Collect metrics and results                     │   │
│  │  4. Call handleSummary(data) with results           │   │
│  └─────────────────┬───────────────────────────────────┘   │
│                    │                                         │
│                    ▼                                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          handleSummary() Function                    │   │
│  │                                                       │   │
│  │  • Receives test summary data                       │   │
│  │  • Calls htmlReport(data, options)                  │   │
│  │  • Returns file map for k6 to write                 │   │
│  └─────────────────┬───────────────────────────────────┘   │
│                    │                                         │
└────────────────────┼─────────────────────────────────────────┘
                     │
                     ▼
      ┌──────────────┴──────────────┐
      │                              │
      ▼                              ▼
┌──────────────┐            ┌──────────────┐
│ summary-     │            │ summary-     │
│ report.html  │            │ data.json    │
└──────────────┘            └──────────────┘
   (Human)                    (Machine)
```

### 2.2 Component Description

#### 2.2.1 k6-reporter Library
- **Source**: https://github.com/benc-uk/k6-reporter
- **Version**: Latest (CDN-based)
- **Import Method**: ES6 import from CDN
- **License**: MIT (open source)

**Key Features:**
- Zero installation required (CDN import)
- Lightweight bundle (~100KB)
- Multiple theme support
- Customizable titles and styling
- Charts powered by Chart.js

#### 2.2.2 Integration Points

**Import Statement:**
```javascript
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js';
```

**Export Function:**
```javascript
export function handleSummary(data) {
  return {
    'summary-report.html': htmlReport(data, {
      title: 'Upsert Question with Taxonomy - API System Test'
    }),
    'summary-data.json': JSON.stringify(data, null, 2),
  };
}
```

---

## 3. Report Structure and Features

### 3.1 HTML Report Sections

#### 3.1.1 Overview Section
- **Test Summary**: Total checks, pass/fail rate, duration
- **Threshold Status**: Visual indicators for threshold compliance
- **Key Metrics Cards**: HTTP requests, iterations, data transfer

#### 3.1.2 Checks & Groups
- **Hierarchical Display**: Test groups with nested checks
- **Pass/Fail Indicators**: Green checkmarks and red X's
- **Check Details**: Individual check names and results
- **Success Percentages**: Per-check and per-group success rates

#### 3.1.3 HTTP Metrics
- **Request Duration**: Min, max, avg, percentiles (p90, p95)
- **Request Rate**: Requests per second
- **Failure Rate**: Percentage of failed requests
- **Response Codes**: Distribution of HTTP status codes

#### 3.1.4 Performance Charts
- **Timeline Graphs**: Request duration over time
- **Histogram**: Distribution of response times
- **Throughput**: Requests per second trend

#### 3.1.5 Iteration Metrics
- **Duration Statistics**: Average, min, max iteration time
- **Success Rate**: Percentage of successful iterations
- **VU Information**: Virtual user configuration

### 3.2 JSON Output Structure

The JSON file contains raw test data for:
- CI/CD pipeline integration
- Custom report generation
- Historical trend analysis
- Automated alerting systems

**Key JSON Sections:**
```json
{
  "metrics": { /* All metric data */ },
  "root_group": { /* Test hierarchy */ },
  "options": { /* Test configuration */ },
  "state": { /* Test execution state */ }
}
```

---

## 4. Implementation Approach

### 4.1 Code Modifications

**File**: `api-system-test/test-upsert-question-with-taxonomy.js`

**Changes Required:**

1. **Add Import (Top of File)**
```javascript
// Existing imports
import { check, group } from 'k6';
import http from 'k6/http';

// NEW: Add k6-reporter import
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/latest/dist/bundle.js';
```

2. **Add Export Function (End of File)**
```javascript
// NEW: Export handleSummary function for report generation
export function handleSummary(data) {
  return {
    'summary-report.html': htmlReport(data, {
      title: 'Upsert Question with Taxonomy - API System Test',
      debug: false  // Set to true for troubleshooting
    }),
    'summary-data.json': JSON.stringify(data, null, 2),
  };
}
```

### 4.2 Configuration Options

Available customization options:

```javascript
{
  // Report title (displayed at top of HTML)
  title: 'Custom Test Report Title',

  // Theme selection
  theme: 'default',  // Options: 'default', 'classic', 'bootstrap'

  // Debug mode (includes raw JSON in HTML)
  debug: false,

  // Custom CSS (advanced usage)
  customStyles: '/* CSS here */'
}
```

### 4.3 No Breaking Changes

This implementation is **fully backward compatible**:
- Console output remains unchanged
- All existing functionality preserved
- Test execution behavior identical
- No modifications to test logic required

---

## 5. Technical Considerations

### 5.1 Security

**CDN Dependency:**
- Library loaded from GitHub CDN
- Using `/latest/` path for automatic updates
- **Risk**: CDN availability dependency
- **Mitigation**: Can download bundle.js locally if needed

**Data Privacy:**
- All processing happens locally (no external calls)
- Reports generated on local filesystem
- No telemetry or data transmission
- Safe for confidential test data

### 5.2 Performance Impact

**Runtime Overhead:**
- Import: ~100-200ms (one-time, CDN fetch with caching)
- Report Generation: ~50-100ms (post-test processing)
- Total Impact: <1% of test execution time

**File Size:**
- HTML Report: ~500KB - 2MB (depending on test complexity)
- JSON Summary: ~50-200KB
- Disk Space: Minimal impact

### 5.3 Browser Compatibility

Generated HTML reports support:
- Chrome/Edge (v90+)
- Firefox (v88+)
- Safari (v14+)
- Opera (v76+)

No IE11 support (Chart.js dependency).

### 5.4 Maintenance

**Library Updates:**
- Auto-updated via CDN `/latest/` path
- Manual version pinning available if needed
- Active project with regular maintenance
- Community support via GitHub issues

---

## 6. Alternative Solutions Comparison

### 6.1 Option 1: k6-reporter (Chosen Solution)

**Pros:**
- Zero installation (CDN-based)
- Simple integration (2 code additions)
- Beautiful default styling
- Active maintenance
- Good documentation

**Cons:**
- External CDN dependency
- Limited customization vs. custom solution

### 6.2 Option 2: k6-html-reporter (npm package)

**Pros:**
- npm package (no CDN dependency)
- Established package (4+ years)

**Cons:**
- Last updated 4 years ago (potentially stale)
- Requires npm installation step
- Less modern UI design
- Lower community activity

### 6.3 Option 3: Custom HTML Report Generator

**Pros:**
- Full control over design
- No external dependencies
- Customized to exact needs

**Cons:**
- Significant development time (~2-3 days)
- Ongoing maintenance burden
- Requires charting library integration
- Reinventing the wheel

### 6.4 Option 4: K6 Cloud / Grafana Integration

**Pros:**
- Enterprise-grade reporting
- Real-time dashboards
- Historical trend analysis
- Team collaboration features

**Cons:**
- Requires infrastructure setup
- Additional cost (K6 Cloud)
- Complexity overhead for simple use case
- Network dependency

**Decision Rationale:**
Option 1 (k6-reporter) chosen for optimal balance of simplicity, functionality, and maintenance overhead.

---

## 7. Testing Strategy

### 7.1 Validation Steps

1. **Smoke Test**: Verify report generation doesn't break existing tests
2. **Content Validation**: Confirm all 42 checks appear in HTML report
3. **Visual Inspection**: Review charts and metrics for accuracy
4. **Cross-Browser Testing**: Open report in Chrome, Firefox, Safari
5. **JSON Validation**: Ensure JSON output is valid and complete

### 7.2 Success Criteria

- ✅ HTML report generates without errors
- ✅ All test results accurately reflected
- ✅ Charts render correctly
- ✅ Pass/fail indicators match console output
- ✅ Report opens in all major browsers
- ✅ File size < 5MB
- ✅ Generation time < 1 second

---

## 8. Documentation Requirements

### 8.1 README Updates

Add section to project README:
```markdown
## Running Tests with HTML Reports

Execute k6 tests to generate HTML reports:

\`\`\`bash
k6 run api-system-test/test-upsert-question-with-taxonomy.js
\`\`\`

Reports generated:
- `summary-report.html` - Visual HTML report
- `summary-data.json` - Raw JSON data

Open the report:
\`\`\`bash
open summary-report.html
\`\`\`
```

### 8.2 Troubleshooting Guide

Update `api-system-test/troubleshoot-tips-upsert.md`:
- Add section on report generation
- Document common issues (CDN timeout, file permissions)
- Provide debugging steps

---

## 9. Future Enhancements

### 9.1 Phase 2 Considerations

1. **Multi-Test Aggregation**: Combine reports from multiple test files
2. **Historical Comparison**: Side-by-side comparison of test runs
3. **CI/CD Integration**: Automatic report archiving and publishing
4. **Custom Thememing**: Quiz CMS branded report styling
5. **Email Distribution**: Automated report delivery to stakeholders

### 9.2 Advanced Features

- **Trend Analysis**: Performance degradation detection
- **SLA Monitoring**: Automatic threshold alerting
- **Report Archiving**: Timestamped report storage
- **Dashboard Integration**: Embed reports in team dashboard

---

## 10. References

### 10.1 External Resources

- **k6-reporter GitHub**: https://github.com/benc-uk/k6-reporter
- **k6 Documentation**: https://grafana.com/docs/k6/latest/results-output/
- **Chart.js**: https://www.chartjs.org/

### 10.2 Internal Documentation

- Current Test Suite: `api-system-test/test-upsert-question-with-taxonomy.js`
- Troubleshooting Guide: `api-system-test/troubleshoot-tips-upsert.md`
- Project README: `README.md`

---

## 11. Appendix

### 11.1 Sample Report Screenshot Description

A typical HTML report includes:
- Header with test title and timestamp
- Summary cards showing 42/42 checks passed (100%)
- Threshold status with green checkmarks
- Grouped test results (Happy Path, Unhappy Path)
- HTTP metrics table with min/max/avg/p90/p95 values
- Performance charts showing request duration timeline
- Footer with k6 version and generation timestamp

### 11.2 Example handleSummary Implementation

```javascript
export function handleSummary(data) {
  // Custom processing before report generation
  const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
  const filename = `report-${timestamp}.html`;

  return {
    [filename]: htmlReport(data, {
      title: `API Test Report - ${new Date().toLocaleString()}`,
      theme: 'default'
    }),
    'latest-report.html': htmlReport(data, {
      title: 'Latest Test Results'
    }),
    [`data-${timestamp}.json`]: JSON.stringify(data, null, 2),
  };
}
```

This approach generates:
- Timestamped archive reports
- Latest report (overwritten each run)
- Timestamped JSON data files

---

**Document Version**: 1.0
**Last Updated**: 2025-10-11
**Next Review**: When implementing Phase 2 enhancements
