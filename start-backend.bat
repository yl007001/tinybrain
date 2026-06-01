@echo off
REM TinyBrain 后端启动脚本（Windows）
REM 用法: 双击运行 或 在终端执行 start-backend.bat

cd /d "%~dp0"

REM 加载 .env 文件
if exist .env (
    echo [TinyBrain] 加载环境变量 from .env
    for /F "usebackq tokens=1,* delims==" %%A in (.env) do (
        REM 跳过注释和空行
        echo %%A | findstr /r "^#" >nul 2>&1 || (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
) else (
    echo [错误] .env 文件不存在！
    echo 请复制 .env.example 为 .env 并填入你的 API Key
    echo 命令: copy .env.example .env
    pause
    exit /b 1
)

REM 检查必要的环境变量
if "%TINYBRAIN_LLM_KEY%"=="" (
    echo [错误] TINYBRAIN_LLM_KEY 未设置！请在 .env 中配置 DeepSeek API Key
    pause
    exit /b 1
)

echo [TinyBrain] 启动后端服务...
echo [TinyBrain] LLM Key: %TINYBRAIN_LLM_KEY:~0,10%...
echo [TinyBrain] 访问地址: http://localhost:8080/swagger-ui.html
echo.

cd tinybrain-app
mvn spring-boot:run -Dspring-boot.run.profiles=dev
