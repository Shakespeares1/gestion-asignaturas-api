# gestion-asignaturas-api

API REST para la gestión académica de asignaturas, desarrollada con Java 21, Spring Boot 4.1.2, arquitectura hexagonal y PostgreSQL.

## Conexión a PostgreSQL

La aplicación lee `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del entorno. No almacenes credenciales en el repositorio. Usa `.env.example` como referencia; Spring Boot no carga ese archivo automáticamente.

El perfil `prod` exige PostgreSQL con `sslmode=require`, ejecuta las migraciones versionadas de Flyway y luego Hibernate valida el esquema. Está preparado para la base vacía nueva declarada en `render.yaml`. No conectes este perfil a una base preexistente sin revisar su esquema y el historial de migraciones. Nunca uses `dev` en Render.

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

En producción, Flyway conserva `baseline-on-migrate=false` y `clean-disabled=true`: una base vacía recibe V1 y V2, mientras una base cuyo historial no coincide detiene el arranque para revisión.

## Migraciones versionadas con Flyway

Flyway aplica `classpath:db/migration` en desarrollo, pruebas y en el primer arranque de producción. El perfil de pruebas usa H2 en modo PostgreSQL y valida después las entidades. Hibernate usa `ddl-auto=validate`; `flyway.clean` está deshabilitado.

Migraciones iniciales:

- `V1__create_asignaturas.sql`: `asignaturas`, ID identity `BIGINT`, campos de texto/`VARCHAR` e enteros según `AsignaturaEntity`, reglas de créditos y semestre positivos e índices por semestre/créditos.
- `V2__create_usuarios.sql`: `usuarios`, ID identity `BIGINT`, email único, enums persistidos como `VARCHAR(16)` con checks, timestamps `TIMESTAMP(6) WITH TIME ZONE`, pareja de recuperación nullable atómica e índices por rol/estado y fecha.

La base nueva de Render recibe V1 y V2 al iniciar la API. `baseline-on-migrate=false` y `clean-disabled=true` se conservan; Flyway no crea un baseline automático ni borra datos.

Para adoptar un esquema existente, primero toma y verifica un respaldo, inspecciona el esquema y compara en una copia aislada todas las columnas, tipos, longitudes, nulabilidad, identity, índices, restricciones y datos con V1/V2. Verifica además emails normalizados y sin duplicados, roles/estados válidos, timestamps presentes y tokens de recuperación emparejados. La línea base no crea tablas, valida ni corrige el esquema. Si existe cualquier diferencia, diseña, prueba y revisa una migración de transición en una copia aislada. La base nueva del Blueprint no requiere baseline.

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

Antes de publicar, confirma el esquema V2, configura la clave JWT y ejecuta de forma controlada el bootstrap del primer ADMIN. En producción Flyway queda deshabilitado; cualquier migración futura debe completarse aparte antes de desplegar la versión que la necesite. La API puede arrancar sin SMTP, pero recuperación de contraseña responde `503` hasta configurar el proveedor y la URL HTTPS de restablecimiento.

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

La cuenta se normaliza antes de limitar. El límite de cuenta se combina con la IP; así un origen no consume el límite de esa cuenta para otros orígenes. La clave del mapa es un HMAC con clave aleatoria en memoria, nunca el correo en claro ni un hash determinista. El almacenamiento tiene un máximo de 50 000 ventanas activas y limpia las expiradas periódicamente. La prueba de frecuencia se mantiene entre solicitudes de una misma instancia, pero se reinicia al reiniciar el proceso. En `prod`, `ClientIpResolver` ignora `CF-Connecting-IP` y solo lee `X-Forwarded-For` cuando `request.getRemoteAddr()` pertenece a una red configurada explícitamente en `APP_PROXY_TRUSTED_CIDRS`; recorre la cadena desde el proxy hacia atrás y se detiene en la primera dirección no confiable. Si no hay redes confiables configuradas o la cadena es inválida, usa la IP del peer. En desarrollo y pruebas ignora los encabezados proxy. Render documenta el uso de `X-Forwarded-For` y que sus servicios públicos pasan por Cloudflare y su balanceador ([documentación de Render](https://render.com/articles/how-render-handles-ddos-attacks)); las redes permitidas deben corresponder a todos los proxies reales de la cadena y verificarse para el servicio antes de configurarlas. Sin esas redes, el limitador puede agrupar solicitudes tras el proxy. Con varias instancias los contadores no se comparten; antes de escalar, sustituir este adaptador por un almacén compartido (por ejemplo Redis con una operación atómica y TTL) y conservar los mismos ámbitos/umbrales.

## OpenAPI y Swagger UI

- UI local: `http://localhost:8080/swagger-ui.html` cuando la aplicación corre con perfil `dev`.
- Especificación JSON: `http://localhost:8080/v3/api-docs` (YAML: `/v3/api-docs.yaml`).
- En Render las rutas esperadas son `https://<nombre-del-servicio>.onrender.com/swagger-ui.html` y `/v3/api-docs`.
- Pulsa **Authorize** e introduce el JWT válido (Swagger agrega el esquema Bearer). El login y el registro son públicos; el resto de las operaciones protegidas responde según las reglas ADMIN/USER.
- La definición OpenAPI/Swagger UI es pública y no contiene credenciales. Las operaciones de usuarios requieren ADMIN; las escrituras de asignaturas requieren ADMIN.

## Preparación para Render

Render ejecuta una imagen Docker basada en Maven/Temurin Java 21. `Dockerfile` construye con `RUN_POSTGRES_INTEGRATION=false ./mvnw -B clean verify`; el stage de ejecución inicia con `java -jar /app/app.jar`. `render.yaml` declara un servicio web y PostgreSQL en la misma región, con auto deploy desactivado y una instancia para que el limitador en memoria conserve estado coherente. Producción migra con Flyway, valida con Hibernate y conserva `server.forward-headers-strategy=none`; `APP_PROXY_TRUSTED_CIDRS` queda vacío hasta confirmar redes upstream confiables. Swagger declara una URL de servidor relativa.

El archivo `render.yaml` sirve como Blueprint para importar la rama `codex/actividad2-auth-y-kodular` del repositorio GitHub `Shakespeares1/gestion-asignaturas-api` y perfil `prod`. Render inyecta `PORT`; la aplicación usa ese valor, con fallback `10000`, y escucha en `0.0.0.0`. El health check `/actuator/health/readiness` verifica la aplicación y PostgreSQL sin depender del SMTP. Los probes están habilitados en `prod`; las rutas de health son públicas y sus detalles permanecen ocultos. Los pasos de creación del servicio y PostgreSQL están en [DEPLOYMENT_RENDER_POSTGRES.md](DEPLOYMENT_RENDER_POSTGRES.md).

Configura en Render:

- Obligatorios: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL` conectado al `connectionString` privado de Render Postgres, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64` (al menos 32 bytes aleatorios Base64). El adaptador convierte la URL Render a JDBC y exige TLS. El blueprint referencia el usuario y contraseña de la base, y genera `JWT_SECRET_BASE64` de 256 bits.
- Opcionales: `JWT_ISSUER` (predeterminado `gestion-academica`), `JWT_TTL_SECONDS` (900; rango 60–86400), `CORS_ALLOWED_ORIGINS` (orígenes HTTPS explícitos separados por coma; vacío deshabilita CORS para navegadores) y `APP_PROXY_TRUSTED_CIDRS` (lista separada por comas; dejar vacía hasta verificar las redes de todos los proxies upstream que llegan a la aplicación). No incluyas rangos de clientes ni redes amplias para habilitar encabezados.
- Para habilitar recuperación por correo: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` y `APP_MAIL_RESET_URL` con esquema HTTPS en producción. STARTTLS y su requisito deben estar habilitados (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true`) y validar la identidad del host (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_CHECKSERVERIDENTITY=true`). Auth/STARTTLS y timeouts SMTP están en `.env.example`. Render bloquea SMTP saliente en puertos 25, 465 y 587 en sus servicios gratuitos; sin proveedor compatible la recuperación responde `503` y no se simula un envío. No configures credenciales hasta disponer de un proveedor real.
- No configures `PORT` manualmente salvo que Render lo solicite; la plataforma lo entrega. No configures el perfil `dev`.

`render.yaml` vincula credenciales de PostgreSQL, genera la clave JWT y deja CORS/CIDR vacías. La conexión a GitHub y la creación del servicio/base se hacen en el flujo de Render. No se han creado recursos en la cuenta del usuario.

Las pruebas normales usan el perfil `test` aislado de Supabase mediante H2 en memoria, no requieren una clave JWT y no envían correo. La prueba PostgreSQL requiere `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RUN_POSTGRES_INTEGRATION=true` y `SPRING_PROFILES_ACTIVE=postgres-integration`. Ejecútala solo contra una base de pruebas aislada: crea, actualiza y elimina un registro temporal.

## Verificación más reciente

Las cifras de pruebas históricas de este repositorio no prueban esta copia tras los cambios de configuración de Render. Ejecuta la compilación y pruebas antes del primer despliegue. Las pruebas locales tampoco validan PostgreSQL real, el advisory lock de bootstrap, un proveedor de correo ni el servicio de Render.
