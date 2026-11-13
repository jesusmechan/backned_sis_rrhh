# Andina RR. HH. — App móvil (Flutter)

Cliente móvil del **Sistema de Gestión de RR. HH.** de Consultora Contable Andina S.A.C.  
Consume la API Spring Boot (`/api`) en local o en la nube (Render).

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

## APK listo para instalar

En el repo:

```
mobile/releases/andina-rrhh-arm64-v1.0.0.apk
```

- Arquitectura: **arm64-v8a** (la mayoría de celulares Android actuales)
- API por defecto: `https://rrhh-api-2bgd.onrender.com`
- En el celular: transferir el APK → permitir «instalar apps desconocidas» → abrir e instalar

> Si el dispositivo es muy antiguo (solo 32-bit), genera de nuevo con `flutter build apk --release` (APK universal) o `--split-per-abi`.

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

## Generar APK de release

El `build.gradle.kts` limita release a **arm64-v8a** (APK más liviano).

```bash
cd mobile
flutter build apk --release \
  --dart-define=API_URL=https://rrhh-api-2bgd.onrender.com

# Salida:
# build/app/outputs/flutter-apk/app-release.apk
```

Copia a `releases/` para versionarla en el repo:

```bash
mkdir -p releases
cp build/app/outputs/flutter-apk/app-release.apk \
  releases/andina-rrhh-arm64-v1.0.0.apk
```


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
├── releases/            # APK versionados para descarga
├── test/
└── pubspec.yaml
```

## Notas

- La hora de marcación la registra el **servidor** (zona `America/Lima`), no el reloj del celular.
- Los recordatorios de marcación son **solo notificaciones locales**; no marcan asistencia solos.
- Guía de nube (Neon + Render + Pages): [docs/DEPLOY.md](../docs/DEPLOY.md)
- Documentación general del monorepo: [README.md](../README.md)
