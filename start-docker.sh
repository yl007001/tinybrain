#!/bin/bash
# =============================================================================
# TinyBrain — Docker 环境启动脚本
# =============================================================================
# 用法: ./start-docker.sh [options]
#
# 启动所有依赖服务（MySQL、ES、Redis、Prometheus、Grafana、Zipkin）
# 以及 TinyBrain App 和 Gateway。
#
# 前提: 已安装 Docker 和 Docker Compose
# =============================================================================

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "🐳 TinyBrain Docker 环境启动"
echo "============================"

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: 未找到 Docker，请先安装 Docker Desktop"
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null 2>&1; then
    echo "❌ 错误: 未找到 Docker Compose"
    exit 1
fi

# 构建项目（确保 JAR 是最新的）
echo ""
echo "📦 构建项目..."
mvn clean install -DskipTests -q
echo "✅ 构建完成"

# 启动 Docker Compose
echo ""
echo "📦 启动所有服务..."
echo "    TinyBrain App:  http://localhost:8080/swagger-ui.html"
echo "    Gateway:        http://localhost:8088/actuator/health"
echo "    Prometheus:     http://localhost:9090"
echo "    Grafana:        http://localhost:3000 (admin/admin)"
echo "    Zipkin:         http://localhost:9411"
echo ""

docker compose up -d

echo ""
echo "✅ 所有服务已启动！"
echo "   查看日志: docker compose logs -f tinybrain-app"
echo "   停止服务: docker compose down -v"
