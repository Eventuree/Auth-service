FROM eclipse-temurin:21-jdk AS local

RUN apt-get update && apt-get install -y tzdata iputils-ping curl dnsutils
ENV TZ=UTC

WORKDIR /app
COPY target/auth_service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

FROM maven:3.9.6-eclipse-temurin-21-alpine as builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jdk-alpine

RUN apk update && apk add tzdata
ENV TZ=UTC

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
