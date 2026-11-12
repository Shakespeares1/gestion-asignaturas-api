-- Inspección estrictamente de lectura. Ejecutar en la base Supabase esperada.
-- No contiene DDL/DML; la transacción READ ONLY y ROLLBACK son defensas adicionales.
BEGIN TRANSACTION READ ONLY;
SET LOCAL statement_timeout = '15s';

SELECT current_database() AS database_name,
       current_schema() AS current_schema,
       current_setting('server_version') AS postgres_version,
       current_setting('transaction_read_only') AS transaction_is_read_only;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name IN ('asignaturas', 'usuarios', 'flyway_schema_history')
ORDER BY table_name;

SELECT table_name, ordinal_position, column_name, data_type,
       character_maximum_length, datetime_precision, is_nullable,
       column_default, is_identity, identity_generation
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('asignaturas', 'usuarios')
ORDER BY table_name, ordinal_position;

SELECT tbl.relname AS table_name, con.conname AS constraint_name,
       CASE con.contype
           WHEN 'p' THEN 'PRIMARY KEY'
           WHEN 'f' THEN 'FOREIGN KEY'
           WHEN 'u' THEN 'UNIQUE'
           WHEN 'c' THEN 'CHECK'
           WHEN 'x' THEN 'EXCLUSION'
           ELSE con.contype::text
       END AS constraint_type,
       pg_get_constraintdef(con.oid, true) AS definition
FROM pg_constraint con
JOIN pg_class tbl ON tbl.oid = con.conrelid
JOIN pg_namespace ns ON ns.oid = tbl.relnamespace
WHERE ns.nspname = 'public'
  AND tbl.relname IN ('asignaturas', 'usuarios')
ORDER BY tbl.relname, con.conname;

SELECT tablename AS table_name, indexname AS index_name, indexdef AS definition
FROM pg_indexes
WHERE schemaname = 'public'
  AND tablename IN ('asignaturas', 'usuarios')
ORDER BY tablename, indexname;

-- La tabla de historial se consulta solo si aparece en el listado de tablas anterior.
-- Resultado esperado según el estado operativo informado: baseline 1 y V2 success.
SELECT installed_rank, version, description, type, script, checksum, installed_on, success
FROM public.flyway_schema_history
ORDER BY installed_rank;

ROLLBACK;
