# Build stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app
# First copy only the POM file to leverage Docker cache
COPY pom.xml .
# Download dependencies (this step is cached unless pom.xml changes)
RUN mvn dependency:go-offline -B
# Copy source code
COPY src /app/src
# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
