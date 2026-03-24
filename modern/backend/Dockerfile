FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
ENV GRADLE_USER_HOME=/workspace/.gradle-docker

COPY gradlew /workspace/gradlew
COPY gradle /workspace/gradle
COPY modern/backend/build.gradle.kts /workspace/modern/backend/build.gradle.kts
COPY modern/backend/settings.gradle.kts /workspace/modern/backend/settings.gradle.kts

RUN chmod +x /workspace/gradlew
RUN ./gradlew -p modern/backend dependencies --no-daemon

COPY modern/backend/src /workspace/modern/backend/src

RUN ./gradlew -p modern/backend bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/modern/backend/build/libs/*.jar /app/app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
