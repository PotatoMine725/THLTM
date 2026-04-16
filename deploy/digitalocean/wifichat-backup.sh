#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="/etc/wifichat/backup.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  source "$ENV_FILE"
fi

DB_PATH="${DB_PATH:-/var/lib/wifichat/chat.db}"
BACKUP_DIR="${BACKUP_DIR:-/var/backups/wifichat}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

if [[ ! -f "$DB_PATH" ]]; then
  echo "Database not found: $DB_PATH"
  exit 0
fi

mkdir -p "$BACKUP_DIR"

timestamp="$(date -u +%Y%m%d-%H%M%S)"
backup_path="$BACKUP_DIR/chat-${timestamp}.db"

sqlite3 "$DB_PATH" <<SQL
.timeout 5000
.backup '$backup_path'
SQL

gzip -f "$backup_path"

find "$BACKUP_DIR" -type f -name "chat-*.db.gz" -mtime +"$RETENTION_DAYS" -delete
echo "Backup created: ${backup_path}.gz"
