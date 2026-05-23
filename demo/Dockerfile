FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /build/target/ruc-service-platform-1.0.0-SNAPSHOT.jar /app/app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
