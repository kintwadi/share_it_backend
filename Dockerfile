# Build stage
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy all files and build the application with Spring Boot executable JAR
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install PostgreSQL
RUN apk update && apk add postgresql postgresql-contrib

# Copy the built JAR file from the build stage
COPY --from=build /app/target/nearshare-back-end-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/entrypoint.sh /app/entrypoint.sh

# Create a non-root user to run the application
RUN addgroup -S spring && adduser -S spring -G spring

# Setup PostgreSQL directories and permissions
RUN mkdir -p /run/postgresql /var/lib/postgresql/data && \
    chown -R spring:spring /run/postgresql /var/lib/postgresql && \
    chmod +x /app/entrypoint.sh

USER spring

# Expose the application port
EXPOSE 8081

# Environment variables
ENV DB_URL=jdbc:postgresql://localhost:5432/nearshare
ENV DB_USERNAME=${DB_USERNAME}
ENV DB_PASSWORD=${DB_PASSWORD}
ENV DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect

# Other ENV variables preserved
ENV JWT_SECRET=${JWT_SECRET}
ENV AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
ENV AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
ENV R2_ACCOUNT_ID=${R2_ACCOUNT_ID}
ENV R2_ACCESS_KEY_ID=${R2_ACCESS_KEY_ID}
ENV R2_SECRET_ACCESS_KEY=${R2_SECRET_ACCESS_KEY}
ENV R2_BUCKET_NAME=${R2_BUCKET_NAME}
ENV R2_ENDPOINT=${R2_ENDPOINT}
ENV R2_PUBLIC_URL=${R2_PUBLIC_URL}
ENV STRIPE_PUBLIC_KEY=${STRIPE_PUBLIC_KEY}
ENV STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}
ENV STRIPE_WEBHOOK_SECRET=${STRIPE_WEBHOOK_SECRET}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["/app/entrypoint.sh"]