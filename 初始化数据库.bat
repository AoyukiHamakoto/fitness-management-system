@echo off
chcp 65001 >nul
setlocal
title 智能健身 - 初始化 MySQL（fitness_mvp）

set "ROOT=%~dp0"
set "SQL=%ROOT%fitness-management-backend\src\main\resources\db\fitness_mvp_all.sql"

if not exist "%SQL%" (
  echo [错误] 找不到脚本: "%SQL%"
  pause
  exit /b 1
)

echo 将使用账号 root / 密码 123456 执行建库与建表（会 DROP 旧表后重建）。
echo 请确保本机 MySQL 已启动，且 mysql 命令在 PATH 中（或改为完整路径）。
echo.
mysql -uroot -p123456 --default-character-set=utf8mb4 < "%SQL%"
if errorlevel 1 (
  echo.
  echo [失败] 请检查：MySQL 服务是否运行、root 密码是否为 123456、mysql 是否在 PATH。
  pause
  exit /b 1
)

echo.
echo [完成] 数据库 fitness_mvp 已就绪。
mysql -uroot -p123456 -e "USE fitness_mvp; SHOW TABLES;"
echo.
pause
