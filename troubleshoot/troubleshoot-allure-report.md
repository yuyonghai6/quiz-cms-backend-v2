### Allure Report Troubleshooting (quiz-cms)

This project is wired for Allure via JUnit5. If "the report doesn't generate" or "nothing opens", follow the steps below.

### What I verified and did to fix it
- Confirmed module setup in `internal-layer/question-bank/pom.xml`:
  - Dependency: `io.qameta.allure:allure-junit5:2.24.0`
  - Plugin: `io.qameta.allure:allure-maven:2.12.0`
- Regenerated fresh test results:
  - Clean old results: `rm -rf **/target/allure-results **/target/allure-report`
  - Run tests: `mvn -DskipTests=false test`
- Verified results were produced:
  - `internal-layer/question-bank/target/allure-results/` (JSON files present)
- Served the report from the module that produced results:
  - `cd internal-layer/question-bank`
  - `mvn io.qameta.allure:allure-maven:2.12.0:serve`
  - This starts a local server and opens the Allure dashboard in your browser.

If you prefer a static report (no server):
- `cd internal-layer/question-bank`
- `mvn io.qameta.allure:allure-maven:2.12.0:report`
- Then open: `internal-layer/question-bank/target/allure-report/allure-maven.html`

### Quick recipe (module-scoped)
```bash
# from repo root
mvn -DskipTests=false -pl internal-layer/question-bank -am test
mvn -pl internal-layer/question-bank io.qameta.allure:allure-maven:2.12.0:serve
```

### Common pitfalls and fixes
- No results in allure-results
  - Ensure tests actually ran and were not filtered/skipped.
  - Make sure JUnit5 is picked up by Surefire (Spring Boot starter test already configures this).
  - Allure annotations (`@Epic`, `@Feature`, `@Story`, `@Description`, `@DisplayName`) are optional for generation, but present in this repo for nicer reports.

- Running from the wrong module
  - In a multi-module project, run Allure from the module that produced results (here: `internal-layer/question-bank`). Use `-pl` to target it.

- Using root-level `mvn allure:serve` and nothing opens
  - If other modules produce no results, the root serve may find nothing. Target the module: `mvn -pl internal-layer/question-bank io.qameta.allure:allure-maven:2.12.0:serve`.

- Headless/CI environment
  - Use static generation: `mvn -pl internal-layer/question-bank io.qameta.allure:allure-maven:2.12.0:report` and archive `internal-layer/question-bank/target/allure-report/`.

- Prefer Allure CLI
  - If installed: `allure serve internal-layer/question-bank/target/allure-results`
  - Or: `allure generate internal-layer/question-bank/target/allure-results -o internal-layer/question-bank/target/allure-report --clean`

### Verified paths in this repo
- Results: `internal-layer/question-bank/target/allure-results/`
- Report (static): `internal-layer/question-bank/target/allure-report/allure-maven.html`

### References
- Allure Maven Plugin: io.qameta.allure:allure-maven:2.12.0
- Allure JUnit5: io.qameta.allure:allure-junit5:2.24.0
- See also: `CLAUDE.md` → Allure Reporting section.


