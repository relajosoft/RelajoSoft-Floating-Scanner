# RelajoSoft Floating Scanner v0.8.0

> Desarrollado por **RelajoSoft** · [relajosoft.com](https://relajosoft.com)  
> Autor y responsable: **Yoel Enrique Estevez Gonzalez**

Aplicación Android para escanear códigos desde una cámara superpuesta y escribir el resultado en el campo activo de la aplicación de trabajo. La versión 0.8.0 está preparada para revisión técnica empresarial y utiliza un único método de escritura: un teclado Android propio basado en `InputMethodService` (IME).

## Qué resuelve

- Mantiene la aplicación de trabajo debajo de la cámara y conserva el input activo.
- Escanea códigos de barras y QR con CameraX y ML Kit, completamente en el dispositivo.
- Lee etiquetas colocadas en horizontal o lateral mediante un segundo análisis girado 90 grados.
- Permite escribir manualmente desde el mismo teclado cuando una etiqueta está dañada.
- Inserta el resultado con la API oficial `InputConnection.commitText()`.
- Oculta el teclado después del escaneo y evita su reapertura automática.

## Decisiones de esta edición

- Se eliminó totalmente el antiguo método por Accesibilidad.
- No existe permiso, servicio, configuración ni código de Accesibilidad.
- La aplicación no solicita Internet, ubicación, contactos ni almacenamiento.
- No conserva historial de lecturas y no transmite códigos.
- El servicio de cámara flotante no está exportado.
- Las copias de seguridad de Android y el tráfico HTTP sin cifrar están desactivados.

## Compilación rápida

1. Abrir la carpeta raíz en Android Studio.
2. Configurar JDK 17 y Android SDK 35.
3. Esperar la sincronización de Gradle.
4. Ejecutar **Build > Make Project**.
5. Para pruebas, usar **Build > Build APK(s)**.
6. Para desplegar, usar **Build > Generate Signed Bundle / APK > APK** y firmar siempre con la misma clave corporativa.

La explicación completa está en [Guía técnica y de compilación](docs/GUIA_TECNICA_Y_COMPILACION.md).

## Documentación para revisión

- [Arquitectura y flujo de datos](docs/ARQUITECTURA_Y_FLUJO.md)
- [Seguridad y permisos](SECURITY.md)
- [Historial de cambios](CHANGELOG.md)
- [Condiciones de uso interno](LICENSE.md)

## Estructura principal

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/relajosoft/floatingscanner/
│   ├── MainActivity.kt
│   ├── FloatingBubbleService.kt
│   ├── ScannerInputMethodService.kt
│   └── Prefs.kt
└── res/
    ├── drawable/
    ├── mipmap-*/
    ├── values/
    └── xml/input_method.xml
```

## Requisitos del dispositivo

- Android 8.0 o posterior, API 26+.
- Cámara trasera.
- Permiso de cámara.
- Permiso manual para mostrar sobre otras aplicaciones.
- RelajoSoft Scanner habilitado y seleccionado como teclado.

## Propiedad

Este es código propietario de RelajoSoft preparado para evaluación y despliegue interno autorizado. Las dependencias de AndroidX, CameraX, ML Kit y Gradle conservan sus licencias originales.
