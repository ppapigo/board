# Application DB backups and rollback

## Scope
- MySQL in `mysql8` (MOA), MySQL in `achat-prod-mysql-1`, and PostgreSQL in `groover-prod-db-1`.
- Backups are three independent *logical* dumps: no Docker volume removal and no database restart.
- Default destination is `/home/ubuntu/backups/databases`, permission 0700; backup files 0600; 14-day retention.
- Includes SQL data and role definitions (PostgreSQL); **does not include MOA uploaded media, TLS credentials or .env secrets**. Back up MOA upload volume separately.
- Local backup is **not disaster recovery** if the Lightsail disk is lost. Encrypt and transfer verified copies off-host; limit access, test restoration in an isolated disposable database, and monitor disk usage.
- Backup of each MySQL database uses `--single-transaction`. Concurrent DDL can compromise logical consistency, and non-transactional tables are not guaranteed consistent. Coordinate production schema migrations.
- PostgreSQL `pg_dumpall` emits roles and database contents; treat dumps as sensitive.
- If a dump fails, the command exits nonzero, leaves no published partial success artifact, and keeps existing backups intact.

## Install (manual, only after verifying scripts against server container versions)
From `~/board`, verify and commit/pull scripts before enabling. Copy systemd unit files from `scripts/ops/systemd/` to `/etc/systemd/system/` using sudo, review paths and service account first, then:

```bash
sudo systemctl daemon-reload
sudo systemctl start app-database-backup.service
sudo systemctl status app-database-backup.service --no-pager
sudo systemctl enable --now app-database-backup.timer
systemctl list-timers --all | grep app-database-backup
```

Stop at any nonzero error. Inspect `journalctl -u app-database-backup.service -n 80 --no-pager` and verify all three `.sql.gz` archives and matching SHA256 sums. Never paste SQL backups or secrets into chat/GitHub.

## Rollback semantics
On a failed application container healthcheck or HTTPS smoke check, CI attempts a rollback to the previously running app image IDs, with dependencies excluded. CI deliberately reports failure even if rollback succeeds.

- A rollback does **not** reverse database migrations or user data writes.
- A rollback can fail if schema or client protocol is no longer compatible with the old image.
- Concurrent pushes to other repositories are separately serialized in their own GitHub Actions; coordinate database changes and maintenance windows.
- Run a real login, write, read, WebSocket and restore drill after initial activation.
