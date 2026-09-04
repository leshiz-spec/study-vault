#!/bin/sh

# Create a timestamped PostgreSQL custom-format backup and prune old backups.
set -eu
umask 077

: "${BACKUP_DIR:?BACKUP_DIR must be set}"
: "${PGHOST:?PGHOST must be set}"
: "${PGPORT:?PGPORT must be set}"
: "${PGDATABASE:?PGDATABASE must be set}"
: "${PGUSER:?PGUSER must be set}"

BACKUP_PREFIX=${BACKUP_PREFIX:-studyvault}
RETENTION_DAYS=${RETENTION_DAYS:-14}

case "$BACKUP_PREFIX" in
  ''|*[!A-Za-z0-9._-]*)
    echo "Backup failed: BACKUP_PREFIX may contain only letters, numbers, dot, underscore, and hyphen" >&2
    exit 1
    ;;
esac
case "$RETENTION_DAYS" in
  ''|*[!0-9]*)
    echo "Backup failed: RETENTION_DAYS must be a non-negative integer" >&2
    exit 1
    ;;
esac

if ! command -v pg_dump >/dev/null 2>&1; then
  echo "Backup failed: pg_dump is not installed or not on PATH" >&2
  exit 1
fi

mkdir -p -- "$BACKUP_DIR"
if [ ! -d "$BACKUP_DIR" ] || [ ! -w "$BACKUP_DIR" ]; then
  echo "Backup failed: BACKUP_DIR is not a writable directory: $BACKUP_DIR" >&2
  exit 1
fi

timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$BACKUP_DIR/${BACKUP_PREFIX}_${timestamp}.dump"

echo "Creating PostgreSQL backup: $backup_file"
if ! pg_dump --format=custom --file="$backup_file" --host="$PGHOST" --port="$PGPORT" --username="$PGUSER" --dbname="$PGDATABASE"; then
  rm -f -- "$backup_file"
  echo "Backup failed: pg_dump could not dump $PGDATABASE@$PGHOST:$PGPORT" >&2
  exit 1
fi

if [ ! -s "$backup_file" ]; then
  rm -f -- "$backup_file"
  echo "Backup failed: pg_dump produced an empty file" >&2
  exit 1
fi

# Retention is deliberately scoped to direct children matching this script's
# prefix, so unrelated files and paths outside BACKUP_DIR are untouched.
find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type f \
  -name "${BACKUP_PREFIX}_*.dump" -mtime "+$RETENTION_DAYS" -exec rm -f -- {} +

echo "Backup complete: $backup_file"
