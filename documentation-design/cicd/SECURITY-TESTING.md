# Security Testing Setup

This project includes comprehensive security testing with **Linting** and **SAST** (Static Application Security Testing).

## 🛡️ Security Testing Pipeline

### 1. **Linting** - Code Quality & Style
Automated code quality checks to maintain clean, consistent code.

**Tools:**
- ✅ **Checkstyle** - Enforces Java coding standards (Google Style)
- ✅ **SpotBugs** - Detects potential bugs and code smells
- ✅ **PMD** - Identifies code quality issues and duplications

**Run Locally:**
```bash
# Run all linting checks
mvn checkstyle:check pmd:check spotbugs:check

# View reports
open target/site/checkstyle.html
open target/site/pmd.html
open target/site/spotbugs.html
```

### 2. **SAST** - Static Application Security Testing
Static code analysis to find security vulnerabilities before runtime.

**Tools:**
- ✅ **SonarCloud** - Comprehensive code quality & security analysis
- ✅ **OWASP Dependency-Check** - Scans dependencies for known vulnerabilities (CVEs)
- ✅ **CodeQL** - GitHub's advanced semantic code analysis

**Run Locally:**
```bash
# OWASP Dependency-Check
mvn dependency-check:check
open target/dependency-check-report.html

# SonarCloud (requires SONAR_TOKEN)
mvn sonar:sonar \
  -Dsonar.projectKey=yuyonghai6_quiz-cms-backend-v2 \
  -Dsonar.organization=yuyonghai6 \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.login=$SONAR_TOKEN
```

---

## 📋 GitHub Actions Workflow

### Main CI/CD Pipeline (`backend-ci.yml`)
Runs on **every push** to `main` and **pull requests**:

1. **Linting** → Code style & quality checks (Fail Fast ⚡)
2. **Build & Test** → Maven build + unit tests
3. **SAST** → Security vulnerability scanning (SpotBugs, OWASP, SonarCloud)
4. **CodeQL** → Advanced static analysis
5. **Security Gate** → Evaluates all checks before deployment
6. **Deploy** → Only if security gate passes (main branch only)

---

## 🔧 Setup Instructions

### 1. SonarCloud Setup (Required for SAST)

1. **Go to [sonarcloud.io](https://sonarcloud.io)**
2. **Sign in with GitHub**
3. **Create a new organization** (or use existing)
4. **Import your repository**: `yuyonghai6/quiz-cms-backend-v2`
5. **Generate a token**:
   - Go to: Account → Security → Generate Tokens
   - Name: `GitHub Actions`
   - Copy the token

6. **Add token to GitHub Secrets**:
   - Go to: Repository → Settings → Secrets and variables → Actions
   - Click "New repository secret"
   - Name: `SONAR_TOKEN`
   - Value: [paste your token]

### 2. Enable CodeQL (Already configured)

CodeQL is automatically enabled in the workflow. No additional setup needed!

### 3. Configure OWASP Dependency-Check Suppressions

Edit `dependency-check-suppressions.xml` to suppress false positives:

```xml
<suppress>
   <notes>False positive - does not affect our usage</notes>
   <packageUrl regex="true">^pkg:maven/org\.example/.*$</packageUrl>
   <cve>CVE-2024-12345</cve>
</suppress>
```

---

## 📊 Security Reports & Dashboards

### SonarCloud Dashboard
- URL: https://sonarcloud.io/project/overview?id=yuyonghai6_quiz-cms-backend-v2
- **Metrics**: Code coverage, bugs, vulnerabilities, code smells, security hotspots

### CodeQL Results
- GitHub → Security → Code scanning alerts
- **Detects**: SQL injection, XSS, path traversal, etc.

### OWASP Dependency-Check
- Download from: GitHub Actions → Artifacts → `dependency-check-report`
- **Shows**: CVE vulnerabilities in dependencies with CVSS scores

---

## 🚨 Security Thresholds

| Tool | Threshold | Action |
|------|-----------|--------|
| OWASP Dependency-Check | CVSS ≥ 7.0 | Fails build |
| SpotBugs | Low | Warning only |
| PMD | All violations | Warning only |
| Checkstyle | All violations | Warning only |

To make tests stricter, edit `pom.xml`:

```xml
<!-- Make linting fail the build -->
<configuration>
  <failOnError>true</failOnError>
  <failOnViolation>true</failOnViolation>
</configuration>
```

---

## 🔍 Common Issues & Solutions

### Issue: SonarCloud token missing
**Solution**: Add `SONAR_TOKEN` to GitHub Secrets (see Setup Instructions)

### Issue: OWASP Dependency-Check finds false positives
**Solution**: Add suppressions to `dependency-check-suppressions.xml`

### Issue: Build time too long
**Solution**:
- Run security scans only on main branch (skip on PRs)
- Cache dependencies: Already enabled with `cache: 'maven'`
- Adjust security tool thresholds to reduce scan time

---

## 📈 Continuous Improvement

**Next Steps:**
1. ✅ Review and fix issues found by tools
2. ✅ Gradually increase security thresholds
3. ✅ Add custom Checkstyle rules
4. ✅ Configure security headers in Spring Boot
5. ✅ Set up SonarQube Quality Gates

---

## 📚 Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [SonarCloud Documentation](https://docs.sonarcloud.io/)
- [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/)
- [CodeQL Documentation](https://codeql.github.com/docs/)
- [Checkstyle Documentation](https://checkstyle.sourceforge.io/)
- [PMD Documentation](https://pmd.github.io/)
- [SpotBugs Documentation](https://spotbugs.github.io/)
