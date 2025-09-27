# JaCoCo Test Coverage User Guide

This guide explains how to generate, view, and interpret JaCoCo test coverage reports in this Maven multi-module Spring Boot project.

## Quick Start

### Generate Coverage Reports
```bash
# Generate coverage reports for all modules
mvn clean test

# Generate coverage for specific modules only
mvn clean test -pl global-shared-library,internal-layer/shared
```

### View Coverage Reports
After running tests, open these HTML files in your browser:
- **Global Shared Library**: `global-shared-library/target/site/jacoco/index.html`
- **Internal Layer Shared**: `internal-layer/shared/target/site/jacoco/index.html`

## Available Commands

### Basic Coverage Generation
```bash
# Clean build and generate coverage reports
mvn clean test

# Generate coverage without cleaning (faster for iterative development)
mvn test
```

### Coverage Validation
```bash
# Check if coverage meets the 70% threshold (fails build if below)
mvn jacoco:check

# Check coverage for specific modules
mvn jacoco:check -pl global-shared-library,internal-layer/shared

# Run full verification (includes coverage check)
mvn verify
```

### Advanced Coverage Commands
```bash
# Generate coverage reports without running tests (if tests already ran)
mvn jacoco:report

# Generate coverage for integration tests (when available)
mvn verify

# Clean old coverage data before new test run
rm -rf target/jacoco.exec target/site/jacoco/
```

## Understanding Coverage Reports

### HTML Report Structure
When you open `target/site/jacoco/index.html`, you'll see:

1. **Overview Page**: Summary of all packages with coverage percentages
2. **Package Drill-down**: Click package names to see class-level coverage
3. **Class Drill-down**: Click class names to see method and line coverage
4. **Source View**: See exactly which lines are covered (green) or missed (red)

### Coverage Metrics Explained
- **Instructions**: JVM bytecode instructions covered/total
- **Branches**: Conditional branches (if/else, switch) covered/total
- **Lines**: Source code lines covered/total
- **Methods**: Methods invoked during tests/total methods
- **Classes**: Classes with at least one method executed/total classes

### Color Coding
- 🟢 **Green**: Fully covered code
- 🔴 **Red**: Not covered code
- 🟡 **Yellow**: Partially covered branches (some conditions not tested)

## Coverage Thresholds

### Current Project Settings
- **Minimum Line Coverage**: 70%
- **Enforcement**: Build fails if coverage drops below threshold
- **Scope**: Applied per module (bundle)

### Coverage Status by Module
```
📊 Module Coverage Status:
├── global-shared-library: 64% (⚠️ Below threshold)
├── internal-layer/shared: Active coverage tracking
├── orchestration-layer: No tests currently
├── question-bank: No tests currently
└── quiz-session: No tests currently
```

## Report Locations

### Generated Files Structure
```
module-name/target/
├── jacoco.exec                    # Execution data (binary)
└── site/jacoco/
    ├── index.html                 # Main HTML report
    ├── jacoco.xml                 # XML format (for CI/CD)
    ├── jacoco.csv                 # CSV format (for analysis)
    ├── jacoco-sessions.html       # Session information
    └── com.quizfun.package/       # Package-specific reports
        ├── index.html
        └── ClassName.html
```

### Direct File Access
Open coverage reports directly:
```bash
# Global Shared Library
open global-shared-library/target/site/jacoco/index.html

# Internal Layer Shared
open internal-layer/shared/target/site/jacoco/index.html

# Or use file:// URLs in browser
file:///absolute/path/to/project/global-shared-library/target/site/jacoco/index.html
```

## Troubleshooting

### No Coverage Data Generated
**Problem**: "Skipping JaCoCo execution due to missing execution data file"

**Solutions**:
1. Ensure tests are actually running: `mvn test -X` (debug mode)
2. Check if module has tests: `find . -name "*Test.java"`
3. Verify JaCoCo plugin is enabled in module's `pom.xml`

### Coverage Reports Not Updated
**Problem**: Old coverage data showing after code changes

**Solutions**:
```bash
# Clean before generating new reports
mvn clean test

# Or manually remove old data
rm -rf */target/jacoco.exec */target/site/jacoco/
```

### Build Fails Due to Coverage
**Problem**: "Coverage checks have not been met"

**Solutions**:
1. **Add more tests** to increase coverage
2. **Skip coverage check temporarily**:
   ```bash
   mvn test -Djacoco.skip=true
   ```
3. **Lower threshold temporarily** (not recommended for production)

### Multiple JaCoCo Plugin Declaration Warning
**Problem**: "'build.plugins.plugin.(groupId:artifactId)' must be unique"

**Status**: Known issue in orchestration-layer module, doesn't affect functionality

## Integration with CI/CD

### XML Reports for Build Systems
JaCoCo generates XML reports at `target/site/jacoco/jacoco.xml` for integration with:
- SonarQube
- CodeCov
- GitHub Actions
- Jenkins

### CSV Reports for Analysis
CSV reports at `target/site/jacoco/jacoco.csv` can be imported into:
- Excel/Google Sheets
- Data analysis tools
- Custom reporting scripts

## Best Practices

### Development Workflow
1. **Write tests first** (TDD approach)
2. **Run coverage frequently**: `mvn test`
3. **Aim for meaningful coverage**, not just high percentages
4. **Focus on critical business logic** first
5. **Review uncovered branches** in HTML reports

### Coverage Targets
- **New code**: Aim for 80%+ coverage
- **Legacy code**: Gradual improvement, 70% minimum
- **Critical paths**: 90%+ coverage preferred
- **Simple getters/setters**: Lower priority

### Monitoring Coverage Trends
```bash
# Generate trend reports by comparing coverage over time
mvn clean test && cp target/site/jacoco/jacoco.xml coverage-$(date +%Y%m%d).xml
```

## Advanced Configuration

### Module-Specific Coverage Settings
To customize coverage for specific modules, add to module's `pom.xml`:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum> <!-- 80% for this module -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

### Excluding Classes from Coverage
```xml
<configuration>
    <excludes>
        <exclude>**/*Config.class</exclude>
        <exclude>**/*Application.class</exclude>
    </excludes>
</configuration>
```

## Summary

JaCoCo provides comprehensive test coverage analysis for this Maven multi-module project. Use `mvn clean test` to generate reports, then open the HTML files in your browser to explore coverage details. The 70% line coverage threshold ensures code quality while providing flexibility for different module needs.

For questions or issues with coverage reporting, refer to the troubleshooting section or check the Maven build logs for specific error messages.