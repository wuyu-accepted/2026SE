#!/bin/bash
# ============================================================
# 应用服务器部署脚本
# 用途：在应用服务器上部署本项目（后端 Spring Boot + 前端 Vue）
# 前提：
#   1. JDK 17+ 已安装
#   2. Maven 3.9+ 已安装（或用 ./mvnw）
#   3. Node.js 18+ 和 npm 已安装
#   4. Nginx 已安装
#   5. 数据库服务器已就绪（PostgreSQL / Kingbase）
#   6. 本脚本在项目根目录执行
# ============================================================

set -e

# ---------- 配置项（根据实际情况修改）----------
DB_HOST="192.168.1.100"          # 数据库服务器 IP
DB_PORT="5432"                   # 数据库端口
DB_NAME="ruc_platform"           # 数据库名
DB_USER="postgres"               # 数据库用户
DB_PASSWORD="your_secure_pass"   # 数据库密码
SERVER_PORT="18080"              # 后端端口
PROJECT_DIR="$(pwd)"             # 项目根目录
DEPLOY_DIR="/opt/ruc-platform"   # 部署目录
NGINX_CONF="/etc/nginx/conf.d/ruc-platform.conf"
# -----------------------------------------------

echo "===== 开始部署 学院服务平台 ====="

# 1. 创建部署目录
echo "[1/6] 创建部署目录..."
sudo mkdir -p "$DEPLOY_DIR"/{backend,frontend}
sudo mkdir -p "$DEPLOY_DIR"/uploads

# 2. 构建 Vue 前端
echo "[2/6] 构建 Vue 前端..."
cd "$PROJECT_DIR/web-counselor"
npm install
npm run build
sudo cp -r dist/* "$DEPLOY_DIR/frontend/"

# 3. 构建 Spring Boot 后端
echo "[3/6] 构建 Spring Boot 后端..."
cd "$PROJECT_DIR/demo"
chmod +x mvnw
./mvnw -B -DskipTests clean package
sudo cp target/ruc-service-platform-1.0.0-SNAPSHOT.jar "$DEPLOY_DIR/backend/app.jar"

# 4. 配置 Nginx
echo "[4/6] 配置 Nginx..."
sudo cp "$PROJECT_DIR/deploy/nginx.conf" "$NGINX_CONF"
# 替换 root 路径为实际部署路径
sudo sed -i "s|/var/www/ruc-platform/web-counselor/dist|$DEPLOY_DIR/frontend|" "$NGINX_CONF"
sudo nginx -t
sudo systemctl reload nginx

# 5. 创建 Systemd 服务（后端守护进程）
echo "[5/6] 创建 Systemd 服务..."
sudo tee /etc/systemd/system/ruc-platform.service > /dev/null << 'SERVICEEOF'
[Unit]
Description=学院服务平台后端
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/ruc-platform/backend
ExecStart=/usr/bin/java -jar /opt/ruc-platform/backend/app.jar \
    --spring.profiles.active=server \
    --server.port=18080
Environment=DB_HOST=__DB_HOST__
Environment=DB_PORT=__DB_PORT__
Environment=DB_NAME=__DB_NAME__
Environment=DB_USER=__DB_USER__
Environment=DB_PASSWORD=__DB_PASSWORD__
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
SERVICEEOF

# 替换数据库配置
sudo sed -i "s|__DB_HOST__|$DB_HOST|g" /etc/systemd/system/ruc-platform.service
sudo sed -i "s|__DB_PORT__|$DB_PORT|g" /etc/systemd/system/ruc-platform.service
sudo sed -i "s|__DB_NAME__|$DB_NAME|g" /etc/systemd/system/ruc-platform.service
sudo sed -i "s|__DB_USER__|$DB_USER|g" /etc/systemd/system/ruc-platform.service
sudo sed -i "s|__DB_PASSWORD__|$DB_PASSWORD|g" /etc/systemd/system/ruc-platform.service

sudo systemctl daemon-reload
sudo systemctl enable ruc-platform

# 6. 启动后端
echo "[6/6] 启动后端服务..."
sudo systemctl restart ruc-platform

echo ""
echo "===== 部署完成 ====="
echo "后端 API: http://$(curl -s ifconfig.me):$SERVER_PORT/api/"
echo "辅导员端: http://$(curl -s ifconfig.me)"
echo "状态查看: sudo systemctl status ruc-platform"
echo "日志查看: sudo journalctl -u ruc-platform -f"
