FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY insights-service/pom.xml ./pom.xml
COPY insights-service/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/target/insights-service-0.1.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
