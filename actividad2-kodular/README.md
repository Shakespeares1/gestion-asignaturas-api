# Actividad 2 — Gestión Académica en Kodular

Este directorio contiene el proyecto fuente de Kodular para la Actividad 2. La aplicación consume la API Spring Boot existente de la Actividad 3:

- API: `https://gestion-asignaturas-api.onrender.com`
- Proyecto fuente: `GestorPedidos_Actividad2_SpringBoot_v14.aia`

## Funcionalidades

- Inicio de sesión con JWT y registro de usuarios.
- CRUD de asignaturas y usuarios, con control de rol/estado.
- Cuatro consultas parametrizadas: dos reportes de asignaturas y dos de usuarios.
- Sensor de movimiento y sensor de ubicación.
- Navegación, validaciones y mensajes de error.

## Pruebas reportadas en Kodular Companion

El usuario confirmó en ejecución: acceso ADMIN; creación, edición, consulta y eliminación de asignaturas; operaciones de administración de usuarios; registro de una cuenta; carga de las cuatro opciones de reportes (el reporte ADMIN/ACTIVO mostró al administrador); detección del movimiento y lectura de coordenadas.

Validación estática del `.aia`: ZIP íntegro, 23 entradas, ocho pantallas, XML/SCM analizables, IDs únicos por pantalla y sin tokens JWT, URLs JDBC con credenciales ni claves privadas.

## Pendiente conocido

La recuperación de contraseña por correo no se considera validada. Requiere configurar SMTP como variables de entorno del backend y probar el envío y el cambio de contraseña de extremo a extremo. No se incluyen secretos en este repositorio.

## APK

La APK no se incluye todavía. Para generarla, importa el `.aia` en Kodular Creator, verifica que `Reportes` abra sin advertencias y usa **Export → Android App (.apk)**. Copia el APK resultante aquí solo después de probarla en un dispositivo.

## Seguridad

No guardes contraseñas, tokens, claves JWT, credenciales de PostgreSQL ni secretos SMTP en el `.aia`, este directorio, el código fuente o capturas de la demostración.
