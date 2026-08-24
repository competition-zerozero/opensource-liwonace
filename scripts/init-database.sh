#!/usr/bin/env bash

set -euo pipefail

: "${DATASET_ROOT:?DATASET_ROOT 환경변수를 설정해야 합니다.}"

db_name="${DB_NAME:-companyx}"
db_username="${DB_USERNAME:-companyx}"

schema_file="${DATASET_ROOT}/sql/01-schema.sql"
data_file="${DATASET_ROOT}/sql/02-data.sql"

if [[ ! -f "${schema_file}" || ! -f "${data_file}" ]]; then
  echo "데이터셋 SQL 파일을 찾을 수 없습니다: ${DATASET_ROOT}/sql" >&2
  exit 1
fi

docker compose up -d postgres

for attempt in {1..30}; do
  if docker compose exec -T postgres pg_isready -U "${db_username}" -d "${db_name}" \
    >/dev/null 2>&1; then
    break
  fi

  if [[ "${attempt}" -eq 30 ]]; then
    echo "PostgreSQL이 준비되지 않았습니다." >&2
    exit 1
  fi

  sleep 1
done

docker compose exec -T postgres psql \
  -v ON_ERROR_STOP=1 \
  -U "${db_username}" \
  -d "${db_name}" \
  -f - < "${schema_file}"

docker compose exec -T postgres psql \
  -v ON_ERROR_STOP=1 \
  -U "${db_username}" \
  -d "${db_name}" \
  -f - < "${data_file}"

echo "Company-X 데이터베이스 초기화와 데이터 적재가 완료되었습니다."
