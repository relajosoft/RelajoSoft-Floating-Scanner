# Arquitectura y flujo de datos

> Desarrollado por **RelajoSoft** · [relajosoft.com](https://relajosoft.com)  
> Autor: **Yoel Enrique Estevez Gonzalez**

## Componentes propios

### `MainActivity`

Pantalla de configuración. Comprueba cámara, overlay, IME habilitado e IME seleccionado. Permite activar o detener el botón flotante y cambiar las opciones operativas.

### `ScannerInputMethodService`

Teclado Android propio. Android le entrega una `InputConnection` únicamente cuando el usuario selecciona un campo editable y este IME está activo. Desde esa conexión se escribe manualmente o se inserta el código leído.

### `FloatingBubbleService`

Servicio en primer plano no exportado. Crea el botón flotante, abre una ventana de cámara no enfocable y coordina CameraX con ML Kit. La bandera `FLAG_NOT_FOCUSABLE` evita que el overlay robe el foco a la app de trabajo.

### `Prefs`

Guarda cuatro opciones booleanas en preferencias privadas: linterna, ENTER, filtro QR con W y eliminación de sufijo -M. No guarda lecturas.

## Recorrido de una lectura

```text
El usuario toca el input de la app de trabajo
                    │
                    ▼
Android entrega InputConnection al IME de RelajoSoft
                    │
                    ▼
El usuario pulsa ESCANEAR o el botón flotante
                    │
                    ▼
Overlay no enfocable + CameraX muestran la cámara
                    │
                    ▼
ML Kit analiza el fotograma localmente
        │                        │
        │ sin resultado          │ código válido
        ▼                        ▼
Segundo análisis +90°      Filtros configurados
                                  │
                                  ▼
                     InputConnection.commitText()
                                  │
                                  ▼
                    El IME se oculta y se liberan
                    cámara, analizador y memoria
```

## Datos que entran y salen

- **Entrada:** fotogramas temporales de la cámara y preferencias locales.
- **Salida:** texto escrito en el campo activo, sonido y vibración de confirmación.
- **Red:** ninguna.
- **Base de datos:** ninguna.
- **Archivos de códigos:** ninguno.
- **Historial o telemetría:** ninguno.

## Dependencias

- AndroidX Core y AppCompat.
- AndroidX Lifecycle Service.
- CameraX para cámara, vista previa y análisis.
- Google ML Kit Barcode Scanning para reconocimiento local.

Las versiones exactas están centralizadas en `app/build.gradle.kts` y deben revisarse mediante pull request antes de modificarlas.
