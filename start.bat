@echo off
REM TinyBrain 一键启动脚本（Windows）
REM 同时启动后端和前端

cd /d "%~dp0"

echo ========================================
echo   TinyBrain - 个人 AI 知识引擎
echo ========================================
echo.

REM 加载 .env
if exist .env (
    for /F "usebackq tokens=1,* delims==" %%A in (.env) do (
        echo %%A | findstr /r "^#" >nul 2>&1 || (
            if not "%%A"=="" set "%%A=%%B"
        )
    )
) else (
    echo [错误] .env 文件不存在！请执行: copy .env.example .env
    pause
    exit /b 1
)

echo [1/2] 启动后端 (http://localhost:8080)...
start "TinyBrain Backend" cmd /c "cd /d %~dp0 && cd tinybrain-app && mvn spring-boot:run -Dspring-boot.run.profiles=dev"

REM 等待后端启动
echo [2/2] 等待后端启动后启动前端...
timeout /t 15 /nobreak >nul

start "TinyBrain Frontend" cmd /c "cd /d %~dp0\tinybrain-ui && npm run dev"

echo.
echo ========================================
echo   启动完成！
echo   前端: http://localhost:5173
echo   后端: http://localhost:8080/swagger-ui.html
echo   账号: demo / password
echo ========================================
echo.
pause
