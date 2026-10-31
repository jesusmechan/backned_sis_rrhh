# Despliegue — Neon + Render + Cloudflare Pages

Rama de despliegue: **`jesus-pantoja`**.  
Los puertos **8080** (API) y **5173** (Vite) son solo para desarrollo local; en producción Render usa la variable **`PORT`** y Pages sirve por **HTTPS**.

## 1. Neon (PostgreSQL)

1. Crear proyecto en [Neon](https://neon.tech).
2. En SQL Editor, ejecutar `database/01_install.sql` (crea esquema, menú y cuentas demo).
3. Copiar cadena de conexión; ejemplo JDBC para Render:

```text
DB_URL=jdbc:postgresql://<host>/<db>?sslmode=require
DB_USERNAME=<usuario>
DB_PASSWORD=<contraseña>
```

## 2. Render (backend)

1. Conectar el repositorio Git; rama **`jesus-pantoja`**.
2. **Root directory:** `backend` · **Runtime:** Docker (`backend/Dockerfile`).
3. Variables de entorno (mínimo):

| Variable | Ejemplo |
|----------|---------|
| `DB_URL` | JDBC Neon con `?sslmode=require` |
| `DB_USERNAME` | usuario Neon |
| `DB_PASSWORD` | contraseña Neon |
| `JWT_SECRET` | ≥ 32 caracteres (única en producción) |
| `CORS_ORIGINS` | `https://<tu-proyecto>.pages.dev` (sin barra final) |
| `PUBLIC_API_URL` | `https://<tu-servicio>.onrender.com` (Swagger) |

4. Health check: `/api/configuracion/publica`.
5. Opcional: importar `render.yaml` en el dashboard de Render.

Tras el deploy, anota la URL pública del API (HTTPS).

## 3. Cloudflare Pages (frontend)

1. Conectar repo; rama **`jesus-pantoja`**.
2. **Root directory:** `frontend`
3. **Build command:** `npm ci && npm run build`
4. **Build output:** `dist`
5. **Environment variable (Production):**

```text
VITE_API_BASE_URL=https://<tu-servicio>.onrender.com
```

(sin barra final; mismo host que el API en Render)

6. `_redirects` en `frontend/public/` evita 404 al refrescar rutas de React.

Vuelve a Render y confirma que `CORS_ORIGINS` incluye la URL exacta de Pages.

## 4. App móvil (Flutter)

No se despliega en estos tres servicios. En la app:

- **Servidor API:** `https://<tu-servicio>.onrender.com`, o
- Build: `flutter build apk --dart-define=API_URL=https://<tu-servicio>.onrender.com`

## 5. Desarrollo local (sin cambiar producción)

```bash
# BD local + backend/.env
cd backend && mvn spring-boot:run

cd frontend && npm install && npm run dev
```

Sin `VITE_API_BASE_URL`, el front usa proxy Vite (`/api` → backend local). En macOS puedes levantar el API con `mvn spring-boot:run` en `backend/`.

## 6. Cuentas demo

Contraseña por defecto tras `01_install.sql`: **Andina2026** (cámbiala en producción).
