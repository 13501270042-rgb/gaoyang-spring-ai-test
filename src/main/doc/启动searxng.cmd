@echo off
chcp 65001 >nul
:: 检查 searxng 容器是否存在
docker inspect searxng >nul 2>&1
if %errorlevel% equ 0 (
    echo [信息] 容器 searxng 已存在，正在启动...
    docker start searxng
) else (
    echo [信息] 容器不存在，新建并启动 searxng
    docker run -d --name searxng ^
-p 8888:8080 ^
-v %USERPROFILE%\searxng-config:/etc/searxng ^
-e SEARXNG_BASE_URL=http://127.0.0.1:8888/ ^
searxng/searxng:latest
)

echo [完成] 访问地址：http://127.0.0.1:8888
pause
