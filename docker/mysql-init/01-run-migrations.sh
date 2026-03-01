#!/bin/bash
set -e
echo "[mysql-init] Running V1 base schema..."
mysql -uroot < /migrations/V1__base_schema.sql
for v in 2 3 4 5 6 7 8 9 10 11 12; do
  for f in /migrations/V${v}__*.sql; do
    if [ -f "$f" ]; then
      echo "[mysql-init] Running $f..."
      mysql -uroot realmcrafter < "$f"
      break
    fi
  done
done
echo "[mysql-init] Done."
