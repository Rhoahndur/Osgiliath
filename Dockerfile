# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend/ .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/osgiliath-backend-1.0.0-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Xmx512m -Xms256m -XX:+UseG1GC -Dserver.port=$PORT -Dspring.profiles.active=prod -jar app.jar"]
