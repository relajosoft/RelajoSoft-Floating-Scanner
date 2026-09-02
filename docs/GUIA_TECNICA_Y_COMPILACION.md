# Guía técnica, compilación y despliegue

> Preparada por **Yoel Enrique Estevez Gonzalez**  
> **RelajoSoft** · [relajosoft.com](https://relajosoft.com)

## 1. Objetivo de esta entrega

Preparé esta versión para que el equipo informático pueda revisar el código completo, compilarlo y desplegarlo en las PDA de la empresa. Dejé un solo método de escritura, el teclado IME de Android, porque es el que probamos y funciona de forma estable.

Para que no haya mareo: la aplicación no usa Accesibilidad, no se conecta a Internet y no guarda los códigos. La cámara y el reconocimiento trabajan dentro de la PDA.

## 2. Herramientas necesarias

- Android Studio.
- JDK 17 configurado como Gradle JDK.
- Android SDK Platform 35.
- Android SDK Build-Tools 34.0.0 o una versión compatible instalada por Android Studio.
- Git, si se va a trabajar desde el repositorio privado.
- Una PDA Android 8.0 o posterior para la prueba final.

El proyecto incluye Gradle Wrapper. No hace falta instalar Gradle manualmente.

## 3. Descargar desde GitHub privado

Desde Android Studio:

1. Seleccionar **File > New > Project from Version Control**.
2. Pegar la URL HTTPS o SSH del repositorio privado.
3. Autenticarse con la cuenta corporativa autorizada.
4. Elegir una carpeta local y pulsar **Clone**.

Desde una terminal:

```bash
git clone URL_DEL_REPOSITORIO_PRIVADO.git
cd RelajoSoftFloatingScanner
```

## 4. Abrir y sincronizar

1. Abrir la carpeta donde están `settings.gradle.kts` y `gradlew.bat`.
2. En **Settings > Build, Execution, Deployment > Build Tools > Gradle**, seleccionar JDK 17.
3. Abrir **Tools > SDK Manager** e instalar Android SDK 35 si falta.
4. Aceptar las licencias del SDK desde Android Studio.
5. Pulsar **Sync Project with Gradle Files**.

`local.properties` se crea automáticamente con la ruta del SDK de cada ordenador y está excluido de Git. No se debe copiar desde otro equipo.

## 5. Verificaciones antes de compilar

Conviene revisar lo siguiente, sin inventar nada raro:

1. `applicationId` debe seguir siendo `com.relajosoft.floatingscanner`.
2. `versionCode` debe aumentar en cada actualización.
3. `versionName` debe identificar claramente la entrega.
4. No debe existir el permiso `INTERNET`.
5. No debe aparecer ninguna referencia a `AccessibilityService`.
6. Las versiones de dependencias no deben cambiar sin revisión.

En Windows se puede comprobar el código con:

```powershell
.\gradlew.bat clean lint assembleDebug
```

En macOS o Linux:

```bash
./gradlew clean lint assembleDebug
```

## 6. Generar un APK de prueba

1. Seleccionar la variante `debug`.
2. Ir a **Build > Build APK(s)**.
3. El APK se genera normalmente en `app/build/outputs/apk/debug/app-debug.apk`.
4. Instalarlo solo en una PDA de prueba.

También puede instalarse desde Android Studio con **Run**, o mediante ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 7. Generar el APK corporativo firmado

1. Abrir **Build > Generate Signed Bundle / APK**.
2. Seleccionar **APK**.
3. Seleccionar la clave corporativa existente o crear una nueva una sola vez.
4. Guardar el archivo `.jks`, su contraseña, el alias y la contraseña del alias fuera del repositorio.
5. Elegir la variante `release`.
6. Activar las firmas V1 y V2; Android Studio puede ofrecer esquemas adicionales según la versión.
7. Generar el APK y conservar el informe de compilación.

La misma clave debe firmar todas las actualizaciones. Si se usa otra, Android rechazará la instalación encima de la versión anterior. Esto sí hay que cuidarlo, porque después no hay arreglo sencillo en todas las PDA.

## 8. Comprobar la huella del APK

Antes de distribuirlo, calculo y entrego una huella SHA-256 para verificar que el archivo no cambió.

PowerShell:

```powershell
Get-FileHash .\app-release.apk -Algorithm SHA256
```

macOS o Linux:

```bash
sha256sum app-release.apk
```

La empresa debe guardar esa huella junto al APK aprobado.

## 9. Configuración inicial en cada PDA

1. Instalar el APK firmado.
2. Abrir **RelajoSoft Scanner**.
3. Conceder cámara y notificaciones.
4. Pulsar **Habilitar RelajoSoft Scanner** y activar el IME.
5. Pulsar **Elegir método de entrada** y seleccionar RelajoSoft Scanner.
6. Permitir **Mostrar sobre otras aplicaciones**.
7. Activar el botón flotante.
8. Abrir la aplicación de trabajo, tocar su campo de entrada y realizar una prueba.

El teclado manual queda disponible para productos cuya etiqueta esté rota o no se lea. Después de escanear, el teclado se mantiene oculto y solo vuelve cuando el operario toca el input.

## 10. Funcionamiento operativo

1. El operario toca el campo donde debe entrar el código.
2. Pulsa **ESCANEAR** en el teclado o el botón flotante.
3. La cámara aparece encima sin sustituir la app de trabajo.
4. Toda la pantalla sirve como zona de lectura; no existe marco de encuadre.
5. ML Kit intenta la orientación normal y después una rotación de 90 grados.
6. Se aplican los filtros configurados.
7. El IME escribe el valor en el campo activo y, si corresponde, envía ENTER.
8. Se apaga la cámara, se liberan recursos y el teclado permanece oculto.

## 11. Pruebas de aceptación

- Código 1D horizontal.
- Código 1D colocado de lado con la PDA recta.
- QR válido.
- QR con W/w cuando el filtro está activado.
- Código terminado en -M y -m con la opción activada y desactivada.
- ENTER activado y desactivado.
- Linterna activada y desactivada.
- Escritura manual, espacio, borrar, mayúsculas, símbolos y ENTER.
- Etiqueta dañada para confirmar que no se escribe un valor incorrecto.
- Diez escaneos seguidos para comprobar que no hay duplicados.
- Confirmar que el teclado no reaparece después de escanear.
- Reiniciar la PDA y comprobar la política corporativa para el servicio flotante.

## 12. Controles de seguridad que pueden revisar

- `AndroidManifest.xml`: permisos mínimos y ausencia de Accesibilidad/Internet.
- `ScannerInputMethodService.kt`: escritura mediante API oficial del IME.
- `FloatingBubbleService.kt`: overlay no enfocable, servicio no exportado y liberación de cámara.
- `Prefs.kt`: únicamente preferencias booleanas privadas.
- `settings.gradle.kts`: repositorios limitados a Google y Maven Central.
- `.gitignore`: exclusión de claves, rutas locales y APK generadas.
- `SECURITY.md`: riesgos conocidos y medidas aplicadas.

## 13. Crear y compartir el repositorio privado

En GitHub:

1. Crear un repositorio nuevo.
2. Marcarlo como **Private**.
3. No añadir README ni `.gitignore` desde GitHub porque ya vienen incluidos.
4. Copiar la URL del repositorio.

En la carpeta del proyecto:

```bash
git init
git add .
git status
git commit -m "Entrega RelajoSoft Scanner v0.8.0 para revision empresarial"
git branch -M main
git remote add origin URL_DEL_REPOSITORIO_PRIVADO.git
git push -u origin main
```

Después, en **Settings > Collaborators and teams**, invitar únicamente a los informáticos autorizados. Para una revisión sin cambios, conceder lectura. Para proponer correcciones mediante ramas y pull requests, conceder escritura según la política de la empresa.

No se deben subir:

- Claves `.jks` o `.keystore`.
- Contraseñas o archivos de firma.
- `local.properties`.
- APK corporativas firmadas dentro del historial Git.
- Capturas o datos reales de la aplicación de trabajo.

## 14. Proceso recomendado para cambios

1. Crear una rama con un nombre descriptivo.
2. Hacer cambios pequeños y comentados.
3. Ejecutar `lint` y compilar.
4. Probar en una PDA real.
5. Abrir un pull request y pedir revisión.
6. Aumentar `versionCode` y documentar el cambio.
7. Firmar la versión aprobada con la clave corporativa.

Como decimos en Cuba, aquí cada cosa tiene su lugar: el código en GitHub privado, la clave en el almacén seguro y el APK aprobado en el canal de distribución de la empresa.
