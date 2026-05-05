FROM eclipse-temurin:23-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw mvnw
COPY pom.xml pom.xml
COPY logstore-core/pom.xml logstore-core/pom.xml
COPY logstore-server/pom.xml logstore-server/pom.xml
COPY logstore-benchmarks/pom.xml logstore-benchmarks/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY logstore-core logstore-core
COPY logstore-server logstore-server
COPY logstore-benchmarks logstore-benchmarks
COPY data data
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:23-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/logstore-server/target/logstore-server-0.1.0.jar app.jar
COPY --from=builder /workspace/data ./data

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
