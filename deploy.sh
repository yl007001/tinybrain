#!/bin/bash
# =============================================================================
# TinyBrain 一键部署脚本
# =============================================================================
# 用法:
#   开发环境:  ./deploy.sh dev
#   生产环境:  ./deploy.sh prod
#   Docker:    ./deploy.sh docker
#   Ollama:    ./deploy.sh ollama
# =============================================================================

set -e

MODE="${1:-dev}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  TinyBrain 部署脚本"
echo "  模式: $MODE"
echo "=========================================="

case "$MODE" in
  dev)
    echo "[1/3] 编译项目..."
    mvn clean install -DskipTests -q

    echo "[2/3] 启动后端 (H2 内存数据库)..."
    echo "       LLM API Key: ${TINYBRAIN_LLM_KEY:-未设置}"
    cd tinybrain-app
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ;;

  prod)
    echo "[1/4] 检查环境变量..."
    if [ -z "$TINYBRAIN_JWT_SECRET" ]; then
      echo "  ⚠️  TINYBRAIN_JWT_SECRET 未设置，将使用开发密钥"
    fi
    if [ -z "$TINYBRAIN_LLM_KEY" ]; then
      echo "  ⚠️  TINYBRAIN_LLM_KEY 未设置，LLM 功能不可用"
    fi
    if [ -z "$TINYBRAIN_DB_PASSWORD" ]; then
      echo "  ❌ TINYBRAIN_DB_PASSWORD 必须设置"
      exit 1
    fi

    echo "[2/4] 编译项目..."
    mvn clean package -DskipTests -q

    echo "[3/4] 启动应用..."
    cd tinybrain-app
    java $JAVA_OPTS -jar target/*.jar --spring.profiles.active=prod
    ;;

  docker)
    echo "[1/3] 编译项目..."
    mvn clean package -DskipTests -q

    echo "[2/3] 启动 Docker Compose..."
    export TINYBRAIN_LLM_KEY="${TINYBRAIN_LLM_KEY:-}"
    export TINYBRAIN_JWT_SECRET="${TINYBRAIN_JWT_SECRET:-}"

    docker-compose up -d --build

    echo "[3/3] 检查服务状态..."
    sleep 5
    curl -s http://localhost:8080/actuator/health | head -c 200
    echo ""
    echo "=========================================="
    echo "  TinyBrain 已启动！"
    echo "  API:     http://localhost:8080"
    echo "  Swagger: http://localhost:8080/swagger-ui.html"
    echo "  Grafana: http://localhost:3000 (admin/admin)"
    echo "  Zipkin:  http://localhost:9411"
    echo "=========================================="
    ;;

  ollama)
    echo "[1/3] 确保 Ollama 已安装..."
    if ! command -v ollama &>/dev/null; then
      echo "  ❌ Ollama 未安装，请先安装: https://ollama.ai"
      exit 1
    fi

    echo "[2/3] 拉取模型..."
    ollama pull qwen2.5:7b 2>/dev/null || echo "  ⚠️  模型拉取失败，可手动执行: ollama pull qwen2.5:7b"
    ollama pull nomic-embed-text 2>/dev/null || echo "  ⚠️  模型拉取失败，可手动执行: ollama pull nomic-embed-text"

    echo "[3/3] 启动应用 (Ollama 模式)..."
    cd tinybrain-app
    mvn spring-boot:run -Dspring-boot.run.profiles=ollama
    ;;

  *)
    echo "用法: ./deploy.sh [dev|prod|docker|ollama]"
    exit 1
    ;;
esac
