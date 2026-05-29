# =============================================================================
# TinyBrain — 多阶段构建 Dockerfile
# =============================================================================
# 构建阶段: 使用 Maven + JDK 17 编译项目
# 运行阶段: 使用 JDK 17 精简镜像运行
#
# 构建:
#   docker build -t tinybrain:latest .
#
# 运行（需先启动 MySQL/ES/Nacos 等依赖）:
#   docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=docker tinybrain:latest
# =============================================================================

# ---- 构建阶段 ----
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制所有 POM 和源码
COPY pom.xml ./
COPY tinybrain-common/pom.xml ./tinybrain-common/
COPY tinybrain-common/src ./tinybrain-common/src/
COPY tinybrain-user/pom.xml ./tinybrain-user/
COPY tinybrain-user/src ./tinybrain-user/src/
COPY tinybrain-knowledge/pom.xml ./tinybrain-knowledge/
COPY tinybrain-knowledge/src ./tinybrain-knowledge/src/
COPY tinybrain-rag/pom.xml ./tinybrain-rag/
COPY tinybrain-rag/src ./tinybrain-rag/src/
COPY tinybrain-agent/pom.xml ./tinybrain-agent/
COPY tinybrain-agent/src ./tinybrain-agent/src/
COPY tinybrain-app/pom.xml ./tinybrain-app/
COPY tinybrain-app/src ./tinybrain-app/src/

# 编译及打包（跳过测试，加速构建）
RUN mvn clean package -DskipTests -q

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# 安装 curl（用于健康检查）
RUN apk add --no-cache curl

# 从构建阶段复制 JAR
COPY --from=builder /build/tinybrain-app/target/*.jar ./app.jar

# 运行时 JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -s http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-docker}"]
