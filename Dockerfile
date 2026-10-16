# Stage 1: Build with Maven & Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimalist Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy compiled JAR artifact
COPY --from=build /app/target/smartquiz-backend-1.0.0.jar app.jar


# Run Spring Boot application with container-aware memory limits
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

