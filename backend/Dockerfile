# FinSight deployment: deterministic Spring Boot build/runtime for Railway.
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src src
RUN mvn -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

ENV TZ=Asia/Kuala_Lumpur
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java -jar app.jar"]
