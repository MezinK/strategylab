# ── Build stage ──
FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

# Cache Gradle wrapper + dependencies
COPY gradlew gradlew
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version

COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

# Copy source and build (skip tests — they need network for Yahoo Finance)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -x sonarlintMain -x sonarlintTest -x pmdMain -x pmdTest

# ── Runtime stage ──
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Railway provides PORT env var
ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
