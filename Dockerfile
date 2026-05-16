# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all files and build the application with Spring Boot executable JAR
COPY . .
RUN mvn clean package -DskipTests -DfrontendSkip=true

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

# Copy the built JAR file from the build stage
COPY --from=build /app/target/nearshare-back-end-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

USER spring

# Expose the application port (Render provides $PORT; default below is 8080)
EXPOSE 8080

ENV PORT=8080
ENV SSL_ENABLED=false
ENV SETTINGS_HTTP_ENABLED=false

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f "http://localhost:${PORT}/shareit/api/health" || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
