# Despliegue inicial en Render con PostgreSQL

Este procedimiento aplica al repositorio de la Actividad 3 en una base de datos nueva y vacía. No reutilices una base de otra actividad ni una base con tablas existentes sin auditarla primero.

## Recursos del Blueprint

`render.yaml` define:

- Un servicio web Docker gratuito, con despliegue automático desactivado.
- Una instancia PostgreSQL gratuita de 1 GB en Oregon, conectada por red privada.
- `DB_URL`, usuario y contraseña referenciados desde el recurso PostgreSQL; no se guardan secretos en Git.
- Un secreto JWT generado por Render.

El código convierte el `connectionString` `postgresql://` de Render a JDBC, descarta usuario/clave de la URL y fuerza `sslmode=require`. Spring Boot ejecuta Flyway al iniciar y crea el esquema versionado en la base vacía. Hibernate valida el esquema resultante.

**Uso temporal para la actividad:** el Postgres gratuito de Render vence 30 días después de su creación. Pasado el vencimiento, existe una gracia de 14 días para pasar a un plan pagado; después se elimina la base y sus datos. No lo uses para almacenar información que debas conservar. El servicio web gratuito también se duerme tras inactividad.

## Crear el servicio y la base

1. Inicia sesión en Render y abre el Blueprint existente `gestion-asignaturas-api`. El servicio web ya existe; el inventario actual no muestra ninguna base PostgreSQL.
2. Conecta el Blueprint a la rama `codex/actividad2-auth-y-kodular-remote` de `Shakespeares1/gestion-asignaturas-api` y sincronízalo con el `render.yaml` de esa rama. Esta configuración debe conservar el servicio web existente y añadir una base PostgreSQL gratuita.
3. Revisa el plan gratuito de PostgreSQL y confirma el cambio del Blueprint. Render debe indicar que añadirá `gestion-asignaturas-db` y que el servicio actualizará sus variables para usar esa base.
4. Espera el primer deploy. El health check `/actuator/health/readiness` debe quedar `UP`; el arranque ejecuta las migraciones V1 y V2 una sola vez en la base nueva.
5. Verifica `https://<servicio>.onrender.com/v3/api-docs` y `https://<servicio>.onrender.com/actuator/health/readiness`.

No cambies la rama a `main` ni actives auto deploy hasta que los cambios se hayan revisado y publicado allí. El Blueprint actual está conectado a `main`; cambiarlo a la rama de trabajo es un paso necesario para aplicar este cambio.

## Crear el primer administrador

No hay endpoint HTTP para crear un ADMIN. El registro público crea cuentas USER. El primer ADMIN se inserta con el perfil de bootstrap, que exige PostgreSQL, una consola interactiva y ausencia de un ADMIN previo. El correo autorizado para esta primera cuenta es `Admin01@gmail.com` (se normaliza a minúsculas); el nombre visible propuesto es `Administrador 01`.

Tras el primer despliegue:

1. En Render, abre la instancia PostgreSQL y copia su **external database URL**. No la compartas por chat ni la guardes en archivos.
2. En una terminal local con Java 21, configura `DB_URL` como URL JDBC externa `jdbc:postgresql://<host>:<puerto>/<base>?sslmode=require`, y define `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET_BASE64`, `BOOTSTRAP_ADMIN_NAME` y `BOOTSTRAP_ADMIN_EMAIL` como variables de entorno. Usa el secret manager de Render o el método local seguro que controles; no las escribas en scripts ni en Git.
3. Ejecuta el artefacto del backend con `--spring.profiles.active=admin-bootstrap --spring.main.web-application-type=none --spring.flyway.enabled=false --spring.jpa.hibernate.ddl-auto=validate --spring.sql.init.mode=never`.
4. Escribe la contraseña de al menos 12 caracteres cuando la terminal la solicite. La entrada no se muestra en pantalla. El proceso termina tras confirmar la creación.
5. Borra las variables de la sesión y prueba el login normal de la API. No ejecutes el perfil bootstrap otra vez.

El proceso remoto escribe una fila de administrador en PostgreSQL; no se debe ejecutar hasta que el propietario confirme el nombre y correo del ADMIN inicial y el Blueprint esté desplegado correctamente.

## Recuperación de contraseña por correo

El servicio espera SMTP por variables de entorno. Render bloquea el SMTP saliente por los puertos 25, 465 y 587 en servicios gratuitos; por eso la recuperación por correo no quedará operativa solo con este Blueprint. Configura un proveedor de correo accesible por HTTPS o un SMTP/puerto permitido, verifica su remitente y carga sus credenciales directamente en Render. No las guardes en `render.yaml`, `.env`, el `.aia` ni Git. Sin el proveedor, el endpoint responde `503` y no debe marcarse como probado.

## Pruebas posteriores

- Health y OpenAPI: GET a las rutas indicadas arriba.
- Login: `POST /api/auth/login` con cuenta existente; esperar `200` y `accessToken`.
- Petición protegida sin JWT: esperar `401`.
- Recuperación sin SMTP: esperar `503`; con proveedor: esperar `202` y comprobar entrega real del mensaje.
- Crear el administrador una sola vez por el procedimiento de consola y probar login con esa cuenta.
