FROM gradle:jdk21-alpine as build
WORKDIR /app

COPY . .

RUN gradle :geo-processor:bootJar -x test --no-daemon --console=plain

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/geo-processor/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]