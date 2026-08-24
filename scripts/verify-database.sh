#!/usr/bin/env bash

set -euo pipefail

db_name="${DB_NAME:-companyx}"
db_username="${DB_USERNAME:-companyx}"

docker compose exec -T postgres psql \
  -v ON_ERROR_STOP=1 \
  -U "${db_username}" \
  -d "${db_name}" \
  -c "SELECT extname FROM pg_extension WHERE extname = 'vector';" \
  -c "SELECT 'departments' AS table_name, COUNT(*) AS row_count FROM departments
      UNION ALL SELECT 'employees', COUNT(*) FROM employees
      UNION ALL SELECT 'clients', COUNT(*) FROM clients
      UNION ALL SELECT 'products', COUNT(*) FROM products
      UNION ALL SELECT 'contracts', COUNT(*) FROM contracts
      UNION ALL SELECT 'projects', COUNT(*) FROM projects
      UNION ALL SELECT 'sales', COUNT(*) FROM sales
      UNION ALL SELECT 'support_tickets', COUNT(*) FROM support_tickets
      UNION ALL SELECT 'document_chunks', COUNT(*) FROM document_chunks
      ORDER BY table_name;" \
  -c "SELECT conname AS constraint_name,
             conrelid::regclass AS table_name,
             confrelid::regclass AS referenced_table
      FROM pg_constraint
      WHERE contype = 'f'
      ORDER BY table_name::text, constraint_name;"
