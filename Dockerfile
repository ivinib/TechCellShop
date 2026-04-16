# Build stage
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests clean package

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL org.opencontainers.image.title="TechCellShop"
LABEL org.opencontainers.image.description="Modular monolith Spring Boot backend for a tech retail domain"
LABEL org.opencontainers.image.source="https://github.com/ivinib/TechCellShop"

RUN groupadd --system spring && useradd --system --gid spring --create-home spring

COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]