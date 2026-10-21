#!/usr/bin/env bash
set -euo pipefail
umask 077
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/backups/databases}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
[[ "$RETENTION_DAYS" =~ ^[0-9]+$ ]] && (( RETENTION_DAYS >= 2 )) || { echo "Invalid retention" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
command -v flock >/dev/null
command -v gzip >/dev/null
command -v sha256sum >/dev/null
exec 9>"$BACKUP_DIR/.backup.lock"
flock -n 9 || { echo "Another backup is running" >&2; exit 1; }

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_one() {
  local name="$1"
  shift
  local target="$BACKUP_DIR/${name}-${timestamp}.sql.gz"
  local tmp="${target}.partial"
  trap 'rm -f "$tmp"' RETURN
  "$@" | gzip -1 > "$tmp"
  test -s "$tmp"
  gzip -t "$tmp"
  mv -- "$tmp" "$target"
  sha256sum "$target" > "$target.sha256"
  echo "OK: $name ($timestamp UTC)"
}

# Passwords are read inside existing DB containers, never echoed on the host.
# Physical volume removal or database restarts are never performed.
backup_one board-mysql docker exec mysql8 sh -ec 'export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD missing}"; exec mysqldump -uroot --single-transaction --quick --routines --triggers --events --all-databases'
backup_one achat-mysql docker exec achat-prod-mysql-1 sh -ec 'export MYSQL_PWD="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD missing}"; exec mysqldump -uroot --single-transaction --quick --routines --triggers --events --all-databases'
backup_one groover-postgres docker exec groover-prod-db-1 sh -ec 'export PGPASSWORD="${POSTGRES_PASSWORD:?POSTGRES_PASSWORD missing}"; exec pg_dumpall -U "${POSTGRES_USER:-groover_admin}"'

# Retain 14 days only after all three backups have succeeded.
find "$BACKUP_DIR" -maxdepth 1 -type f \( -name 'board-mysql-*.sql.gz' -o -name 'achat-mysql-*.sql.gz' -o -name 'groover-postgres-*.sql.gz' -o -name '*.sql.gz.sha256' \) -mtime +"$RETENTION_DAYS" -delete
echo "All database logical backups complete"
