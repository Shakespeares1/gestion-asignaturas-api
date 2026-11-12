# Runbook operativo

## Flyway y adopción de Supabase

Flyway está habilitado por defecto y ejecuta validación/migraciones al iniciar la aplicación. Hibernate ejecuta `ddl-auto=validate` en producción después de Flyway; `validate` no bloquea las migraciones de Flyway. `spring.flyway.baseline-on-migrate=false` impide crear un baseline automáticamente y debe mantenerse así.

Antes de que cualquier instancia se conecte a la base Supabase existente:

1. Suspende temporalmente despliegues y toma un respaldo verificable de esquema y datos.
2. Inspecciona el esquema e historial Flyway existente sin arrancar esta aplicación contra esa base.
3. Compara columnas, tipos, longitudes, nulabilidad, identity, constraints, índices y datos con `V1__create_asignaturas.sql` y `V2__create_usuarios.sql`; prueba la comparación sobre una copia aislada.
4. Si la estructura existente representa V1 y coincide con ella, repara primero las diferencias de V1 bajo un procedimiento revisado. Tras verificar el esquema, solicita aprobación operativa y aplica manualmente el baseline en versión 1. Baseline solo registra historial: no crea, valida ni corrige objetos.
5. Ejecuta `flyway info` y confirma que V2 sea la migración pendiente. Solicita autorización independiente antes de `flyway migrate`; V2 crea `usuarios`.
6. Si hay cualquier diferencia o incertidumbre, no hagas baseline ni migrate. Diseña y ensaya una transición sobre una copia restaurada; revisa el plan antes de producción.
7. Despliega con perfil `prod` solo tras confirmar el esquema e historial: Flyway esta deshabilitado en este perfil y Hibernate `validate` comprueba el mapeo sin aplicar migraciones. Migraciones futuras requieren una operacion separada, controlada y aprobada.

No se conectó a Supabase, no se ejecutó DDL ni baseline como parte de este runbook.

### Estado de adopción reportado (11 de noviembre de 2026)

- El operador confirmó que la reparación de V1 fue verificada, el baseline se estableció en versión 1 y V2 se aplicó.
- La salida de Flyway visible en la terminal confirma validación de migraciones, versión actual 1 antes de migrar y aplicación exitosa de V2 hasta versión 2.
- No repitas `baseline` ni `migrate`. En la verificación remota de solo lectura del 11-11-2026, `flyway info` mostró baseline V1 y V2 `Success`; `flyway validate` confirmó 3 migraciones. Antes del primer arranque de producción, inspecciona objetos por catálogo y valida el mapeo JPA con Flyway deshabilitado.
- En esta sesión solo se ejecutaron `flyway info` y `flyway validate` con SSL; no hubo SQL de escritura. La integración PostgreSQL de Maven continúa omitida intencionalmente.

## Resolución segura de IP cliente en Render

- `server.forward-headers-strategy=none` se mantiene para conservar la dirección del peer en `request.getRemoteAddr()`.
- En `prod`, `ClientIpResolver` ignora `CF-Connecting-IP`. Solo procesa `X-Forwarded-For` si el peer inmediato pertenece a una red en `APP_PROXY_TRUSTED_CIDRS`; valida la cadena como literales IP y recorre los saltos desde la derecha.
- Mantén `APP_PROXY_TRUSTED_CIDRS` vacío hasta verificar las redes exactas de los proxies que aparecen como peer y dentro de la cadena. Con lista vacía, los límites usan el peer observado; en Render eso puede agrupar clientes detrás del balanceador.
- Render documenta el uso de `X-Forwarded-For` para recuperar IP de cliente, pero la lista de confianza debe corresponder al camino real de la solicitud. No agregues rangos aportados por un request ni confíes en encabezados sin validar el peer.

## Crear el primer administrador

### Propiedades de seguridad

- No existe endpoint HTTP de bootstrap. `POST /api/usuarios` sigue siendo registro público limitado a `USER`/`ACTIVO`; campos adicionales enviados por el cliente no asignan permisos.
- El bootstrap está disponible solo con el perfil `admin-bootstrap`, usa la misma abstracción de hash configurada con BCrypt y nunca recibe la contraseña como argumento de proceso ni variable de entorno.
- El guard del perfil aborta antes de inicializar Flyway si no se especifican `web-application-type=none`, `flyway.enabled=false`, `ddl-auto=validate`, `sql.init.mode=never` y una conexión PostgreSQL con `sslmode=require`; tampoco admite perfiles dev/test/integración.
- La contraseña del primer ADMIN debe cumplir al menos 12 caracteres y como máximo 72 bytes UTF-8.
- La contraseña se captura con `System.console().readPassword`, sin eco. La ejecución falla si no existe una consola interactiva.
- Un advisory lock de PostgreSQL con alcance transaccional serializa ejecuciones concurrentes. Bajo ese lock se consulta si existe algún `ADMIN`; si ya existe, se rechaza el bootstrap. La inserción y la comprobación quedan dentro de la misma transacción.
- No uses el runner en cada despliegue. Es una operación única, controlada y ejecutada desde una estación/entorno administrativo autorizado.

### Precondiciones

1. La base objetivo debe estar respaldada y su esquema debe haberse comparado con V1/V2. Si ya existe, completa el procedimiento de adopción anterior antes de usar el runner.
2. La tabla `usuarios` debe existir y coincidir con las entidades/migraciones. El runner valida el esquema mediante Hibernate. El comando deshabilita Flyway para impedir que esta tarea ejecute migraciones; no omitas la adopción del esquema.
3. Usa una conexión PostgreSQL protegida por TLS, con permisos mínimos para consultar e insertar usuarios y obtener advisory locks.
4. Proporciona `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET_BASE64` al entorno del proceso mediante el gestor seguro aprobado. `DB_URL` debe incluir `sslmode=require`; la aplicación carga la configuración JWT al iniciar aunque el runner no emita tokens.
5. Define `BOOTSTRAP_ADMIN_NAME` y `BOOTSTRAP_ADMIN_EMAIL` para el proceso. El email se normaliza a minúsculas. No pongas la contraseña en comandos, scripts, archivos `.env` ni historial de terminal.
6. Ejecuta desde una consola interactiva con Java 21. La operación inserta un usuario real en la base seleccionada.

### Ejecución controlada en Windows PowerShell

Tras cargar las variables de conexión y JWT desde el gestor de secretos autorizado, define los datos no secretos de identidad en la sesión:

```powershell
$env:BOOTSTRAP_ADMIN_NAME = "Nombre del administrador"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.org"
```

Construye el artefacto localmente y luego ejecuta el proceso no web. No uses `mvn spring-boot:run` en una sesión sin consola interactiva.

```powershell
$env:RUN_POSTGRES_INTEGRATION = "false"
.\mvnw.cmd -DskipTests package
java -jar .\target\gestion-asignaturas-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=admin-bootstrap --spring.main.web-application-type=none --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=validate --spring.sql.init.mode=never
```

El runner pedirá la contraseña sin mostrarla. Al terminar correctamente informa que creó el administrador y finaliza la ejecución. La cuenta queda creada en la base especificada; el procedimiento no se ha ejecutado durante esta fase.

### Resultados y recuperación

- **Falta `BOOTSTRAP_ADMIN_NAME` o `BOOTSTRAP_ADMIN_EMAIL`:** aborta antes de solicitar la contraseña; define ambos valores y vuelve a ejecutar si no se creó cuenta.
- **No hay consola interactiva:** aborta sin aceptar una contraseña por argumento o entorno; abre PowerShell directamente en una terminal interactiva.
- **Ya existe un ADMIN:** el caso de uso falla bajo el lock y no crea otra cuenta. No borres ni cambies usuarios para forzar el bootstrap; usa el proceso normal de administración/recuperación de acceso.
- **Email ya registrado como USER:** aborta por unicidad y no cambia el rol. Un administrador existente debe elevar el acceso por el endpoint administrativo protegido, o seleccionar una dirección autorizada que no exista.
- **Fallo de conexión, esquema o transacción:** la transacción revierte la inserción; revisa la configuración y el esquema con el administrador de base. Flyway está deshabilitado en este comando.
- **Después de la creación:** conserva el registro de operación sin contraseñas, hashes, tokens ni cadenas de conexión. Verifica el acceso mediante el flujo normal de login y cambia a un perfil de uso habitual; no vuelvas a ejecutar el perfil `admin-bootstrap`.

El bloqueo evita que dos procesos de bootstrap de esta versión creen el primer ADMIN simultáneamente. Las pruebas locales validan la lógica de rechazo y el orden lock/comprobación; el comportamiento del advisory lock y la transacción requiere validación en PostgreSQL aislado antes de una operación productiva.
