@echo off
chcp 65001 >nul
setlocal EnableExtensions
title 智能健身 - 一键启动

set "ROOT=%~dp0"
set "BACKEND=%ROOT%fitness-management-backend"
set "FRONTEND=%ROOT%fitness-management-frontend"

if not exist "%BACKEND%\pom.xml" (
  echo [错误] 未找到后端目录: "%BACKEND%"
  pause
  exit /b 1
)
if not exist "%FRONTEND%\package.json" (
  echo [错误] 未找到前端目录: "%FRONTEND%"
  pause
  exit /b 1
)

echo.
echo [1/2] 启动后端 Spring Boot ^(新窗口^) ...
start "fitness-backend" cmd /k "cd /d ""%BACKEND%"" && echo 目录: %%CD%% && mvn spring-boot:run"

echo [2/2] 启动前端 Vite ^(新窗口^) ...
timeout /t 2 /nobreak >nul
start "fitness-frontend" cmd /k "cd /d ""%FRONTEND%"" && echo 目录: %%CD%% && if exist node_modules (npm run dev) else (npm install && npm run dev)"

echo.
echo ========================================
echo   后端 API: http://localhost:8080/fitness-api
echo   前端页面: http://localhost:5173
echo ========================================
echo.
echo 已打开两个独立窗口运行服务。关闭对应窗口即可停止该服务；
echo 或运行根目录「一键关闭.bat」尝试释放 8080 / 5173 端口。
echo.
pause
