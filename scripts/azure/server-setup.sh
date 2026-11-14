#!/usr/bin/env bash
# One-time setup of a fresh Ubuntu 24.04 Azure VM for the HarvestHub backend.
# Run ON THE SERVER:  sudo bash server-setup.sh harvesthubapp.tech you@example.com
# Installs Java 21, nginx and certbot, creates the 'harvesthub' systemd service and the
# environment file /etc/harvesthub/harvesthub.env (fill in the secrets afterwards).
set -euo pipefail

DOMAIN="${1:?usage: sudo bash server-setup.sh <domain> <email-for-ssl>}"
EMAIL="${2:?usage: sudo bash server-setup.sh <domain> <email-for-ssl>}"
APP_USER=harvesthub
APP_DIR=/opt/harvesthub

echo "== packages"
apt-get update -y
apt-get install -y openjdk-21-jre-headless nginx certbot python3-certbot-nginx ufw

echo "== swap (Spring Boot on a small VM needs it)"
if ! swapon --show | grep -q swapfile; then
  fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

echo "== app user and folders"
id -u "$APP_USER" >/dev/null 2>&1 || useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin "$APP_USER"
mkdir -p "$APP_DIR/uploads" /etc/harvesthub
chown -R "$APP_USER:$APP_USER" "$APP_DIR"

if [ ! -f /etc/harvesthub/harvesthub.env ]; then
  cat > /etc/harvesthub/harvesthub.env <<'ENV'
# HarvestHub production config. Fill in every value, then: sudo systemctl restart harvesthub
MONGODB_URI=
MONGODB_DATABASE=harvesthub_db
JWT_SECRET=
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
GOOGLE_MAPS_API_KEY=
UPLOAD_DIR=/opt/harvesthub/uploads
SEED_DEMO_DATA=false
ENV
  # Generate a strong JWT secret automatically.
  sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')|" /etc/harvesthub/harvesthub.env
fi
chown root:"$APP_USER" /etc/harvesthub/harvesthub.env
chmod 640 /etc/harvesthub/harvesthub.env

echo "== systemd service"
cat > /etc/systemd/system/harvesthub.service <<UNIT
[Unit]
Description=HarvestHub backend
After=network-online.target
Wants=network-online.target

[Service]
User=$APP_USER
WorkingDirectory=$APP_DIR
EnvironmentFile=/etc/harvesthub/harvesthub.env
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar $APP_DIR/app.jar --server.port=8080 --server.address=127.0.0.1 --server.forward-headers-strategy=native
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable harvesthub

echo "== nginx reverse proxy"
cat > /etc/nginx/sites-available/harvesthub <<NGINX
server {
    listen 80;
    server_name $DOMAIN;
    client_max_body_size 30M;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 120s;
    }
}
NGINX
ln -sf /etc/nginx/sites-available/harvesthub /etc/nginx/sites-enabled/harvesthub
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

echo "== firewall (SSH + HTTP/HTTPS only; 8080 stays private)"
ufw allow OpenSSH && ufw allow 'Nginx Full' && ufw --force enable

echo "== HTTPS certificate"
if getent hosts "$DOMAIN" >/dev/null; then
  certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos -m "$EMAIL" --redirect \
    || echo "!! certbot failed - check the DNS A record points to this VM, then run: sudo certbot --nginx -d $DOMAIN"
else
  echo "!! $DOMAIN does not resolve yet. After DNS points here, run: sudo certbot --nginx -d $DOMAIN"
fi

echo
echo "Done. Next:"
echo "  1. sudo nano /etc/harvesthub/harvesthub.env   (fill MONGODB_URI, MAIL_*, GOOGLE_MAPS_API_KEY)"
echo "  2. From your laptop: scripts\\azure\\deploy-azure.ps1 -HostName <VM IP> -KeyPath <your .pem>"
