# Andina RR. HH. — App móvil (Flutter)

Cliente móvil del **Sistema de Gestión de RR. HH.** de Consultora Contable Andina S.A.C.  
Consume la API Spring Boot (`/api`) en local o en la nube (Render).

## Descargar e instalar

### Android (APK)

**[⬇ Descargar APK v1.0.0 (arm64)](https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-arm64-v1.0.0.apk)**

| | |
|---|---|
| Archivo | [`andina-rrhh-arm64-v1.0.0.apk`](https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-arm64-v1.0.0.apk) (~57 MB) |
| Arquitectura | **arm64-v8a** (celulares Android actuales) |
| API | `https://rrhh-api-2bgd.onrender.com` |
| Carpeta en el repo | [`mobile/releases/`](releases/) |

1. Abre el enlace de descarga **desde el propio teléfono** (Chrome / navegador).
2. Si pide permiso, permite **instalar apps de orígenes desconocidos** para el navegador.
3. Abre el APK descargado e instala **Andina RRHH**.
4. Usuario de prueba: `juan.espinoza` / `Andina2026`.

> Enlace directo:  
> `https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-arm64-v1.0.0.apk`

> Si el dispositivo es muy antiguo (solo 32-bit), genera un APK universal con `flutter build apk --release`.

### iOS (IPA + Sideloadly en Windows)

**[⬇ Descargar IPA v1.0.0](https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-ios-v1.0.0.ipa)** (~9 MB · arm64 · iOS 15+)

En iPhone **no** se puede instalar abriendo el archivo como en Android. El compañero con PC Windows usa **Sideloadly** + cable USB + su Apple ID.

**Guía para el compañero (paso a paso):**  
[`docs/INSTALAR_IOS_SIDELOADLY.md`](docs/INSTALAR_IOS_SIDELOADLY.md)

Resumen: Sideloadly → arrastrar IPA → Apple ID → Start → en el iPhone confiar el perfil en Ajustes → General → VPN y gestión de dispositivos. Con Apple ID gratis la firma caduca ~cada 7 días (hay que volver a sideloadear).

## Qué incluye

| Área | Detalle |
|------|---------|
| Auth | Login JWT, sesión persistente, logout |
| Inicio | Resumen del día, accesos rápidos, pendientes |
| Marcar | Entrada / salida (origen `MOVIL`, hora Lima del servidor) |
| Solicitudes | Permisos, vacaciones, horas extras |
| Avisos | Notificaciones del circuito de aprobación |
| Perfil | Datos del colaborador, saldo de vacaciones, ajustes |
| Configuración | Tema claro/oscuro y URL del servidor API |
| Recordatorios | Notificaciones **locales** para avisar marcación (entrada/salida) |

## Requisitos de desarrollo

- Flutter 3.24+ (stable)
- JDK 17+
- Android SDK (API 34–37)
- Dispositivo/emulador Android, o Chrome para smoke test

## Cómo ejecutar

### 1) Dependencias

```bash
cd mobile
flutter pub get
```

### 2) API en la nube (recomendado)

```bash
flutter run --dart-define=API_URL=https://rrhh-api-2bgd.onrender.com
```

### 3) API local

Backend en `:8080`, luego:

```bash
# Emulador Android (10.0.2.2 = localhost del host)
flutter run --dart-define=API_URL=http://10.0.2.2:8080

# Dispositivo físico en la misma Wi‑Fi (usa la IP de tu PC)
flutter run --dart-define=API_URL=http://192.168.x.x:8080

# USB + adb reverse
adb reverse tcp:8080 tcp:8080
flutter run --dart-define=API_URL=http://127.0.0.1:8080
```

También puedes cambiar el servidor desde **Perfil → Configuración → Servidor API**.

### Hot reload

Con `flutter run` abierto: `r` = hot reload, `R` = hot restart. Evita `flutter build apk` en cada prueba.

## Generar APK de release (Android)

El `build.gradle.kts` limita release a **arm64-v8a** (APK más liviano).

```bash
cd mobile
flutter build apk --release \
  --dart-define=API_URL=https://rrhh-api-2bgd.onrender.com

# Salida:
# build/app/outputs/flutter-apk/app-release.apk
```

```bash
mkdir -p releases
cp build/app/outputs/flutter-apk/app-release.apk \
  releases/andina-rrhh-arm64-v1.0.0.apk
```

## Generar IPA de release (iOS, para Sideloadly)

Requiere **Mac + Xcode**. Se genera **sin firma** (`--no-codesign`); Sideloadly firma al instalar en el iPhone del compañero.

```bash
cd mobile
flutter build ipa --release --no-codesign \
  --dart-define=API_URL=https://rrhh-api-2bgd.onrender.com

# Archive:
# build/ios/archive/Runner.xcarchive
```

Empaquetar el `.ipa` desde el `.app` del archive (Flutter con `--no-codesign` no exporta el IPA solo):

```bash
APP=build/ios/archive/Runner.xcarchive/Products/Applications/Runner.app
WORKDIR=$(mktemp -d)
mkdir -p "$WORKDIR/Payload"
cp -R "$APP" "$WORKDIR/Payload/Runner.app"
mkdir -p releases
OUT="$PWD/releases/andina-rrhh-ios-v1.0.0.ipa"
rm -f "$OUT"
( cd "$WORKDIR" && zip -r -y "$OUT" Payload )
rm -rf "$WORKDIR"
ls -lh releases/andina-rrhh-ios-v1.0.0.ipa
```

Guía de instalación para Windows: [`docs/INSTALAR_IOS_SIDELOADLY.md`](docs/INSTALAR_IOS_SIDELOADLY.md).


## Usuarios de prueba

Contraseña: **`Andina2026`**

| Usuario | Rol |
|---------|-----|
| `juan.espinoza` | EMPLEADO |
| `jesus.pantoja` | JEFE |
| `carla.reyes` | RRHH |
| `jesus.mechan` | ADMIN |

## Estructura

```
mobile/
├── lib/
│   ├── config/          # API_URL / resolución de base
│   ├── models/
│   ├── providers/       # Auth, tema
│   ├── screens/
│   ├── services/        # API client, recordatorios locales
│   ├── theme/
│   └── widgets/
├── android/
├── ios/
├── docs/                # Guías de instalación (p. ej. Sideloadly)
├── releases/            # APK / IPA versionados para descarga
├── test/
└── pubspec.yaml
```

## Notas

- La hora de marcación la registra el **servidor** (zona `America/Lima`), no el reloj del celular.
- Los recordatorios de marcación son **solo notificaciones locales**; no marcan asistencia solos.
- iOS: instalación con Sideloadly → [`docs/INSTALAR_IOS_SIDELOADLY.md`](docs/INSTALAR_IOS_SIDELOADLY.md).
- Guía de nube (Neon + Render + Pages): [docs/DEPLOY.md](../docs/DEPLOY.md)
- Documentación general del monorepo: [README.md](../README.md)
