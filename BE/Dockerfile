# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Copy .env if needed by spring-dotenv
COPY .env .env

# Install wait-for-it equivalent (optional, but good for DB readiness)
RUN apk add --no-cache netcat-openbsd

EXPOSE 2000
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]
