# Contexto del proyecto: Gestión Académica de Asignaturas

## Stack y arquitectura

- Java 21, Spring Boot 4.1.2, Spring Security 7, Maven y PostgreSQL.
- Arquitectura hexagonal: dominio sin Spring/JPA; aplicación con casos de uso y puertos; infraestructura con REST, configuración, seguridad y persistencia JPA.
- Flyway administra cambios de esquema. Está habilitado globalmente; Hibernate usa `validate` de forma predeterminada y en producción. `dev` permite `update`; `test` usa H2 en memoria, Flyway y `validate`.

## Funcionalidades implementadas

### Asignaturas

- CRUD REST y persistencia PostgreSQL/JPA.
- ADMIN crea, actualiza y elimina; USER y ADMIN consultan.
- Dos reportes parametrizados con filtros combinables, paginación y validación de rangos.

### Usuarios y seguridad

- CRUD de usuarios con email normalizado/único, roles ADMIN/USER, estados ACTIVO/INACTIVO y datos de recuperación.
- Registro público siempre crea USER/ACTIVO. Los campos de rol/estado no forman parte del DTO de registro.
- BCrypt, login JWT HS256, autorización por rol, recuperación con token aleatorio almacenando solo su hash.
- Política de contraseñas de 12 caracteres mínimos y 72 bytes UTF-8 máximos en registro, restablecimiento y bootstrap; no impone reglas arbitrarias de composición y no cambia el mínimo del login existente.
- Limitador singleton en memoria para login, recuperación, restablecimiento y registro; combina IP y digest de cuenta donde aplica, devuelve 429 y `Retry-After`.
- Dos reportes de usuarios protegidos para ADMIN. Las respuestas no incluyen hashes ni tokens.
- Bootstrap inicial de ADMIN agregado como tarea de consola bajo el perfil `admin-bootstrap`, con contraseña interactiva sin eco, BCrypt, comprobación de ADMIN existente y advisory lock transaccional PostgreSQL. No existe endpoint público de bootstrap ni cuenta predeterminada. La operación no se ha ejecutado.

### API y despliegue

- OpenAPI/Swagger con esquema Bearer JWT.
- Dockerfile y `render.yaml` preparados; no se ha creado un servicio ni desplegado.
- SMTP STARTTLS obligatorio y verificación de identidad cuando se configura; `APP_MAIL_RESET_URL` no vacío debe ser HTTPS en prod. Entrega real de correo no verificada.

## Migraciones y Supabase

- `V1__create_asignaturas.sql` y `V2__create_usuarios.sql` representan el esquema inicial de ambas entidades.
- Flyway puede aplicar migraciones al iniciar la aplicación. Hibernate `ddl-auto=validate` no evita los cambios de esquema que ejecute Flyway.
- `spring.flyway.baseline-on-migrate=false`; no se debe activar baseline automático.
- Verificación remota de solo lectura: Flyway CLI 12.4.0 conectó a Supabase PostgreSQL 17.11 con SSL; `info` muestra baseline V1 y V2 `Success`, y `validate` confirmó 3 migraciones.
- No repetir baseline ni migrate. Se comprobó el mapeo JPA en un arranque diagnóstico del perfil `prod`, con Flyway deshabilitado y Hibernate `validate`. Las llamadas HTTP realizadas fueron únicamente a localhost, no al backend remoto.
- Antes de un arranque normal de producción, completar la inspección actualizada de catálogos tras V2 y configurar Render. Flyway conserva `baseline-on-migrate=false`, Hibernate `validate` y esquema predeterminado `public`.

## Archivos relevantes

- Dominio: `src/main/java/com/gestionacademica/domain/model/`.
- Aplicación: `src/main/java/com/gestionacademica/application/port/` y `application/service/`.
- Bootstrap: `BootstrapAdminUseCase`, `AdminBootstrapLockPort`, `BootstrapAdminService`, `AdminBootstrapConfiguration`, `AdminBootstrapLockAdapter`.
- Protección de solicitudes: `AuthenticationRateLimitFilter`, `RequestRateLimiter`, `RateLimitedOperation` y `RateLimitExceptionHandler`.
- Política de contraseñas: `application/service/PasswordPolicy.java` y anotación REST `infrastructure/web/validation/ValidPassword.java`.
- Persistencia: `src/main/java/com/gestionacademica/infrastructure/persistence/`.
- Configuración: `src/main/resources/application*.properties`.
- Migraciones: `src/main/resources/db/migration/V1__create_asignaturas.sql` y `V2__create_usuarios.sql`.
- Runbook de Flyway, Supabase y bootstrap: `OPERATIONS.md`.
- API y ejecución: `README.md`.

## Verificación

- Resultado de esta sesión: `mvnw.cmd -Dmaven.repo.local=<TEMP> clean test` con `RUN_POSTGRES_INTEGRATION=false`; 101 pruebas, 0 fallos, 0 errores y 1 integración PostgreSQL opt-in omitida. BUILD SUCCESS. FlywayMigrationIntegrationTest ejecutó V1/V2 sobre H2; no prueba Supabase.
- Los tests MockMvc cubren `429`/`Retry-After` para login, registro, recuperación y restablecimiento, incluida la limitación IP previa a validar el cuerpo; tests unitarios cubren aislamiento IP/cuenta, expiración de ventana, política de contraseñas y configuración de correo.
- Maven no pudo abrir el lock de solo lectura en el `.m2` habitual; se usó una copia temporal de la caché y no se alteró el repositorio Maven del usuario.
- La suite no valida advisory locks ni Supabase. Flyway confirmó conexión remota, historial baseline V1/V2 y checksums. El arranque JPA real y las comprobaciones HTTP locales posteriores se registran abajo; no se probaron escrituras CRUD remotas.

### Verificación backend real (2026-11-11)

- El JAR arrancó con el perfil `prod`, Flyway deshabilitado explícitamente, Hibernate `validate` y sin inicialización SQL. Spring completó el arranque, validando JPA contra el esquema remoto sin migrar ni escribir datos.
- En localhost, OpenAPI respondió `200`; el endpoint de asignaturas sin autenticación respondió `401`, como exige la configuración de seguridad.
- `/actuator/health` respondió `200` al excluir solo durante esta comprobación el indicador SMTP, que aparecía no saludable por falta de configuración local. No se probó el envío de correo.
- Proceso temporal finalizado; variables de base de datos y JWT eliminadas del entorno de la terminal. Sin pruebas CRUD de escritura, despliegue ni cambios remotos.

### Preparación del health check de Render

- El health check agregado podía reflejar SMTP como caído cuando el proveedor de correo no estaba configurado. `render.yaml` ahora consulta `/actuator/health/readiness`; producción habilita probes y readiness verifica `readinessState,db`, sin incluir SMTP.
- `/actuator/health/**` es público con detalles ocultos. `AuthorizationIntegrationTest` comprueba readiness `UP` con H2; suite completa posterior: 102 pruebas, 0 fallos, 0 errores, 1 integración PostgreSQL omitida intencionalmente.
- No se creó ni desplegó el servicio Render. La comprobación de readiness fue automatizada en MockMvc/H2, no contra Render.

## Pendiente y riesgos

- Limitador en memoria se reinicia al reiniciar la instancia y no comparte contadores entre instancias; `render.yaml` fija una instancia inicial. Usar Redis u otro almacenamiento atómico compartido antes de escalar horizontalmente. `ClientIpResolver` ignora `CF-Connecting-IP`; en `prod` solo interpreta `X-Forwarded-For` si el peer pertenece a `APP_PROXY_TRUSTED_CIDRS`, y usa el peer como fallback. Las redes reales de upstream para Render aún deben verificarse antes de configurarlas; sin ellas, los clientes pueden compartir la IP del balanceador para el rate limit.
- Los límites usan ventanas fijas (un pequeño burst cerca del borde de ventana es posible); revisar umbrales con tráfico real antes de producción. No se envió correo ni se validó un handshake SMTP real.
- Validar el bootstrap y el advisory lock contra PostgreSQL temporal aislado antes de usarlo productivamente. La prueba opt-in PostgreSQL fue omitida; no se probó el lock en PostgreSQL.
- Completar verificación remota de solo lectura de Flyway y tablas después de V2, antes del primer arranque de producción.
- Configurar secretos, CORS y proveedor SMTP reales; validar Render y despliegue en un paso posterior.
- Kodular, sensores y entregables académicos siguen pendientes.

## Próximo paso

Repetir de forma segura `flyway info`/`validate` y consultas de catálogo de solo lectura con credenciales introducidas localmente; no volver a ejecutar baseline ni migrate. Después validar el arranque de producción sin desplegar y probar el bootstrap/advisory lock en PostgreSQL aislado.


### Render deployment guard (2026-11-12)

- `application-prod.properties` explicitly sets `spring.flyway.enabled=false`; the Render startup cannot run Flyway migrate/baseline. Hibernate remains `validate` and refuses to start on a mismatched schema. Future schema changes must use an approved, separate migration operation.
- `render.yaml` keeps readiness at `/actuator/health/readiness`, disables auto deploys on commits, and leaves `CORS_ALLOWED_ORIGINS` and `APP_PROXY_TRUSTED_CIDRS` empty until actual values are verified.
- Render expects `DB_URL` in JDBC format (`jdbc:postgresql://...?...sslmode=require`), `DB_USERNAME`, `DB_PASSWORD`; it does not consume `DATABASE_URL`. `JWT_SECRET_BASE64` is generated by Render as a base64-encoded 256-bit secret.
- Service creation in Render triggers an initial deployment. Render/GitHub connections are not available in this session; no service was created. SMTP credentials remain absent and recovery requests return 503 until configured.
