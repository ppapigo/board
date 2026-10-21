#!/usr/bin/env bash
set -euo pipefail
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="$repo_root/scripts/ops/backup-databases.sh"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin"
cat > "$tmp/bin/docker" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
if [[ "$*" == *"mysql8"* ]]; then
  printf '%s\n' 'CREATE DATABASE board;'
elif [[ "$*" == *"achat-prod-mysql-1"* ]]; then
  if [[ "${FAIL_ACHAT:-0}" == 1 ]]; then
    printf 'incomplete dump\n'
    exit 42
  fi
  printf '%s\n' 'CREATE DATABASE achat;'
elif [[ "$*" == *"groover-prod-db-1"* ]]; then
  printf '%s\n' 'CREATE DATABASE groover;'
else
  exit 2
fi
MOCK
chmod +x "$tmp/bin/docker"
export BACKUP_DIR="$tmp/backups" RETENTION_DAYS=14
PATH="$tmp/bin:$PATH" bash "$script" >/dev/null
test "$(find "$BACKUP_DIR" -type f -name '*.sql.gz' | wc -l)" -eq 3
test "$(find "$BACKUP_DIR" -type f -name '*.sha256' | wc -l)" -eq 3
for file in "$BACKUP_DIR"/*.sql.gz; do
  gzip -t "$file"
done
(cd "$BACKUP_DIR" && sha256sum -c -- *.sha256 >/dev/null)
echo "PASS: backup success + gzip checksums"
rm -f "$BACKUP_DIR"/*.sql.gz "$BACKUP_DIR"/*.sha256
status=0
FAIL_ACHAT=1 PATH="$tmp/bin:$PATH" bash "$script" >"$tmp/failure.log" 2>&1 || status=$?
test "$status" -ne 0
test "$(find "$BACKUP_DIR" -type f -name '*.sql.gz' | wc -l)" -eq 1
test "$(find "$BACKUP_DIR" -type f -name '*.partial' | wc -l)" -eq 0
echo "PASS: backup abort on failed DB + no published partial archive"
