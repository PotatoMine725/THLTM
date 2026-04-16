#!/usr/bin/env bash
set -euo pipefail

APP_PORT="${APP_PORT:-61000}"
DB_PATH="${DB_PATH:-/var/lib/wifichat/chat.db}"
REMOTE_DIR="${REMOTE_DIR:-/opt/wifichat}"
DEPLOY_DIR="/tmp/wifichat-deploy"
SETUP_FIREWALL="${SETUP_FIREWALL:-0}"
OPEN_UDP_DISCOVERY="${OPEN_UDP_DISCOVERY:-0}"
UDP_PORT="${UDP_PORT:-50000}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-port)
      APP_PORT="$2"
      shift 2
      ;;
    --db-path)
      DB_PATH="$2"
      shift 2
      ;;
    --remote-dir)
      REMOTE_DIR="$2"
      shift 2
      ;;
    --deploy-dir)
      DEPLOY_DIR="$2"
      shift 2
      ;;
    --setup-firewall)
      SETUP_FIREWALL=1
      shift
      ;;
    --open-udp-discovery)
      OPEN_UDP_DISCOVERY=1
      shift
      ;;
    --udp-port)
      UDP_PORT="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "$DEPLOY_DIR/wifi-chat-server.jar" ]]; then
  echo "Missing artifact: $DEPLOY_DIR/wifi-chat-server.jar" >&2
  exit 1
fi

if [[ $EUID -ne 0 ]]; then
  if command -v sudo >/dev/null 2>&1; then
    exec sudo --preserve-env=APP_PORT,DB_PATH,REMOTE_DIR,SETUP_FIREWALL,OPEN_UDP_DISCOVERY,UDP_PORT bash "$0" "$@"
  fi
  echo "Please run as root (or install sudo)."
  exit 1
fi

apt_update_with_fallback() {
  set +e
  apt-get -o Acquire::Retries=3 update
  local rc=$?
  set -e
  if [[ $rc -eq 0 ]]; then
    return 0
  fi

  echo "apt-get update failed due to mirror inconsistency. Retrying with DEP-11 disabled..."
  apt-get clean
  rm -rf /var/lib/apt/lists/*
  apt-get \
    -o Acquire::Retries=5 \
    -o Acquire::Languages=none \
    -o Acquire::IndexTargets::deb::DEP-11::DefaultEnabled=false \
    -o Acquire::IndexTargets::deb-src::DEP-11::DefaultEnabled=false \
    update
}

echo "[1/8] Installing dependencies..."
export DEBIAN_FRONTEND=noninteractive
apt_update_with_fallback
apt-get install -y openjdk-17-jre-headless ca-certificates sqlite3

echo "[2/8] Ensuring service user and directories..."
if ! id -u wifichat >/dev/null 2>&1; then
  useradd --system --home /opt/wifichat --shell /usr/sbin/nologin wifichat
fi

mkdir -p "$REMOTE_DIR"
mkdir -p "$(dirname "$DB_PATH")"
mkdir -p /var/log/wifichat
mkdir -p /var/backups/wifichat
mkdir -p /etc/wifichat

chown -R wifichat:wifichat "$REMOTE_DIR" "$(dirname "$DB_PATH")" /var/log/wifichat /var/backups/wifichat

echo "[3/8] Installing app artifact..."
install -m 0644 "$DEPLOY_DIR/wifi-chat-server.jar" "$REMOTE_DIR/wifi-chat-server.jar"
chown wifichat:wifichat "$REMOTE_DIR/wifi-chat-server.jar"

echo "[4/8] Installing config files..."
cat > /etc/wifichat/server.env <<EOF
APP_PORT=$APP_PORT
DB_PATH=$DB_PATH
JAVA_OPTS=-Xms256m -Xmx512m
EOF
chmod 0644 /etc/wifichat/server.env

cat > /etc/wifichat/backup.env <<EOF
DB_PATH=$DB_PATH
BACKUP_DIR=/var/backups/wifichat
RETENTION_DAYS=14
EOF
chmod 0644 /etc/wifichat/backup.env

echo "[5/8] Installing systemd units and backup script..."
install -m 0644 "$DEPLOY_DIR/wifichat-server.service" /etc/systemd/system/wifichat-server.service
install -m 0755 "$DEPLOY_DIR/wifichat-backup.sh" /usr/local/bin/wifichat-backup.sh
install -m 0644 "$DEPLOY_DIR/wifichat-backup.service" /etc/systemd/system/wifichat-backup.service
install -m 0644 "$DEPLOY_DIR/wifichat-backup.timer" /etc/systemd/system/wifichat-backup.timer

echo "[6/8] Reloading systemd and starting services..."
systemctl daemon-reload
systemctl enable --now wifichat-server.service
systemctl restart wifichat-server.service
systemctl enable --now wifichat-backup.timer

echo "[7/8] Configuring firewall (optional)..."
if [[ "$SETUP_FIREWALL" == "1" ]]; then
  if command -v ufw >/dev/null 2>&1; then
    ufw allow OpenSSH
    ufw allow "${APP_PORT}/tcp"
    if [[ "$OPEN_UDP_DISCOVERY" == "1" ]]; then
      ufw allow "${UDP_PORT}/udp"
    fi
    ufw --force enable
  else
    echo "UFW not found; skipping firewall setup."
  fi
fi

echo "[8/8] Service status:"
systemctl --no-pager --full status wifichat-server.service || true

echo ""
echo "Done."
echo "Server: $REMOTE_DIR/wifi-chat-server.jar"
echo "DB: $DB_PATH"
echo "Port: $APP_PORT/tcp"
echo "Logs: /var/log/wifichat/server.log and /var/log/wifichat/server.err.log"
echo "Backups: /var/backups/wifichat"
