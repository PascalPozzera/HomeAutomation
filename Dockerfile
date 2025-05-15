# ---- Build Stage ----
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
COPY . .

RUN ./gradlew clean build --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
