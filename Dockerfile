FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy the Maven descriptors first to maximize Docker layer caching.
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw

# Build the application artifact without running tests in the image build stage.
COPY src ./src
RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
