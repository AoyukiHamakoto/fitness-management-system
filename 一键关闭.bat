@echo off
chcp 65001 >nul
setlocal EnableExtensions
title 智能健身 - 一键关闭

echo 正在尝试结束占用 8080 ^(后端^) 与 5173 ^(前端^) 端口的监听进程...
echo.

powershell -NoProfile -ExecutionPolicy Bypass -Command "& { $ports = 8080, 5173; foreach ($port in $ports) { Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | ForEach-Object { $pid = $_.OwningProcess; $n = (Get-Process -Id $pid -ErrorAction SilentlyContinue).ProcessName; Write-Host ('端口 ' + $port + ' -> PID ' + $pid + ' (' + $n + ')'); Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue } } }"

echo.
echo 处理完成。若端口仍被占用，请关闭标题为 fitness-backend / fitness-frontend 的窗口，
echo 或在任务管理器中结束对应的 java.exe / node.exe。
echo.
pause
