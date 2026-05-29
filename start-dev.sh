#!/bin/bash
# =============================================================================
# TinyBrain — 开发环境启动脚本
# =============================================================================
# 用法: ./start-dev.sh [options]
# 选项:
#   -b, --build   先执行 Maven 编译
#   -t, --test    运行单元测试
#   -h, --help    显示帮助信息
# =============================================================================

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "🚀 TinyBrain 开发环境启动"
echo "========================"

# 解析参数
BUILD=false
TEST=false

for arg in "$@"; do
    case "$arg" in
        -b|--build) BUILD=true ;;
        -t|--test)  TEST=true  ;;
        -h|--help)
            echo "用法: $0 [-b] [-t] [-h]"
            echo "  -b, --build   先执行 Maven 编译"
            echo "  -t, --test    运行单元测试"
            echo "  -h, --help    显示帮助信息"
            exit 0
            ;;
        *)
            echo "未知参数: $arg"
            exit 1
            ;;
    esac
done

# 测试
if [ "$TEST" = true ]; then
    echo ""
    echo "📦 运行单元测试..."
    mvn test -q -pl tinybrain-common,tinybrain-rag,tinybrain-agent
    echo "✅ 测试完成"
fi

# 编译
if [ "$BUILD" = true ]; then
    echo ""
    echo "📦 编译项目..."
    mvn clean install -DskipTests -q
    echo "✅ 编译完成"
fi

# 启动应用（dev 模式，H2 内存数据库）
echo ""
echo "📦 启动 TinyBrain (dev profile, H2 内存数据库)..."
echo "    Swagger UI: http://localhost:8080/swagger-ui.html"
echo "    API 文档:   http://localhost:8080/v3/api-docs"
echo "    H2 控制台:  http://localhost:8080/h2-console"
echo ""

cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
