# Seguridad de RelajoSoft Scanner

> Desarrollado por **RelajoSoft** · [relajosoft.com](https://relajosoft.com)  
> Responsable: **Yoel Enrique Estevez Gonzalez**

## Resumen

La aplicación está diseñada para ejecutar el escaneo y la escritura localmente. No declara el permiso `INTERNET`, no posee servidor, no integra analítica y no guarda historial de los códigos leídos.

## Permisos declarados

| Permiso | Motivo | Activación |
|---|---|---|
| `CAMERA` | Capturar fotogramas para ML Kit | Confirmación del usuario |
| `SYSTEM_ALERT_WINDOW` | Mostrar botón y cámara encima de la app de trabajo | Ajuste especial manual |
| `VIBRATE` | Confirmar una lectura correcta | Automática |
| `FOREGROUND_SERVICE` | Mantener disponible el botón flotante | Automática |
| `FOREGROUND_SERVICE_CAMERA` | Usar la cámara desde el servicio visible | Automática |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Declarar el caso del overlay persistente | Automática |
| `POST_NOTIFICATIONS` | Mostrar la notificación obligatoria del servicio | Confirmación en Android 13+ |

No se solicitan permisos de Accesibilidad, Internet, almacenamiento, ubicación, contactos, micrófono ni teléfono.

## Controles aplicados

- **IME protegido por Android:** `ScannerInputMethodService` exige `BIND_INPUT_METHOD`, permiso que solo concede el sistema.
- **Servicio interno:** `FloatingBubbleService` utiliza `android:exported="false"` y no puede iniciarse directamente desde otra aplicación.
- **Foco controlado por el usuario:** el código solo se escribe en el campo que el operario seleccionó.
- **Procesamiento local:** CameraX entrega el fotograma a ML Kit dentro del dispositivo.
- **Sin persistencia de lecturas:** el valor se mantiene en memoria durante el procesamiento y después se descarta.
- **Sin red:** el manifiesto no declara `INTERNET` y `usesCleartextTraffic` está desactivado.
- **Sin copias de seguridad:** `allowBackup` está desactivado.
- **Sin Accesibilidad:** se eliminó el servicio y toda referencia a ese mecanismo.
- **Dependencias limitadas:** Gradle solo permite los repositorios oficiales Google Maven y Maven Central.
- **Firma de actualización:** todas las versiones corporativas deben firmarse con la misma clave privada protegida fuera de GitHub.

## Riesgos conocidos y tratamiento

1. El permiso de overlay es sensible porque permite dibujar sobre otras aplicaciones. Se solicita manualmente, la cámara es visible y el servicio mantiene una notificación permanente.
2. Un IME puede recibir el contenido que el usuario escribe mientras está seleccionado. Este IME no registra, almacena ni transmite pulsaciones.
3. Una clave de firma perdida impide actualizar las instalaciones existentes. Debe guardarse en un gestor corporativo de secretos con copia de recuperación y acceso limitado.
4. Las dependencias deben revisarse antes de cada actualización. No se deben cambiar versiones sin recompilar, ejecutar Lint y repetir las pruebas en la PDA objetivo.

## Comunicación de incidencias

Las incidencias de seguridad deben comunicarse de forma privada al responsable del repositorio o mediante [relajosoft.com](https://relajosoft.com). No se deben publicar códigos internos, capturas de la aplicación de trabajo, APK firmadas ni claves en incidencias públicas.
