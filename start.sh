#!/bin/bash
# TinyBrain 一键启动脚本（Linux/Mac）
# 用法: chmod +x start.sh && ./start.sh

cd "$(dirname "$0")"

echo "========================================"
echo "  TinyBrain - 个人 AI 知识引擎"
echo "========================================"
echo ""

# 加载 .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
    echo "[TinyBrain] 环境变量已加载"
else
    echo "[错误] .env 文件不存在！请执行: cp .env.example .env"
    exit 1
fi

# 检查必要的环境变量
if [ -z "$TINYBRAIN_LLM_KEY" ]; then
    echo "[错误] TINYBRAIN_LLM_KEY 未设置！请在 .env 中配置 DeepSeek API Key"
    exit 1
fi

echo "[TinyBrain] LLM Key: ${TINYBRAIN_LLM_KEY:0:10}..."
echo ""

# 启动后端
echo "[1/2] 启动后端 (http://localhost:8080)..."
cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev &
BACKEND_PID=$!
cd ..

# 等待后端启动
echo "[2/2] 等待后端启动..."
for i in $(seq 1 60); do
    if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then
        echo "[TinyBrain] 后端启动成功！"
        break
    fi
    sleep 2
done

# 启动前端
echo "[TinyBrain] 启动前端 (http://localhost:5173)..."
cd tinybrain-ui
if [ ! -d "node_modules" ]; then
    echo "[TinyBrain] 首次启动，安装依赖..."
    npm install
fi
npm run dev &
cd ..

echo ""
echo "========================================"
echo "  启动完成！"
echo "  前端: http://localhost:5173"
echo "  后端: http://localhost:8080/swagger-ui.html"
echo "  账号: demo / password"
echo "========================================"
echo ""
echo "按 Ctrl+C 停止所有服务"
wait
