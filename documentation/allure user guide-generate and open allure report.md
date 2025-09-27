## Install Allure CLI and generate/open Allure report

### Overview
This guide shows how to install the Allure CLI, run tests to produce Allure results, generate an aggregated cross-module report, and open it locally.

- Verified Allure results directories in this repo:
  - `internal-layer/question-bank/target/allure-results`
  - `internal-layer/shared/target/allure-results`
  - `global-shared-library/target/allure-results`
- Aggregated report output location:
  - `target/allure-report`

### 1) Install Allure CLI
Allure CLI is required to generate and open reports.

```bash
npm i -g allure-commandline --registry=https://registry.npmjs.org
```

Validate installation:
```bash
allure --version
```

If you see a "not found" message, ensure your global npm bin directory is on PATH (e.g., `~/.npm-global/bin` or `~/.local/share/npm/bin`).

### 2) Run tests to produce Allure results
From the project root:
```bash
mvn clean
mvn -q -T1C -DskipITs=false test
or
mvn clean test
```

This will create `allure-results` folders in each module that has tests configured for Allure.

explain command:
1. -q

Runs Maven in quiet mode.

Suppresses most log output, only showing errors or important info.

Useful to reduce noise in CI logs.

2. -T1C

Enables parallel build execution.

-T means "threads".

1C means "one thread per CPU core".

So if your machine has 8 cores, Maven runs with 8 threads in parallel.

This speeds up builds when you have multiple modules.

3. -DskipITs=false

-D sets a system property for Maven.

skipITs usually refers to "Skip Integration Tests".

By default, some Maven plugins (like maven-failsafe-plugin) look at this property.

Here it’s set to false, which forces integration tests to run.




To confirm results were created:
```bash
find /home/joyfulday/nus-proj/quiz-cms -type d -name allure-results -maxdepth 4
```

Expected paths (examples):
- `/home/joyfulday/nus-proj/quiz-cms/internal-layer/question-bank/target/allure-results`
- `/home/joyfulday/nus-proj/quiz-cms/internal-layer/shared/target/allure-results`
- `/home/joyfulday/nus-proj/quiz-cms/global-shared-library/target/allure-results`

### 3) Generate an aggregated Allure report (Option B)
Aggregate multiple modules into a single report:
```bash
allure generate \
  -o /home/joyfulday/nus-proj/quiz-cms/target/allure-report \
  /home/joyfulday/nus-proj/quiz-cms/internal-layer/question-bank/target/allure-results \
  /home/joyfulday/nus-proj/quiz-cms/internal-layer/shared/target/allure-results \
  /home/joyfulday/nus-proj/quiz-cms/global-shared-library/target/allure-results
```

You should see:
```bash
Report successfully generated to /home/joyfulday/nus-proj/quiz-cms/target/allure-report
```

### 4) Open the report
Option 1: Start Allure's local web server (auto-opens in your default browser on some systems):
```bash
allure open /home/joyfulday/nus-proj/quiz-cms/target/allure-report
```
Example output:
```bash
Starting web server...
Server started at http://127.0.0.1:45269. Press <Ctrl+C> to exit
```

Option 2: Open the static HTML directly (no server):
```bash
xdg-open /home/joyfulday/nus-proj/quiz-cms/target/allure-report/index.html 2>/dev/null || true
```

### Per-module report (Option A)
You can also open a single module's results directly with a one-off server:
```bash
cd /home/joyfulday/nus-proj/quiz-cms/internal-layer/question-bank
allure serve target/allure-results
```

### Troubleshooting
- Allure CLI missing:
  - Install with `npm i -g allure-commandline` and ensure your npm global bin is on PATH.
- "fd: command not found":
  - Use `find` as shown above to locate `allure-results`.
- GPU-related or Chromium errors in console when running `allure open`:
  - These messages are harmless. If it bothers you, open the static HTML (`index.html`) instead of running the server.
- Different port or firewall rules:
  - The server binds to `127.0.0.1` on a random port. If needed, open the reported URL manually in your browser.

### Optional automation
- Bash script to generate an aggregated report from project root:
```bash
#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="/home/joyfulday/nus-proj/quiz-cms"
OUT_DIR="$ROOT_DIR/target/allure-report"
allure generate -o "$OUT_DIR" \
  "$ROOT_DIR/internal-layer/question-bank/target/allure-results" \
  "$ROOT_DIR/internal-layer/shared/target/allure-results" \
  "$ROOT_DIR/global-shared-library/target/allure-results"
allure open "$OUT_DIR"
```
- You can also add a Makefile target or npm script to wrap the `allure generate` + `allure open` commands.

### Notes
- This project uses Testcontainers for integration testing; no local Docker Compose MongoDB instance is required.
- Reports include Allure annotations like `@Epic`, `@Feature`, `@Story`, `@DisplayName`, `@Description` already applied in tests.


