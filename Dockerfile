FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
COPY data data
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/target/LogStore-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /workspace/data ./data

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
