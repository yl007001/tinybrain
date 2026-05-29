# =============================================================================
# TinyBrain Gateway — Dockerfile
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

COPY pom.xml ./
COPY tinybrain-common/pom.xml ./tinybrain-common/
COPY tinybrain-common/src ./tinybrain-common/src/
COPY tinybrain-gateway/pom.xml ./tinybrain-gateway/
COPY tinybrain-gateway/src ./tinybrain-gateway/src/

RUN mvn clean package -DskipTests -q -pl tinybrain-common,tinybrain-gateway -am

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /build/tinybrain-gateway/target/*.jar ./gateway.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -s http://localhost:8088/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar gateway.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}"]
