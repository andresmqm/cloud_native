FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# Crear el directorio EFS antes de ejecutar la aplicación
RUN mkdir -p /app/efs

COPY --from=build /app/target/eventos-service-1.0.0.jar app.jar
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]