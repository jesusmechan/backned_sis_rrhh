# Imagen para Render (Neon + env vars en el panel). Local: mvn spring-boot:run o scripts/ en macOS.
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/rrhh-api-*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
