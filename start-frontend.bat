@echo off
REM TinyBrain 前端启动脚本（Windows）
REM 用法: 双击运行 或 在终端执行 start-frontend.bat

cd /d "%~dp0\tinybrain-ui"

if not exist node_modules (
    echo [TinyBrain] 首次启动，安装依赖...
    call npm install
)

echo [TinyBrain] 启动前端服务...
echo [TinyBrain] 访问地址: http://localhost:5173
echo [TinyBrain] 演示账号: demo / password
echo.

call npm run dev
