# what I did
Added Dockerfile at repo root using the documented multi-stage build:
Maven builder (Java 21) + runtime JRE image
Caches dependencies via POM-only copy and go-offline
Builds the full multi-module project with tests skipped
Copies the orchestration-layer JAR to the final image
Added .dockerignore to keep the context small and builds fast.
Fixed pom.xml:
Removed an incorrect dependency on the aggregator module internal-layer (packaging=pom), which caused build failures in Docker.
Build result:

Image built successfully as quiz-cms-api:dev.

# try it
Run with dev profile, talking to your host MongoDB on Linux:
```
docker run --rm \
  --add-host=primary:host-gateway \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATA_MONGODB_URI='mongodb://root:bdffe98cbd9f1f134bd48ca3918c1deb38e381e90a1ddc8b582c952f92c2b58e@primary:27017,secondary:27018/quizfun?authSource=admin' \
  -p 8765:8765 \
  --name quiz-cms-api \
  quiz-cms-api:dev
```


Override profile or port:
```
# prod profile
docker run --rm -p 8765:8765 -e SPRING_PROFILES_ACTIVE=prod quiz-cms-api:dev# change server portdocker run --rm -p 8080:8080 -e SERVER_PORT=8080 quiz-cms-api:dev
```

notes
Files created:
Dockerfile — multi-stage build, default dev profile
.dockerignore — ignores target/, .git, Allure outputs, etc.
Files edited:
pom.xml — removed dependency on internal-layer aggregator
If you’d like, I can add the optional Make targets or a compose file to run the app and Mongo together.