# ===== Stage 1: Build =====
# Uses a full Maven + JDK image, since building the app needs the compiler and dependency resolution
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy only the pom.xml first — Docker caches this layer, so dependencies
# only get re-downloaded when pom.xml actually changes, not on every code edit
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy the actual source code and build the jar
COPY src ./src
RUN mvn clean package -DskipTests

# ===== Stage 2: Run =====
# Much smaller image — just the Java runtime, no Maven, no build tools.
# This is what actually gets deployed.
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy ONLY the built jar from the build stage — nothing else from Stage 1
COPY --from=build /app/target/*.jar app.jar

# The port your Spring Boot app listens on
EXPOSE 8080

# Startup command
ENTRYPOINT ["java", "-jar", "app.jar"]