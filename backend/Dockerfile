FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -DskipTests package

FROM eclipse-temurin:25-jre-ubi10-minimal

# Keep the supported minimal Java runtime current without unused service-manager tooling.
RUN microdnf update -y && microdnf clean all

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
USER 10001:10001

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar app.jar"]
