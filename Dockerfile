# Этап сборки
FROM maven:latest AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Этап запуска
FROM openjdk:17-jdk-slim
MAINTAINER HappySadAlex <alex.drevilo@gmail.com>
LABEL version="1" authors="HappySadAlex", name="tgbot_tech_task"

WORKDIR /app
COPY --from=builder /app/target/*.jar tgbot-0.0.1-SNAPSHOT.jar
COPY src/main/resources/application.yml ./application.yml
ENTRYPOINT ["java", "-Dspring.config.location=file:/app/application.yml", "-jar", "tgbot-0.0.1-SNAPSHOT.jar"]