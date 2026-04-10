FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

COPY ./common ./common
COPY ./gateway ./gateway

WORKDIR /build/gateway
RUN ./gradlew shadowJar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/gateway/build/libs/gateway.jar app.jar

EXPOSE 1984/udp
EXPOSE 1488

ENTRYPOINT ["java", "-jar", "app.jar"]