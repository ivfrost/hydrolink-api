# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-jammy AS deps
WORKDIR /build
COPY --chmod=0755 mvnw mvnw
COPY .mvn .mvn
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline -DskipTests

# Compile and package the application
FROM deps AS package
WORKDIR /build
ARG CACHEBUST
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw package -DskipTests && \
    mv target/$(./mvnw help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-\
$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout).jar target/app.jar

# Extract layers from the packaged jar
FROM package AS extract
WORKDIR /build
RUN java -Djarmode=tools -jar target/app.jar extract --layers --destination target/extracted

# DEVELOPMENT: Use the JDK and enable remote debugging.
FROM extract AS development
WORKDIR /build
RUN cp -r /build/target/extracted/dependencies/ ./
RUN cp -r /build/target/extracted/spring-boot-loader/ ./
RUN cp -r /build/target/extracted/snapshot-dependencies/ ./
RUN cp -r /build/target/extracted/application/ ./
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8000
CMD ["java", "-Dspring.profiles.active=default", "-jar", "app.jar"]

# PRODUCTION: Use the JRE and a non-root user.
FROM eclipse-temurin:25-jre-jammy AS production
ARG UID=10001
RUN adduser --disabled-password --gecos "" --home "/nonexistent" --shell "/sbin/nologin" --no-create-home --uid "${UID}" appuser

WORKDIR /app
USER appuser

COPY --from=extract /build/target/extracted/application/ ./
COPY --from=extract /build/target/extracted/dependencies/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]