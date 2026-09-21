@echo off
chcp 65001
:: 检查名为 pgvector 的容器是否存在
docker inspect pgvector >nul 2>&1
if %errorlevel% equ 0 (
    echo 容器已存在，启动容器
    docker start pgvector
) else (
    echo 容器不存在，新建容器
    docker run -d ^
    --name pgvector ^
    -p 5432:5432 ^
    -e POSTGRES_DB=postgres ^
    -e POSTGRES_USER=postgres ^
    -e POSTGRES_PASSWORD=123456 ^
    pgvector/pgvector:pg16
)
echo 操作完成
pause
