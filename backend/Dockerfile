# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package && cp target/*.jar app.jar

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S villagemart && adduser -S villagemart -G villagemart \
    && apk add --no-cache wget tzdata
ENV TZ=Asia/Kolkata \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER villagemart
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/actuator/health" | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
