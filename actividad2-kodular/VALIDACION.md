# Validación del paquete de Actividad 2

- Archivo: `GestorPedidos_Actividad2_SpringBoot_v14.aia`
- SHA-256: `FAF8753720DA83722AB54FBAF785DEFAC46768701D17FDA9B941402F46DE150D`
- Tamaño: 1,380,290 bytes
- ZIP: 23 entradas, sin rutas inseguras.
- Pantallas: 8 con archivos `.bky` y `.scm`.
- Bloques: 5,073; XML y JSON de diseñador analizados; IDs únicos dentro de cada pantalla.
- Reportes: `Solicitar` presente; no hay setter de `busy` suelto en la raíz ni setter `spnTipoReporte.Enabled`; se encontraron cuatro setters para bloquear/desbloquear el botón de consulta.
- Secretos: no se encontraron tokens JWT, URL JDBC con credenciales ni claves privadas dentro del `.aia`.
- Companion: pruebas de ejecución confirmadas por el usuario, enumeradas en `README.md`.
- No verificado: generación y ejecución de APK final; recuperación de contraseña por email con SMTP configurado.

El archivo fuente se conserva como una copia; no se alteró el original del usuario.
