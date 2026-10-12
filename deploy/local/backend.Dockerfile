ARG MAVEN_IMAGE=maven:3.9.11-eclipse-temurin-21-alpine
ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:21.0.8_9-jre-alpine
FROM ${MAVEN_IMAGE} AS build

ARG MAVEN_TEST_ARGS=""

WORKDIR /workspace
COPY pom.xml ./
COPY backend/service-common/pom.xml backend/service-common/pom.xml
COPY backend/gateway-service/pom.xml backend/gateway-service/pom.xml
COPY backend/catalog-service/pom.xml backend/catalog-service/pom.xml
COPY backend/inventory-service/pom.xml backend/inventory-service/pom.xml
COPY backend/order-service/pom.xml backend/order-service/pom.xml
RUN mvn -B -DskipTests dependency:go-offline

COPY backend backend
RUN mvn -B ${MAVEN_TEST_ARGS} package

FROM ${JAVA_RUNTIME_IMAGE} AS runtime

RUN addgroup -S ecommerce && adduser -S -G ecommerce ecommerce
WORKDIR /app
COPY --from=build /workspace/backend/gateway-service/target/gateway-service-*.jar /app/gateway-service.jar
COPY --from=build /workspace/backend/catalog-service/target/catalog-service-*.jar /app/catalog-service.jar
COPY --from=build /workspace/backend/inventory-service/target/inventory-service-*.jar /app/inventory-service.jar
COPY --from=build /workspace/backend/order-service/target/order-service-*.jar /app/order-service.jar
USER ecommerce

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar"]
