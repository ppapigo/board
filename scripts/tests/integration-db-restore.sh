#!/usr/bin/env bash
set -euo pipefail
# Disposable databases only. Do not run this against production networks or volumes.
suffix="$(date +%s)-$$"
mysql_name="ci-restore-mysql-$suffix"
pg_name="ci-restore-postgres-$suffix"
tmp="$(mktemp -d)"
cleanup() {
  docker rm -f "$mysql_name" "$pg_name" >/dev/null 2>&1 || true
  rm -rf "$tmp"
}
trap cleanup EXIT

docker run -d --name "$mysql_name" -e MYSQL_ROOT_PASSWORD=ci_disposable_password_only -e MYSQL_DATABASE=ci_restore mysql:8.4 >/dev/null
for i in $(seq 1 60); do
  if docker exec "$mysql_name" mysqladmin ping -uroot -pci_disposable_password_only --silent >/dev/null 2>&1; then break; fi
  sleep 2
done
docker exec "$mysql_name" mysql -uroot -pci_disposable_password_only -D ci_restore -e 'CREATE TABLE restore_probe (id INT PRIMARY KEY, value_text VARCHAR(64)); INSERT INTO restore_probe VALUES (1, "persisted_mysql");' 2>/dev/null
docker exec "$mysql_name" mysqldump -uroot -pci_disposable_password_only --single-transaction ci_restore 2>"$tmp/mysql-warnings" >"$tmp/mysql.sql"
test -s "$tmp/mysql.sql"
docker exec "$mysql_name" mysql -uroot -pci_disposable_password_only -e 'DROP DATABASE ci_restore; CREATE DATABASE ci_restore;' 2>/dev/null
docker exec -i "$mysql_name" mysql -uroot -pci_disposable_password_only ci_restore <"$tmp/mysql.sql" 2>/dev/null
mysql_count="$(docker exec "$mysql_name" mysql -uroot -pci_disposable_password_only -N -D ci_restore -e 'SELECT COUNT(*) FROM restore_probe WHERE value_text = "persisted_mysql";' 2>/dev/null)"
test "$mysql_count" = 1
echo 'PASS: disposable MySQL logical backup and restore'

docker run -d --name "$pg_name" -e POSTGRES_PASSWORD=ci_disposable_password_only -e POSTGRES_DB=ci_restore postgres:17-alpine >/dev/null
for i in $(seq 1 60); do
  if docker exec "$pg_name" pg_isready -U postgres -d ci_restore >/dev/null 2>&1; then break; fi
  sleep 2
done
docker exec "$pg_name" psql -U postgres -d ci_restore -v ON_ERROR_STOP=1 -c "CREATE TABLE restore_probe(id INT PRIMARY KEY, value_text TEXT); INSERT INTO restore_probe VALUES (1, 'persisted_postgres');" >/dev/null
docker exec "$pg_name" pg_dump -U postgres -d ci_restore >"$tmp/postgres.sql"
test -s "$tmp/postgres.sql"
docker exec "$pg_name" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c 'DROP DATABASE ci_restore WITH (FORCE);' >/dev/null
docker exec "$pg_name" psql -U postgres -d postgres -v ON_ERROR_STOP=1 -c 'CREATE DATABASE ci_restore;' >/dev/null
docker exec -i "$pg_name" psql -U postgres -d ci_restore -v ON_ERROR_STOP=1 <"$tmp/postgres.sql" >/dev/null
pg_count="$(docker exec "$pg_name" psql -U postgres -d ci_restore -Atc "SELECT COUNT(*) FROM restore_probe WHERE value_text='persisted_postgres';")"
test "$pg_count" = 1
echo 'PASS: disposable PostgreSQL logical backup and restore'
