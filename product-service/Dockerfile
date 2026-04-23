FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY ./common ./common
COPY ./product-service ./product-service

WORKDIR /build/product-service
RUN ./gradlew shadowJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/product-service/build/libs/product-service.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]