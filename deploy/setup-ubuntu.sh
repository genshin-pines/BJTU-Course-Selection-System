#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/var/www/bjtu-review"
ENV_DIR="/etc/bjtu-review"
DB_NAME="bjtu_review"
DB_USER="bjtu_review"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Please run as root: sudo bash deploy/setup-ubuntu.sh"
  exit 1
fi

if [[ ! -f "backend/pom.xml" || ! -f "frontend/package.json" ]]; then
  echo "Run this script from the project root."
  exit 1
fi

read -r -s -p "Set MySQL password for ${DB_USER}: " DB_PASSWORD
echo
read -r -s -p "Set JWT secret, at least 32 chars: " JWT_SECRET
echo
read -r -p "Set SMTP username for verification email, blank to keep disabled: " MAIL_USERNAME
if [[ -n "${MAIL_USERNAME}" ]]; then
  read -r -s -p "Set SMTP auth code/password for ${MAIL_USERNAME}: " MAIL_PASSWORD
  echo
else
  MAIL_PASSWORD=""
fi

if [[ "${DB_PASSWORD}" == *"'"* || "${JWT_SECRET}" == *"'"* || "${MAIL_USERNAME}" == *"'"* || "${MAIL_PASSWORD}" == *"'"* ]]; then
  echo "For this setup script, do not use single quotes in passwords, secrets, or mail settings."
  exit 1
fi

if [[ ${#JWT_SECRET} -lt 32 ]]; then
  echo "JWT secret must be at least 32 chars."
  exit 1
fi

apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install -y \
  ca-certificates \
  curl \
  gnupg \
  openjdk-17-jdk \
  maven \
  mysql-server \
  nginx \
  rsync

if ! command -v node >/dev/null 2>&1 || [[ "$(node -v | sed 's/v//' | cut -d. -f1)" -lt 18 ]]; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
  DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs
fi

systemctl enable --now mysql
systemctl enable --now nginx

mysql <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

mysql < backend/src/main/resources/db/schema.sql
mysql < backend/src/main/resources/db/migration_align_current_schema.sql

mkdir -p "${APP_DIR}" "${ENV_DIR}"
rsync -a --delete \
  --exclude ".git" \
  --exclude "backend/target" \
  --exclude "frontend/node_modules" \
  --exclude "frontend/dist" \
  ./ "${APP_DIR}/"

cat > "${ENV_DIR}/bjtu-review.env" <<EOF
SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:3306/${DB_NAME}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=${DB_USER}
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
SPRING_MAIL_HOST=smtp.qq.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=${MAIL_USERNAME:-your-email@qq.com}
SPRING_MAIL_PASSWORD=${MAIL_PASSWORD:-your-auth-code}
EOF
chmod 600 "${ENV_DIR}/bjtu-review.env"
chown -R www-data:www-data "${APP_DIR}" "${ENV_DIR}"

cd "${APP_DIR}/backend"
mvn clean package -DskipTests
chown -R www-data:www-data "${APP_DIR}/backend/target"

cd "${APP_DIR}/frontend"
npm ci
npm run build
chown -R www-data:www-data "${APP_DIR}/frontend/dist"

cp "${APP_DIR}/deploy/bjtu-review.service" /etc/systemd/system/bjtu-review.service
cp "${APP_DIR}/deploy/nginx-bjtu-review.conf" /etc/nginx/sites-available/bjtu-review
ln -sfn /etc/nginx/sites-available/bjtu-review /etc/nginx/sites-enabled/bjtu-review
rm -f /etc/nginx/sites-enabled/default

nginx -t
systemctl daemon-reload
systemctl enable bjtu-review
systemctl restart bjtu-review
systemctl reload nginx

echo "Done. Open: http://$(curl -fsS ifconfig.me || hostname -I | awk '{print $1}')"
