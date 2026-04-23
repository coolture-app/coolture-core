FROM eclipse-temurin:25-jdk-alpine AS builder-api
WORKDIR /build/rest-api
RUN apk add --no-cache bash
COPY rest-api/pom.xml rest-api/mvnw rest-api/checkstyle.xml ./
COPY rest-api/.mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY rest-api/src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jdk-alpine AS builder-gateway
WORKDIR /build/gateway
RUN apk add --no-cache bash
COPY gateway/pom.xml gateway/mvnw gateway/checkstyle.xml ./
COPY gateway/.mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY gateway/src ./src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder-api /build/rest-api/target/*.jar rest-api.jar
COPY --from=builder-gateway /build/gateway/target/*.jar gateway.jar
COPY docker/entrypoint.sh ./entrypoint.sh
RUN chmod +x entrypoint.sh
EXPOSE 8080 8081
ENTRYPOINT ["./entrypoint.sh"]
