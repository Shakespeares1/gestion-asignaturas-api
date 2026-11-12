# Inspección de Supabase sin escrituras

No se necesita compartir credenciales en el chat. La base ya fue verificada anteriormente con Flyway y Hibernate según el registro operativo; esta revisión de catálogos es una comprobación adicional y no repite `baseline`, `migrate` ni validación CRUD.

## Opción recomendada: conexión local con `psql`

Use una instalación local de PostgreSQL que incluya `psql`. En una ventana nueva de PowerShell configure solo la URL sin contraseña embebida y el usuario. El siguiente ejemplo usa el host/usuario de la conexión PostgreSQL de Supabase; obtenga esos valores desde el panel autorizado:

```powershell
$env:DB_URL = 'postgresql://<host-supabase>:5432/postgres?sslmode=require'
$env:DB_USER = '<usuario-postgres>'
psql --dbname $env:DB_URL --username $env:DB_USER --password --no-psqlrc --set ON_ERROR_STOP=on --file .\ops\supabase-read-only-inspection.sql
Remove-Item Env:DB_URL, Env:DB_USER -ErrorAction SilentlyContinue
```

`psql` solicita la contraseña de forma interactiva y no la muestra. No la añada a `DB_URL`, al comando ni a un archivo. Las variables existen solo en esa ventana; elimínelas al terminar. Si no tiene `psql`, puede instalar PostgreSQL client tools o ejecutar las sentencias del archivo en el SQL Editor de Supabase, seleccionando primero el proyecto/base correcta.

## Qué revisar en el resultado

- `transaction_is_read_only` debe ser `on` y la versión del servidor debe identificar la base esperada.
- Deben aparecer las tablas `asignaturas`, `usuarios` y `flyway_schema_history`.
- Compare tipos, límites, nulabilidad, identity y defaults de cada columna con `V1__create_asignaturas.sql`, `V2__create_usuarios.sql` y las entidades JPA.
- Compare definiciones de `CHECK`, `UNIQUE`, primary key e índices con las migraciones.
- El historial Flyway debe mostrar baseline en versión `1` y versión `2` aplicada con `success = true`; no ejecute acciones Flyway desde esta inspección.
- Si falta una tabla, hay diferencias o el historial no corresponde, deténgase y preserve la salida para revisión. No intente repararlo desde esta sesión.

El script abre una transacción de solo lectura y termina con `ROLLBACK`. No consulta filas de negocio ni modifica objetos ni datos. No se ejecuta mientras no haya una sesión local con acceso.
