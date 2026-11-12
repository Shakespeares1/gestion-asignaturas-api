# Integración de Kodular con Gestión Académica API

## Estado de los archivos de referencia

`GestorPedidos_v8.aia.zip` no está presente en el workspace; no hay un `.aia`, `.aix` ni `.apk` local para inspeccionar. Por eso aún no se puede afirmar qué pantallas, componentes, bloques o conexión Supabase contiene, ni aplicar cambios al proyecto original. Cuando el archivo se incorpore al workspace se podrá inventariar `youngandroidproject/project.properties`, `src/*/Screen*.scm` y `src/*/Screen*.bky`, y reemplazar la integración de datos.

## Arquitectura de cliente propuesta

- La app se conecta únicamente a la URL HTTPS pública de la API Spring Boot. No incluir claves, URL ni SDK de Supabase en la app.
- Usa el componente no visible `Web` para REST y `TinyDB` para conservar el JWT de sesión. No persistir contraseñas; limpiar el token al cerrar sesión.
- Cada solicitud protegida agrega `Authorization: Bearer <accessToken>` y `Accept: application/json`. Solicitudes JSON agregan `Content-Type: application/json`.
- Maneja `GotText` inspeccionando URL, código HTTP y cuerpo. Enrutamiento por URL/tag evita que dos solicitudes concurrentes se interpreten como la misma respuesta. Maneja `TimedOut` con opción de reintentar.
- La documentación vigente del componente Web incluye GET, POST, PUT y DELETE; no documenta PATCH. Por ello, la API conserva PATCH y también permite PUT en `PUT /api/usuarios/{id}/access` para clientes Kodular. Confirmado con prueba MockMvc después de añadir la compatibilidad.
- En Android el componente nativo Web no aplica CORS como un navegador; CORS sí debe limitarse a orígenes frontend de navegador en `CORS_ALLOWED_ORIGINS`.

## Pantallas sugeridas

1. **Login**: email y contraseña; `POST /api/auth/login`; guardar `accessToken`, usuario e indicador de rol, y enviar token en solicitudes siguientes.
2. **Registro**: `POST /api/usuarios`; aceptar nombre, email y contraseña. El servidor siempre crea `USER` activo; ocultar controles de rol.
3. **Inicio**: menú condicionado por rol. Ambos roles pueden consultar asignaturas y reportes de asignaturas; solo ADMIN ve gestión de usuarios, escrituras de asignaturas y reportes de usuarios.
4. **Asignaturas**: lista/detalle con GET; formulario crear POST, editar PUT y eliminar DELETE solo para ADMIN.
5. **Reportes**: cuatro consultas GET, con filtros y paginación; presentar `items`, `totalElements`, `totalPages` y navegación `page`.
6. **Usuarios (ADMIN)**: consultar, actualizar nombre/email, usar PUT `/access` para rol/estado y eliminar solo mediante una acción confirmada.
7. **Recuperación**: pedir correo con POST `/forgot-password`; aceptar token recibido fuera de la app y nueva contraseña en POST `/reset-password`. Sin proveedor SMTP el servidor responde 503 y la app debe explicar que recuperación no está habilitada.

## Componentes de Kodular

- Visibles: `TextBox`, `PasswordTextBox`, `Button`, `ListView`/`ListViewImageText`, `Notifier`, `VerticalArrangement` y `HorizontalArrangement`.
- No visibles: uno o varios `Web`, `TinyDB`, `Clock`; opcional `AccelerometerSensor` y `LocationSensor` solo para una función académica explícita.
- Esquema de headers como lista de pares: `[["Content-Type","application/json"],["Accept","application/json"],["Authorization","Bearer " + token]]`. Para login/registro público se omite Authorization.
- Serializa objetos JSON con `Web.JsonObjectEncode`; decodifica respuestas con `Web.JsonTextDecodeWithDictionaries`. Construye filtros con `Web.UriEncode` para valores de texto.
- Como el componente Web retorna un solo evento `GotText`, asocia la operación pendiente con la URL/tag guardada antes de llamar `Get`, `PostText`, `PutText` o `Delete`.
- Trata 401 como sesión vencida (limpiar token y volver a login), 403 como permiso insuficiente, 400 como validación, 409 como email duplicado, 429 respetando `Retry-After`, 503 como correo no disponible, y timeout como fallo de conectividad. No muestres respuestas internas o credenciales.

## Contratos REST principales

| Flujo | Método y ruta | Acceso |
|---|---|---|
| Login | `POST /api/auth/login` | Público |
| Registro | `POST /api/usuarios` | Público, crea USER/ACTIVO |
| Asignaturas | `GET /api/asignaturas[/{id}]` | USER/ADMIN |
| Crear/editar/eliminar asignatura | `POST /api/asignaturas`, `PUT/DELETE /api/asignaturas/{id}` | ADMIN |
| Usuarios | `GET/PUT/DELETE /api/usuarios[/{id}]`, `PUT /api/usuarios/{id}/access` | ADMIN |
| Recuperación | `POST /api/auth/forgot-password`, `POST /api/auth/reset-password` | Público |
| Reportes de asignaturas | `GET /api/reportes/asignaturas/carrera-semestre-area`, `.../creditos-profesor-nombre` | USER/ADMIN |
| Reportes de usuarios | `GET /api/reportes/usuarios/rol-estado`, `.../fecha-registro` | ADMIN |

La app no debe llamar endpoints protegidos antes de obtener el JWT. El JWT actual expira en 900 segundos por defecto; no hay refresh token. Al recibir 401 se debe pedir login de nuevo.

## Sensores

El acelerómetro no es necesario para CRUD; puede usarse como gesto opcional para refrescar lista, con alternativa visible y sin disparar peticiones repetidas. La ubicación no es necesaria para administrar asignaturas/usuarios: solo se debe incorporar si se define un requisito de producto concreto, con solicitud contextual de permiso y sin transmitir ubicación a la API hasta que exista endpoint y consentimiento explícito. No enviar telemetría de sensores por defecto.

## Pruebas

Importe `http/postman/gestion-academica.postman_collection.json` y el environment local. Ejecute contra localhost o staging aislado. CRUD altera datos; no ejecute las solicitudes POST/PUT/PATCH/DELETE contra producción para validar. La recuperación requiere un SMTP real y un token de correo válido.
