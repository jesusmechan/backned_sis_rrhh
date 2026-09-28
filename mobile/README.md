# Andina RR. HH. — App móvil (Flutter)

Cliente móvil del sistema de Gestión de RR. HH. Consume el API en `:8080`.

## Herramientas en disco D

Todo el tooling vive fuera de C: en `D:\AndinaRRHH\`:

| Carpeta | Contenido |
|---|---|
| `D:\AndinaRRHH\flutter` | SDK Flutter |
| `D:\AndinaRRHH\android-sdk` | Android SDK + emulador |
| `D:\AndinaRRHH\avd` | AVD `andina_pixel` |
| `%LOCALAPPDATA%\Pub\Cache` | Caché de paquetes Dart (en C:; Kotlin no admite C:+D: mezclados) |
| `D:\AndinaRRHH\gradle-cache` | Caché Gradle |
| `D:\AndinaRRHH\downloads` | Descargas temporales |

El código de la app está en este repo: `Proyecto_RRHH/mobile/`.

## Funcionalidades

- Login JWT
- Marcar asistencia (origen `MOVIL`)
- Permisos (listar / solicitar)
- Notificaciones
- Perfil / logout

## Cómo ejecutar en el emulador

Abre una **nueva** terminal (para cargar PATH de usuario) o ejecuta:

```powershell
$env:Path = "D:\AndinaRRHH\flutter\bin;D:\AndinaRRHH\android-sdk\platform-tools;D:\AndinaRRHH\android-sdk\emulator;" + $env:Path
$env:ANDROID_HOME = "D:\AndinaRRHH\android-sdk"
$env:ANDROID_SDK_ROOT = "D:\AndinaRRHH\android-sdk"
$env:ANDROID_AVD_HOME = "D:\AndinaRRHH\avd"
$env:GRADLE_USER_HOME = "D:\AndinaRRHH\gradle-cache"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"

# 1) Backend en :8080
# 2) Arrancar emulador
emulator -avd andina_pixel

# 3) En otra terminal, correr la app
cd "...\Proyecto_RRHH\mobile"
flutter run
```

**Importante:** usa **PowerShell**, no Git Bash (en bash `$env:Path` no funciona).

Alternativa rápida sin emulador: `flutter run -d chrome`

## Usuarios de prueba

`juan.espinoza` / `Andina2026`
