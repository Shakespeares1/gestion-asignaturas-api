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
- No conectar la aplicación al Supabase existente hasta respaldar e inspeccionar esquema y datos y compararlos con V1/V2.
- Baseline manual en versión 2 solo después de revisión y confirmación de equivalencia exacta. Si existe alguna diferencia, diseñar y probar una migración de transición sobre una copia aislada. No se ejecutó conexión, baseline, DDL ni cambio de datos remotos.

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

- Resultado Maven verificado en la auditoría final: `mvnw.cmd -o clean test` con `RUN_POSTGRES_INTEGRATION=false`; 99 pruebas totales, 98 aprobadas, 0 fallos, 0 errores y 1 integración PostgreSQL opt-in omitida. BUILD SUCCESS. También se validó el rechazo de `X-Forwarded-For` y las precondiciones del perfil bootstrap.
- Los tests MockMvc cubren `429`/`Retry-After` para login, registro, recuperación y restablecimiento, incluida la limitación IP previa a validar el cuerpo; tests unitarios cubren aislamiento IP/cuenta, expiración de ventana, política de contraseñas y configuración de correo.
- Maven no pudo abrir el lock de solo lectura en el `.m2` habitual; se usó una copia temporal de la caché y no se alteró el repositorio Maven del usuario.
- La suite no valida advisory locks ni conexión real a PostgreSQL. No hay evidencia de conexión/validación en Supabase.

## Pendiente y riesgos

- Limitador en memoria se reinicia al reiniciar la instancia y no comparte contadores entre instancias; `render.yaml` fija una instancia inicial. Usar Redis u otro almacenamiento atómico compartido antes de escalar horizontalmente. En `prod`, la app usa `CF-Connecting-IP` de Render y desactiva forwarded headers; fuera de Render, configurar un resolver de IP confiable.
- Los límites usan ventanas fijas (un pequeño burst cerca del borde de ventana es posible); revisar umbrales con tráfico real antes de producción. No se envió correo ni se validó un handshake SMTP real.
- Validar el bootstrap y el advisory lock contra PostgreSQL temporal aislado antes de usarlo productivamente. La prueba opt-in PostgreSQL fue omitida; no se probó el lock en PostgreSQL.
- Completar la inspección/adopción controlada del esquema Supabase antes del primer arranque de producción.
- Configurar secretos, CORS y proveedor SMTP reales; validar Render y despliegue en un paso posterior.
- Kodular, sensores y entregables académicos siguen pendientes.

## Próximo paso

Validar bootstrap/advisory lock en PostgreSQL aislado. Luego, revisar y aprobar el procedimiento de adopción del esquema antes de conectar a Supabase o ejecutar el bootstrap real.
