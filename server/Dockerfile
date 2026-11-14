# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

RUN apk add --no-cache inotify-tools postgresql-client

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw \
	&& ./mvnw -B -q dependency:go-offline

COPY src src
COPY scripts scripts
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh scripts/*.sh \
	&& ./mvnw -B -q -DskipTests package \
	&& cp target/*.jar /workspace/app.jar

EXPOSE 8080
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
