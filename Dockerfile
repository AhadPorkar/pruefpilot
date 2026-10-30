# Release baseline verified 2026-10-23. Pin image digests after the first verified release build.
FROM node:26.12.1-bookworm-slim AS web
WORKDIR /src/frontend
COPY frontend/package.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build && npm test

FROM maven:3-eclipse-temurin-26 AS java
WORKDIR /src/backend
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src/ ./src/
COPY --from=web /src/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -ntp package
COPY infra/HealthCheck.java /tmp/HealthCheck.java
RUN javac -d /tmp /tmp/HealthCheck.java

FROM eclipse-temurin:27_35-jre-resolute
LABEL org.opencontainers.image.title="PruefPilot" \
      org.opencontainers.image.description="Asset inspections and independently verified corrective actions" \
      org.opencontainers.image.version="1.0.0-rc.1" \
      org.opencontainers.image.authors="Ahad Porkar" \
      org.opencontainers.image.licenses="MIT"
WORKDIR /app
RUN groupadd --system --gid 10001 pilot && useradd --system --uid 10001 --gid pilot --no-create-home pilot
COPY --from=java --chown=10001:10001 /src/backend/target/pruefpilot-1.0.0-rc.1.jar /app/app.jar
COPY --from=java --chown=10001:10001 /tmp/HealthCheck.class /app/HealthCheck.class
USER 10001:10001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=8s --start-period=90s --retries=4 CMD ["java","-cp","/app","HealthCheck"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
