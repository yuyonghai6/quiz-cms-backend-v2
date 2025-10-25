
# Design: Dockerizing the Modular Monolith with Dev Profile (Spring Boot)

## 1) Objective

Provide an immediately actionable, secure, and fast Docker build for this Maven multi-module Spring Boot app. The image should default to the dev profile and be trivially overridable at runtime, with clear guidance for connecting to MongoDB from inside a container on Linux.

## 2) Core principles

- Efficiency and cache reuse: Separate dependency resolution from source compilation for faster rebuilds.
- Minimal, secure runtime: Use JRE base for the final image, JDK only in the builder.
- Runtime configurability: Default to dev, support overrides via environment variables.
- Clarity and maintainability: Multi-stage build with small, readable steps; optional layered-JAR optimization.

## 3) Proposed Dockerfile (root of repo)

Notes specific to this repo:
- Main runnable module: `orchestration-layer` (produces the runnable Spring Boot JAR)
- Java 21 and Spring Boot 3.5.6 (as defined in the root `pom.xml`)
- Tests use Testcontainers; we skip tests in the image build path

```dockerfile
# --- Stage 1: builder ---
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1) Copy POMs to cache dependencies early (multi-module aware)
COPY pom.xml ./
COPY orchestration-layer/pom.xml ./orchestration-layer/
COPY internal-layer/pom.xml ./internal-layer/
COPY internal-layer/shared/pom.xml ./internal-layer/shared/
COPY internal-layer/question-bank/pom.xml ./internal-layer/question-bank/
COPY internal-layer/question-bank-query/pom.xml ./internal-layer/question-bank-query/
COPY global-shared-library/pom.xml ./global-shared-library/
COPY external-service-proxy/pom.xml ./external-service-proxy/

# 2) Warm Maven cache (fast rebuilds when only source changes)
RUN mvn -q -T1C dependency:go-offline

# 3) Copy sources and build (skip tests for image build path)
COPY . .
RUN mvn -q -T1C clean install -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Defaults for dev. All can be overridden at 'docker run' time.
ENV SPRING_PROFILES_ACTIVE=dev \
		SERVER_PORT=8765

# Copy main application JAR
COPY --from=builder /app/orchestration-layer/target/orchestration-layer-*.jar /app/application.jar

# Expose the default server port
EXPOSE 8765

# If layering is enabled (see optional section below), you can use a classpath-based startup;
# here we keep a simple JAR run which honors the env vars above.
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
```

### Optional: Layered JAR optimization (even faster class-change rebuilds)

Spring Boot supports layered JARs that separate dependencies from application classes/resources. If your `spring-boot-maven-plugin` produces layered JARs (default in modern Boot), you can extract layers and copy only the changed parts in the final stage:

```dockerfile
# Replace the runtime COPY and ENTRYPOINT with:
COPY --from=builder /app/orchestration-layer/target/orchestration-layer-*.jar /app/app.jar
RUN java -Djarmode=layertools -jar /app/app.jar extract

# Entrypoint using layers
ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]
```

If you adopt this, prefer copying the `dependencies/` layer once and the `application/` layer on each source change to maximize cache reuse. This is an optimization and not required to ship.

## 4) .dockerignore (strongly recommended)

Reduce build context size and speed up builds by adding a `.dockerignore` beside the `Dockerfile`:

```
.git
**/target
**/.idea
**/.project
**/.classpath
**/.settings
**/.DS_Store
**/node_modules
**/.allure
**/allure-results
**/allure-report
```

## 5) Build and run

Run these from the repository root.

### Build

```bash
docker build -t quiz-cms-api:dev .
```

### Run (default dev profile)

If your MongoDB is on your host at 127.0.0.1:27017, containers can’t reach host “localhost” directly. Use `host.docker.internal` (with an extra flag on Linux) and pass the connection string:

```bash
docker run --rm \
	--add-host=host.docker.internal:host-gateway \
	-e SPRING_PROFILES_ACTIVE=dev \
	-e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/quizfun \
	-p 8765:8765 \
	--name quiz-cms-api \
	quiz-cms-api:dev
```

### Override profile or port

```bash
# prod profile
docker run --rm -p 8765:8765 -e SPRING_PROFILES_ACTIVE=prod quiz-cms-api:dev

# change server port
docker run --rm -p 8080:8080 -e SERVER_PORT=8080 quiz-cms-api:dev
```

Environment variables map to Spring names by convention:
- `SPRING_PROFILES_ACTIVE` → active profile
- `SERVER_PORT` → `server.port`
- `SPRING_DATA_MONGODB_URI` → `spring.data.mongodb.uri`

## 6) Makefile targets (optional)

Without changing existing workflows, you can add these targets to `Makefile` to standardize local Docker usage.

```make
.PHONY: docker-build docker-run-dev docker-run-prod docker-stop

docker-build:
	docker build -t quiz-cms-api:dev .

docker-run-dev:
	docker run --rm \
	  --add-host=host.docker.internal:host-gateway \
	  -e SPRING_PROFILES_ACTIVE=dev \
	  -e SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/quizfun \
	  -p 8765:8765 \
	  --name quiz-cms-api \
	  quiz-cms-api:dev

docker-run-prod:
	docker run --rm -p 8765:8765 -e SPRING_PROFILES_ACTIVE=prod --name quiz-cms-api quiz-cms-api:dev

docker-stop:
	docker rm -f quiz-cms-api || true
```

These are suggestions; adopt them if they fit your workflow.

## 7) Verification checklist

- Image builds successfully and is small (JRE base, not JDK)
- Container starts and binds to host on port 8765 (or overridden port)
- Dev profile active by default; can override with `SPRING_PROFILES_ACTIVE`
- MongoDB connection works from container using `SPRING_DATA_MONGODB_URI`
- Health endpoints and main APIs are reachable

## 8) Troubleshooting (Linux dev)

- App can’t connect to MongoDB running on host:
	- Use `--add-host=host.docker.internal:host-gateway` and set `SPRING_DATA_MONGODB_URI=mongodb://host.docker.internal:27017/quizfun`
	- Alternatively, run MongoDB in Docker and put both containers on the same user-defined network
- Port already in use:
	- Change `SERVER_PORT` or host mapping: `-e SERVER_PORT=8080 -p 8080:8080`
- Slow rebuilds:
	- Ensure `.dockerignore` excludes `target` directories
	- Keep the POM-only dependency cache step before copying sources
	- Consider layered JAR optimization

## 9) Future enhancements (optional)

- Docker Compose for a full dev stack (app + MongoDB + seed scripts)
- Use Spring Boot buildpacks (`mvn spring-boot:build-image`) for standardized base images
- Add SBOM and vulnerability scanning in CI
- Multi-arch builds if needed (e.g., `linux/amd64, linux/arm64`)

## 10) Plan of action

1) Add `.dockerignore` as shown above
2) Add the Dockerfile at repo root (copy from section 3)
3) Build and run locally with the dev profile and host Mongo
4) Optionally add Makefile targets and/or Compose later

This document aligns with the current multi-module layout (see `CLAUDE.md`) and requires no code changes to the application. It focuses on a clean dev-profile containerization that’s ready for Linux hosts.
