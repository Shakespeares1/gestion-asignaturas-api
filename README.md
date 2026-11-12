# gestion-asignaturas-api

API REST para la gestión académica de asignaturas, desarrollada con Java 21, Spring Boot 4.1.2, arquitectura hexagonal y PostgreSQL.

## Conexión a PostgreSQL / Supabase

La aplicación lee `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del entorno. No almacenes credenciales en el repositorio. Usa `.env.example` como referencia; Spring Boot no carga ese archivo automáticamente.

El perfil predeterminado valida el esquema existente. Flyway corre en `dev` y `test`; en `prod` está deshabilitado para que el arranque no ejecute migraciones remotas. `prod` exige Hibernate `validate` y PostgreSQL con `sslmode=require`; una diferencia de esquema detiene el arranque. Las migraciones futuras deben aplicarse en una operación controlada antes de desplegar la versión nueva. Nunca uses `dev` en Render ni frente a Supabase/esquemas compartidos.

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

No conectes la aplicación a la base Supabase existente hasta inspeccionar y respaldar el esquema y sus datos. En `prod`, Flyway está deshabilitado y Hibernate valida el esquema al arrancar. Conserva `baseline-on-migrate=false`; aplica futuras migraciones mediante una operación independiente, revisada y aprobada antes de desplegar la versión que las requiere.

## Migraciones versionadas con Flyway

Flyway aplica `classpath:db/migration` en desarrollo y pruebas; el perfil de pruebas usa H2 en modo PostgreSQL y valida después las entidades. En producción Flyway permanece deshabilitado para impedir cambios de esquema durante el arranque. Hibernate usa `ddl-auto=validate`; `flyway.clean` está deshabilitado.

Migraciones iniciales:

- `V1__create_asignaturas.sql`: `asignaturas`, ID identity `BIGINT`, campos de texto/`VARCHAR` e enteros según `AsignaturaEntity`, reglas de créditos y semestre positivos e índices por semestre/créditos.
- `V2__create_usuarios.sql`: `usuarios`, ID identity `BIGINT`, email único, enums persistidos como `VARCHAR(16)` con checks, timestamps `TIMESTAMP(6) WITH TIME ZONE`, pareja de recuperación nullable atómica e índices por rol/estado y fecha.

Flyway está habilitado globalmente, pero `application-prod.properties` lo deshabilita; Hibernate `validate` comprueba el esquema al iniciar Render. `baseline-on-migrate=false` y `clean-disabled=true` se conservan. Una base vacía de pruebas recibe V1 y V2 al iniciar. **No conectes la aplicación a Supabase existente hasta respaldar e inspeccionar su esquema y datos.** En producción, preparar y aplicar futuras migraciones requiere una operación controlada separada; no se ejecutan al iniciar la API.

Para adoptar un esquema existente, primero toma y verifica un respaldo, inspecciona el esquema y compara en una copia aislada todas las columnas, tipos, longitudes, nulabilidad, identity, índices, restricciones y datos con V1/V2. Verifica además emails normalizados y sin duplicados, roles/estados válidos, timestamps presentes y tokens de recuperación emparejados. La operación ya reportada para esta base estableció baseline Flyway en versión 1 y aplicó V2. No repitas baseline ni migrate. La línea base no crea tablas, valida ni corrige el esquema. Si existe cualquier diferencia, diseña, prueba y revisa una migración de transición en una copia aislada.

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

## Preparación para Render (sin servicio creado)

Render ejecuta una imagen Docker basada en Maven/Temurin Java 21. `Dockerfile` construye con `RUN_POSTGRES_INTEGRATION=false ./mvnw -B clean verify`; el stage de ejecución inicia con `java -jar /app/app.jar`. `render.yaml` fija `numInstances: 1` para que el limitador en memoria tenga estado coherente. Producción usa Hibernate `validate`, deshabilita Flyway y conserva `server.forward-headers-strategy=none`; `APP_PROXY_TRUSTED_CIDRS` queda vacío hasta confirmar redes upstream confiables. Swagger declara una URL de servidor relativa. El blueprint tiene auto deploy desactivado; la creación de un servicio Render inicia un primer despliegue y requiere autorización aparte.

El archivo `render.yaml` sirve como blueprint para importar el repositorio GitHub `Shakespeares1/gestion-asignaturas-api`, rama `main`, con auto deploy desactivado y perfil `prod`. Al crear el servicio, Render debe vincular el repositorio y aplicar ese blueprint. Render inyecta `PORT`; la aplicación usa ese valor, con fallback `10000`, y escucha en `0.0.0.0`. El health check usa `/actuator/health/readiness`, que verifica disponibilidad de la aplicación y PostgreSQL sin depender del estado SMTP. Los probes están habilitados en `prod`; las rutas de health son públicas y sus detalles permanecen ocultos.

Configura en Render:

- Obligatorios: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL` en formato JDBC (`jdbc:postgresql://<host>:5432/<base>?sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64` (al menos 32 bytes aleatorios Base64). El blueprint genera `JWT_SECRET_BASE64` de 256 bits. No uses `DATABASE_URL`: la configuración de Spring consume `DB_URL`.
- Opcionales: `JWT_ISSUER` (predeterminado `gestion-academica`), `JWT_TTL_SECONDS` (900; rango 60–86400), `CORS_ALLOWED_ORIGINS` (orígenes HTTPS explícitos separados por coma; vacío deshabilita CORS para navegadores) y `APP_PROXY_TRUSTED_CIDRS` (lista separada por comas; dejar vacía hasta verificar las redes de todos los proxies upstream que llegan a la aplicación). No incluyas rangos de clientes ni redes amplias para habilitar encabezados.
- Para habilitar recuperación por correo: `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` y `APP_MAIL_RESET_URL` con esquema HTTPS en producción. STARTTLS y su requisito deben estar habilitados (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED=true`) y validar la identidad del host (`SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_CHECKSERVERIDENTITY=true`). Auth/STARTTLS y timeouts SMTP están en `.env.example`. Si faltan estos ajustes, recuperación responde `503`; no se simula un envío. No configures SMTP hasta disponer de un proveedor real.
- No configures `PORT` manualmente salvo que Render lo solicite; la plataforma lo entrega. No configures el perfil `dev`.

`render.yaml` usa variables `sync: false` para DB_URL/usuario/contraseña, genera la clave JWT y deja CORS/CIDR vacías. La conexión GitHub, la carga de secretos y creación del servicio requieren completar el flujo de Render; crear un Web Service inicia el primer despliegue, por lo que debe autorizarse una vez verificada la configuración. No se ha creado ni desplegado el servicio.

Las pruebas normales usan el perfil `test` aislado de Supabase mediante H2 en memoria, no requieren una clave JWT y no envían correo. La prueba PostgreSQL requiere `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `RUN_POSTGRES_INTEGRATION=true` y `SPRING_PROFILES_ACTIVE=postgres-integration`. Ejecútala solo contra una base de pruebas aislada: crea, actualiza y elimina un registro temporal.

## Verificación más reciente

La última ejecución local de `mvnw.cmd -o clean test` con `RUN_POSTGRES_INTEGRATION=false` terminó con `BUILD SUCCESS`: 105 pruebas totales, 104 aprobadas, 0 fallos, 0 errores y 1 integración PostgreSQL omitida intencionalmente. No valida PostgreSQL real, el advisory lock en una base PostgreSQL, un servidor SMTP real, la construcción de la imagen Docker ni la conexión actual con Supabase.
