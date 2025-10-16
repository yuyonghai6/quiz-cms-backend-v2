# --- Stage 1: builder ---
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy POMs to prime the Maven cache for multi-module build
COPY pom.xml ./
COPY orchestration-layer/pom.xml ./orchestration-layer/
COPY internal-layer/pom.xml ./internal-layer/
COPY internal-layer/shared/pom.xml ./internal-layer/shared/
COPY internal-layer/question-bank/pom.xml ./internal-layer/question-bank/
COPY internal-layer/question-bank-query/pom.xml ./internal-layer/question-bank-query/
COPY global-shared-library/pom.xml ./global-shared-library/
COPY external-service-proxy/pom.xml ./external-service-proxy/

# Warm dependencies cache
RUN mvn -q -T1C dependency:go-offline

# Copy all sources and build (skip tests for image build path)
COPY . .
RUN mvn -q -T1C clean install -DskipTests

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Defaults for dev. Overridable at runtime
ENV SPRING_PROFILES_ACTIVE=dev \
    SERVER_PORT=8765

# Copy the runnable JAR from the orchestration-layer module
COPY --from=builder /app/orchestration-layer/target/orchestration-layer-*.jar /app/application.jar

# Expose app port
EXPOSE 8765

# Start the app
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
