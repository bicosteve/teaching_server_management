# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache inotify-tools

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw \
	&& ./mvnw -B -q dependency:go-offline

COPY src src
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh \
	&& ./mvnw -B -q -DskipTests package \
	&& cp target/*.jar /workspace/app.jar

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
