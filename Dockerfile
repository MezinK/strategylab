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

RUN groupadd --system appuser && useradd --system --gid appuser appuser

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

# Railway sets PORT env var; Spring reads SERVER_PORT automatically
ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
