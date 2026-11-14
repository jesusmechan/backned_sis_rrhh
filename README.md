# Sistema de Gestión de RR. HH. — Consultora Contable Andina S.A.C.

Proyecto del *Curso Integrador I — Sistemas Software* (UTP).  
Origen funcional: **Plan de Toma de Requerimientos v1.0**.

La consultora es una MYPE. El sistema cubre personal, contratos, asistencia, permisos, horas extras, planillas, remuneraciones, contabilidad de planilla, desempeño, reclutamiento, usuarios, reportes y auditoría. El circuito de aprobación **no guarda al aprobador en la solicitud**: se configura por tipo de trámite y se instancia paso a paso.

## Stack

| Capa | Tecnología |
|------|------------|
| Base de datos | PostgreSQL 16+ (local o [Neon](https://neon.tech)) |
| API | Java 21, Spring Boot 3.4.x, Spring Security, JPA |
| Auth | JWT (access ~15 min + refresh 7 días) |
| Exportes | Excel (Apache POI) y PDF (OpenPDF) |
| Frontend web | React 19 + Vite |
| App móvil | Flutter (Android) — carpeta [`mobile/`](mobile/) |

## Estructura del monorepo

```
backned_sis_rrhh/
├── README.md                 # Este archivo
├── docs/                     # Arquitectura, API, BD, pruebas, deploy
├── database/                 # Scripts SQL (install / reset)
├── backend/                  # API Spring Boot (:8080)
├── frontend/                 # React + Vite (:5173)
└── mobile/                   # App Flutter + APK en releases/
```

## Inicio rápido

| Qué | Cómo |
|-----|------|
| Base de datos | `database/00_create_database.sql` → `01_install.sql` (detalle abajo) |
| Backend | `cd backend && cp .env.example .env && mvn spring-boot:run` |
| Frontend | `cd frontend && npm install && npm run dev` |
| Móvil | Ver [`mobile/README.md`](mobile/README.md) |
| Nube | Guía completa: [`docs/DEPLOY.md`](docs/DEPLOY.md) |

### Cuentas demo (contraseña `Andina2026`)

| Usuario | Rol |
|---------|-----|
| `jesus.mechan` | ADMIN |
| `carla.reyes` | RRHH |
| `jesus.pantoja` | JEFE |
| `juan.espinoza` | EMPLEADO |

Recorridos de prueba: [`docs/PRUEBAS.md`](docs/PRUEBAS.md).

---

## 1. Base de datos (pgAdmin / local)

1. Query Tool sobre **`postgres`**, con **Auto commit** activo.
2. Ejecutar `database/00_create_database.sql`.
3. Query Tool sobre **`rrhh_andina`** → ejecutar `01_install.sql`.
4. Para resetear demo: `02_reset.sql`.

Detalle: [`docs/BASE_DE_DATOS.md`](docs/BASE_DE_DATOS.md) · MER: [`docs/MER.md`](docs/MER.md).

## 2. Backend

```bash
cd backend
cp .env.example .env   # Windows: copy .env.example .env
```

Ejemplo local en `.env`:

```
DB_URL=jdbc:postgresql://127.0.0.1:5432/rrhh_andina
DB_USERNAME=postgres
DB_PASSWORD=root
```

```bash
mvn spring-boot:run
```

- API: http://localhost:8080  
- Swagger: http://localhost:8080/swagger-ui.html  
- OpenAPI: http://localhost:8080/v3/api-docs  

Usar `127.0.0.1` (no `localhost`) evita choques IPv6 con Docker en el puerto 5432.

### Login

`POST /api/auth/login`

```json
{ "nombreUsuario": "jesus.mechan", "password": "Andina2026" }
```

Rutas protegidas: `Authorization: Bearer <accessToken>`.  
Referencia: [`docs/API.md`](docs/API.md).

## 3. Frontend web

```bash
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173. Vite proxea `/api` al backend en `:8080`.

## 4. App móvil (Flutter)

Documentación: **[`mobile/README.md`](mobile/README.md)**.

**Android** — [⬇ APK v1.0.0](https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-arm64-v1.0.0.apk) (arm64 · ~57 MB)

**iOS** — [⬇ IPA v1.0.0](https://github.com/jesusmechan/backned_sis_rrhh/raw/main/mobile/releases/andina-rrhh-ios-v1.0.0.ipa) (~9 MB)  
Instalación en iPhone desde Windows: **[`mobile/docs/INSTALAR_IOS_SIDELOADLY.md`](mobile/docs/INSTALAR_IOS_SIDELOADLY.md)** (Sideloadly).

API de producción: `https://rrhh-api-2bgd.onrender.com`  
(también configurable en la app: Perfil → Configuración).
## Configuración dinámica

El código no fija reglas de negocio: salen de la base y se editan en **Maestros** (`MAESTRO_GESTIONAR`).

- Parámetros (empresa, zona, moneda, límites, tasas)
- Catálogos, valores por fecha (RMV, UIT), AFP, regímenes, tramos 5ta
- Roles y permisos funcionales

Semilla: `database/01_install.sql`. En bases ya instaladas, el backend aplica scripts en `backend/src/main/resources/db/`.

## Módulos

| Módulo | Qué cubre |
|--------|-----------|
| Personal | Empleados, organigrama, carga Excel |
| Contratos | Modalidad, régimen, AFP, horario, remuneración, vacaciones |
| Maestros | Áreas, cargos, horarios, tipos de permiso, catálogos, parámetros |
| Permisos / HE | Registro, cancelación, historial |
| Aprobación | Flujos configurables + bandeja |
| Asistencia | Marcación ingreso/salida y corrección |
| Planillas | Boletas mensuales y PDF |
| Contabilidad | Asiento al cerrar planilla |
| Desempeño / Reclutamiento | Evaluación y convocatorias |
| Usuarios / Roles | Cuentas, menú y permisos |
| Reportes / Auditoría | JSON, Excel, PDF y bitácora |

## Documentación

| Documento | Contenido |
|-----------|-----------|
| [`docs/ARQUITECTURA_DE_PROYECTO.md`](docs/ARQUITECTURA_DE_PROYECTO.md) | Hexágono y carpetas |
| [`docs/API.md`](docs/API.md) | Endpoints |
| [`docs/BASE_DE_DATOS.md`](docs/BASE_DE_DATOS.md) / [`docs/MER.md`](docs/MER.md) | Modelo de datos |
| [`docs/PRUEBAS.md`](docs/PRUEBAS.md) | Cuentas y flujos |
| [`docs/DEPLOY.md`](docs/DEPLOY.md) | Neon + Render + Cloudflare Pages |
| [`mobile/README.md`](mobile/README.md) | App Flutter, APK e IPA |
| [`mobile/docs/INSTALAR_IOS_SIDELOADLY.md`](mobile/docs/INSTALAR_IOS_SIDELOADLY.md) | Instalar IPA en iPhone (Windows + Sideloadly) |

**Rama de despliegue en la nube:** `jesus-pantoja` (ver `docs/DEPLOY.md`).
