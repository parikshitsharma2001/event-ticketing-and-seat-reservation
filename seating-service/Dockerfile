FROM maven:3.8.6-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/seating-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
