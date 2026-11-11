# gestion-asignaturas-api

API REST para la gestión académica de asignaturas, desarrollada con Java 21, Spring Boot 4.1.2, arquitectura hexagonal y PostgreSQL.

## Conexión a PostgreSQL / Supabase

La aplicación lee `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del entorno. No almacenes credenciales en el repositorio. Usa `.env.example` como referencia; Spring Boot no carga ese archivo automáticamente.

El perfil predeterminado valida el esquema existente. Flyway está habilitado globalmente y puede ejecutar las migraciones al iniciar la aplicación. `dev` permite que Hibernate actualice el esquema y debe usarse solo con una base local desechable. `test` usa H2 en memoria con Flyway y Hibernate `validate`. `prod` usa `validate` y exige PostgreSQL con `sslmode=require`. Nunca uses `dev` en Render ni frente a Supabase/esquemas compartidos.

En PowerShell, define las variables solo para la terminal actual antes de iniciar:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/gestion?sslmode=disable"
$env:DB_USERNAME = "<usuario-local>"
$env:DB_PASSWORD = "<contraseña-local>"
$env:SPRING_PROFILES_ACTIVE = "dev"
$bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
[Array]::Clear($bytes, 0, $bytes.Length)
.\mvnw.cmd spring-boot:run
```

Usa exclusivamente una base local desechable con `dev`: `ddl-auto=update` puede alterar su esquema. Para probar sin base local, las pruebas usan H2:

```powershell
$env:RUN_POSTGRES_INTEGRATION = "false"
.\mvnw.cmd clean test
```

No conectes la aplicación a la base Supabase existente hasta inspeccionar y respaldar el esquema y sus datos. En producción Flyway puede ejecutar migraciones al iniciar antes de que Hibernate valide; `ddl-auto=validate` no impide que Flyway cambie el esquema. Conserva `baseline-on-migrate=false` y sigue el procedimiento de adopción descrito abajo antes de cualquier conexión.

## Migraciones versionadas con Flyway

Flyway aplica `classpath:db/migration` antes de que Hibernate ejecute `ddl-auto=validate`. El perfil de pruebas usa H2 en modo PostgreSQL, también ejecuta estas migraciones y valida después las entidades. En producción no se usa `ddl-auto=update`; Flyway es el mecanismo de cambio del esquema y `flyway.clean` está deshabilitado.

Migraciones iniciales:

- `V1__create_asignaturas.sql`: `asignaturas`, ID identity `BIGINT`, campos de texto/`VARCHAR` e enteros según `AsignaturaEntity`, reglas de créditos y semestre positivos e índices por semestre/créditos.
- `V2__create_usuarios.sql`: `usuarios`, ID identity `BIGINT`, email único, enums persistidos como `VARCHAR(16)` con checks, timestamps `TIMESTAMP(6) WITH TIME ZONE`, pareja de recuperación nullable atómica e índices por rol/estado y fecha.

Flyway está habilitado globalmente, valida las migraciones y puede ejecutarlas durante el arranque. Hibernate usa `ddl-auto=validate` en producción, después de la preparación de esquema por Flyway; `validate` por sí solo no evita DDL de Flyway. Flyway no crea baseline automáticamente (`baseline-on-migrate=false`), y debe conservarse así. Una base vacía de pruebas recibe V1 y V2 al iniciar. **No conectes la aplicación a Supabase existente hasta respaldar e inspeccionar su esquema y datos.** Flyway debe detenerse ante un esquema no vacío sin historial; no cambies esa protección para forzar el inicio.

Para adoptar un esquema existente, primero toma y verifica un respaldo, inspecciona el esquema y compara en una copia aislada todas las columnas, tipos, longitudes, nulabilidad, identity, índices, restricciones y datos con V1/V2. Verifica además emails normalizados y sin duplicados, roles/estados válidos, timestamps presentes y tokens de recuperación emparejados. Solo si la estructura existente coincide exactamente con el resultado de V1+V2, un administrador puede ejecutar un procedimiento manual, revisado y aprobado para registrar baseline Flyway en versión 2; esta acción marca V1/V2 como representadas y permite aplicar desde V3 en adelante. La línea base no crea tablas, valida ni corrige el esquema. Si existe cualquier diferencia, no ejecutes baseline: diseña, prueba y revisa una migración de transición en una copia aislada. No se ejecutó baseline ni migración remota en esta fase.

## API de asignaturas

- `POST /api/asignaturas`
- `GET /api/asignaturas`
- `GET /api/asignaturas/{id}`
- `PUT /api/asignaturas/{id}`
- `DELETE /api/asignaturas/{id}`

Las solicitudes de creación y actualización requieren `nombre`, `nombreCompleto`, `numeroCreditos` y `semestre`; créditos y semestre deben ser mayores que cero.

## API de usuarios

- `POST /api/usuarios` es registro público y siempre crea un usuario `USER`/`ACTIVO`.
- Listar, consultar, actualizar, eliminar y cambiar acceso (`PATCH /api/usuarios/{id}/access`) requieren `ADMIN`.
- `PUT /api/usuarios/{id}` solo cambia `nombreCompleto` y `email`.
- Solo ADMIN puede cambiar rol/estado mediante `PATCH /api/usuarios/{id}/access`.

## Autenticación y recuperación

- `POST /api/auth/login` recibe `email` y `password`; devuelve un Bearer JWT y datos públicos del usuario.
- `POST /api/auth/forgot-password` recibe `email`; responde de forma genérica. Si falta la configuración SMTP requerida, devuelve `503` sin consultar si el correo existe.
- `POST /api/auth/reset-password` recibe `token` y `newPassword`; los tokens vencidos o reutilizados se rechazan.
- Registro y restablecimiento exigen al menos 12 caracteres, rechazan valores vacíos o compuestos por espacios y limitan la entrada a 72 bytes UTF-8 compatibles con BCrypt. No se exigen mayúsculas, números ni símbolos. El login conserva compatibilidad con contraseñas históricas más cortas; la nueva política no bloquea esas cuentas.
- La clave JWT debe establecerse como `JWT_SECRET_BASE64` con al menos 32 bytes aleatorios codificados en Base64. La aplicación no tiene una clave predeterminada y no inicia sin ella fuera del perfil de pruebas.
- En PowerShell, genera la clave directamente en la variable de entorno sin imprimirla ni guardarla en un archivo:

```powershell
$bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(32)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
[Array]::Clear($bytes, 0, $bytes.Length)
$env:JWT_ISSUER = "gestion-academica"
$env:JWT_TTL_SECONDS = "900"
```

- Correo: configura `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` y `APP_MAIL_RESET_URL`. STARTTLS está habilitado y es obligatorio, con verificación de identidad del servidor; en `prod`, cualquier enlace de recuperación configurado debe usar HTTPS. Si la configuración no puede garantizar TLS, la recuperación queda no disponible y no intenta enviar. Los secretos solo deben existir en el entorno del proceso. Las pruebas no contactan SMTP.
- ADMIN gestiona usuarios y asignaturas. USER solo consulta asignaturas. El alta pública siempre asigna USER/ACTIVO aunque el cliente incluya campos adicionales. No hay una cuenta predeterminada; el primer ADMIN se crea mediante el runner local `admin-bootstrap`, nunca por HTTP. Consulta [OPERATIONS.md](OPERATIONS.md) y ejecútalo únicamente después de revisar y preparar la base objetivo.
- No se exponen hashes de contraseña ni de recuperación. La respuesta genérica de recuperación no confirma que el correo se haya entregado.

No despliegues la API públicamente hasta revisar/adoptar el esquema, ejecutar de forma controlada el bootstrap del primer ADMIN, configurar la clave JWT y el SMTP, y completar las migraciones necesarias.

El perfil `admin-bootstrap` solicita la contraseña con entrada de consola sin eco y bloquea la inicialización con un advisory lock transaccional de PostgreSQL. La operación no se ejecutó durante esta fase. Sus instrucciones y precondiciones están en [OPERATIONS.md](OPERATIONS.md).

## Reportes parametrizados

Todos los reportes devuelven `items`, `page`, `size`, `totalElements` y `totalPages`. La página predeterminada es `0`, el tamaño predeterminado es `50` y el máximo es `100` registros por petición. Los filtros se ejecutan en la base de datos y se pueden combinar.

### Asignaturas: carrera, semestre y área

`GET /api/reportes/asignaturas/carrera-semestre-area`

Parámetros opcionales: `carrera` (coincidencia exacta sin distinguir mayúsculas), `semestre` (exacto y positivo), `areaConocimiento` (contiene, sin distinguir mayúsculas). Ejemplo:

```text
GET /api/reportes/asignaturas/carrera-semestre-area?carrera=Ingenieria%20de%20Software&semestre=5&areaConocimiento=sistemas
```

### Asignaturas: créditos, profesor y nombre

`GET /api/reportes/asignaturas/creditos-profesor-nombre`

Parámetros opcionales: `numeroCreditosMin`, `numeroCreditosMax` (límites inclusivos y positivos), `profesor` y `nombre` (coincidencia parcial sin distinguir mayúsculas). Devuelve `400` si el mínimo supera el máximo. Ejemplo:

```text
GET /api/reportes/asignaturas/creditos-profesor-nombre?numeroCreditosMin=3&numeroCreditosMax=5&profesor=ana&nombre=datos
```

### Usuarios: rol y estado

`GET /api/reportes/usuarios/rol-estado`

Parámetros opcionales: `role` (`ADMIN` o `USER`) y `accountStatus` (`ACTIVO` o `INACTIVO`). Son combinables y el endpoint requiere `ADMIN`. Ejemplo:

```text
GET /api/reportes/usuarios/rol-estado?role=USER&accountStatus=ACTIVO
```

### Usuarios: fecha de registro

`GET /api/reportes/usuarios/fecha-registro`

Parámetros opcionales `fechaDesde` y `fechaHasta` con formato `YYYY-MM-DD`. El rango incluye ambos días; el inicio/final se evalúa en UTC. Devuelve `400` si la fecha inicial es posterior a la final. Requiere `ADMIN`. Ejemplo:

```text
GET /api/reportes/usuarios/fecha-registro?fechaDesde=2026-01-01&fechaHasta=2026-03-31&page=0&size=25
```

Los reportes de asignaturas requieren JWT de ADMIN o USER. Los reportes de usuarios requieren JWT de ADMIN; sus respuestas reutilizan `UsuarioResponse` y no incluyen contraseñas, hashes ni tokens.

## Límites de frecuencia

El limitador singleton usa ventanas fijas en memoria, responde `429` con `Retry-After` y no guarda contraseñas ni correos en sus claves (usa HMAC-SHA-256 con una clave aleatoria local a la instancia). Límites actuales:

| Operación | Por IP | Por combinación IP + cuenta |
|---|---:|---:|
| Login | 30 cada 10 min | 8 cada 10 min |
| Solicitud de recuperación | 10 por hora | 3 por hora |
| Restablecimiento | 12 cada 15 min | — |
| Registro | 10 por hora | 3 por hora |

La cuenta se normaliza antes de limitar. El límite de cuenta se combina con la IP; así un origen no consume el límite de esa cuenta para otros orígenes. La clave del mapa es un HMAC con clave aleatoria en memoria, nunca el correo en claro ni un hash determinista. El almacenamiento tiene un máximo de 50 000 ventanas activas y limpia las expiradas periódicamente. La prueba de frecuencia se mantiene entre solicitudes de una misma instancia, pero se reinicia al reiniciar el proceso. En `prod`, se usa `CF-Connecting-IP` (sobrescrito por la capa Cloudflare de Render); no se usa `X-Forwarded-For`. La aplicación ignora los encabezados forwarded de proxy y publica Swagger con una URL relativa. Si se despliega fuera de Render, configura una resolución de IP confiable antes de producción. Con varias instancias los contadores no se comparten; antes de escalar, sustituir este adaptador por un almacén compartido (por ejemplo Redis con una operación atómica y TTL) y conservar los mismos ámbitos/umbrales.

## OpenAPI y Swagger UI

- UI local: `http://localhost:8080/swagger-ui.html` cuando la aplicación corre con perfil `dev`.
- Especificación JSON: `http://localhost:8080/v3/api-docs` (YAML: `/v3/api-docs.yaml`).
- En Render las rutas esperadas son `https://<nombre-del-servicio>.onrender.com/swagger-ui.html` y `/v3/api-docs`.
- Pulsa **Authorize** e introduce el JWT válido (Swagger agrega el esquema Bearer). El login y el registro son públicos; el resto de las operaciones protegidas responde según las reglas ADMIN/USER.
- La definición OpenAPI/Swagger UI es pública y no contiene credenciales. Las operaciones de usuarios requieren ADMIN; las escrituras de asignaturas requieren ADMIN.

## Preparación para Render (sin servicio creado)

Render no ofrece runtime Java nativo; se prepara una imagen Docker basada en Maven/Temurin Java 21. `Dockerfile` construye con `RUN_POSTGRES_INTEGRATION=false ./mvnw -B clean verify`; el stage de ejecución inicia con `java -jar /app/app.jar`. `render.yaml` fija `numInstances: 1` para que el limitador en memoria tenga estado coherente en el despliegue inicial. Producción ignora encabezados forwarded (`server.forward-headers-strategy=none`); la API usa `CF-Connecting-IP` para los límites y Swagger declara una URL de servidor relativa. No se ha creado ni desplegado el servicio remoto.

El archivo `render.yaml` sirve como blueprint para importar el repositorio GitHub `Shakespeares1/gestion-asignaturas-api`, rama `main`, con auto deploy por commit y perfil `prod`. Al crear el servicio, Render debe vincular el repositorio y aplicar ese blueprint. Render inyecta `PORT`; la aplicación usa ese valor, con fallback `10000`, y escucha en `0.0.0.0`. El health check queda en `/actuator/health`.

Configura en Render:

- Obligatorios: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL` (PostgreSQL y `sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64` (al menos 32 bytes aleatorios Base64). El blueprint genera `JWT_SECRET_BASE64` de 256 bits.
- Opcionales: `JWT_ISSUER` (predeterminado `gestion-academica`), `JWT_TTL_SECONDS` (900; rango 60–86400), `CORS_ALLOWED_ORIGINS` (orígenes HTTPS explícitos separados por coma; vacío deshabilita CORS para navegadores).
- Para habilitar recuperación por correo: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` y `APP_MAIL_RESET_URL` con esquema HTTPS en producción. STARTTLS y su requisito deben estar habilitados (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true`) y validar la identidad del host (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_CHECKSERVERIDENTITY=true`). Auth/STARTTLS y timeouts SMTP están en `.env.example`. Si faltan estos ajustes, recuperación responde `503`; no se simula un envío. No configures SMTP hasta disponer de un proveedor real.
- No configures `PORT` manualmente salvo que Render lo solicite; la plataforma lo entrega. No configures el perfil `dev`.

`render.yaml` usa variables `sync: false` para DB_URL/usuario/contraseña, y la clave JWT generada por Render. La conexión GitHub, la carga de secretos, creación de servicio y despliegue requieren acciones posteriores en el dashboard; nada de eso se realizó en esta etapa.

Las pruebas normales usan el perfil `test` aislado de Supabase mediante H2 en memoria, no requieren una clave JWT y no envían correo. La prueba PostgreSQL requiere `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RUN_POSTGRES_INTEGRATION=true` y `SPRING_PROFILES_ACTIVE=postgres-integration`. Ejecútala solo contra una base de pruebas aislada: crea, actualiza y elimina un registro temporal.

## Verificación más reciente

La última ejecución local de `mvnw.cmd -o clean test` con `RUN_POSTGRES_INTEGRATION=false` terminó con `BUILD SUCCESS`: 99 pruebas totales, 98 aprobadas, 0 fallos, 0 errores y 1 integración PostgreSQL omitida intencionalmente. Incluye pruebas MockMvc de límites, resolución de IP sin confiar en `X-Forwarded-For`, validación del perfil de bootstrap, política de contraseñas y configuración segura de SMTP/URL. No valida PostgreSQL real, el advisory lock en una base PostgreSQL, un servidor SMTP real ni la conexión con Supabase.
