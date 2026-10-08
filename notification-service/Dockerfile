FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY notification-service/pom.xml ./pom.xml
COPY notification-service/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/notification-service-0.1.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
