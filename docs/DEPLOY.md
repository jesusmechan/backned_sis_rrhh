# Guía paso a paso — Despliegue en la nube

Despliegue del **Sistema RRHH Andina** con:

| Capa | Servicio | URL del panel |
|------|----------|----------------|
| Base de datos | [Neon](https://neon.tech) | [console.neon.tech](https://console.neon.tech) |
| Backend (Spring Boot) | [Render](https://render.com) | [dashboard.render.com](https://dashboard.render.com) |
| Frontend (React + Vite) | [Cloudflare Pages](https://pages.cloudflare.com) | [dash.cloudflare.com](https://dash.cloudflare.com) → **Workers & Pages** |

**Repositorio:** `jesusmechan/backned_sis_rrhh`  
**Rama de despliegue:** `jesus-pantoja`

Los puertos **8080** y **5173** son solo para desarrollo local. En producción, Render usa la variable **`PORT`** y Pages publica por **HTTPS** (`*.pages.dev` o dominio propio).

---

## Antes de empezar

- [ ] Código subido a GitHub en la rama **`jesus-pantoja`**.
- [ ] Cuenta en [Neon](https://console.neon.tech/signup), [Render](https://dashboard.render.com/register) y [Cloudflare](https://dash.cloudflare.com/sign-up).
- [ ] En Render, conectar GitHub: **Account Settings** → **Account Security** → **Git Deployment Credentials** → **Add credential** → GitHub → autorizar.  
  Documentación: [Connect your Git provider](https://render.com/docs/git-provider).

**Orden recomendado:** Neon → Render → Cloudflare Pages → ajustar CORS en Render → probar web y móvil.

---

## Parte 1 — Base de datos en Neon

### Qué hay en `database/` (revisar antes de ejecutar nada)

Los scripts del repo **no** los ejecuta Neon ni Render automáticamente; tú los corres una vez contra la base en la nube. Detalle del modelo: [docs/BASE_DE_DATOS.md](BASE_DE_DATOS.md).

| Archivo | ¿En Neon? | Función |
|---------|-----------|---------|
| **`00_create_database.sql`** | **Normalmente no** | Crea la base **`rrhh_andina`** conectado a `postgres` en **pgAdmin local**. Neon ya trae una base (`neondb`); no hace falta este archivo salvo que quieras otra base con ese nombre vía `CREATE DATABASE`. |
| **`01_install.sql`** | **Sí — obligatorio** | Instalación **completa** (~2 180 líneas): `DROP SCHEMA public CASCADE`, tipos ENUM, tablas, índices, **funciones y triggers** de aprobación/planilla, menú, catálogos, 4 usuarios demo, marcaciones seed (zona **`America/Lima`**). Requiere extensión **`pgcrypto`**. Contraseña demo: **`Andina2026`**. |
| **`02_reset.sql`** | **Solo después** | Limpia trámites y deja las 4 cuentas; **no** sustituye a `01_install.sql`. Úsalo para resetear demo en la misma base. |

**Compatibilidad con el backend Spring:** al final, `01_install.sql` inserta en `script_aplicado` el código **`06_configuracion_dinamica`**, para que al arrancar Render **no** vuelva a aplicar duplicados de `backend/src/main/resources/db/06_configuracion_dinamica.sql`. Aun así, **debes** ejecutar `01_install.sql` en Neon **antes** del primer deploy; el `MenuSchemaInitializer` del backend **no** reemplaza la instalación completa.

**PostgreSQL:** el proyecto pide **16+**; los triggers usan `EXECUTE FUNCTION` (sintaxis PG14+). Elige Postgres 16–18 al crear el proyecto Neon.

**Precauciones en Neon:**

- `01_install.sql` empieza destruyendo **`public`** en la base seleccionada; no lo ejecutes en una base compartida con otros datos.
- El script es **grande**; si el SQL Editor corta o hace timeout, usa **`psql`** con la connection string de Neon o ejecuta por bloques (DDL → triggers → INSERTs).
- El nombre **`rrhh_andina`** en JDBC es **opcional**; lo importante es que `DB_URL` apunte a la base donde corriste `01_install.sql` (`neondb` o `rrhh_andina`).

### 1.1 Crear proyecto

1. Entra a **[console.neon.tech](https://console.neon.tech)** e inicia sesión.
2. Pulsa **New project**.
3. **Project name:** por ejemplo `rrhh-andina`.
4. **Region:** elige la más cercana (p. ej. AWS `US East` o la que Neon muestre para tu zona).
5. **Postgres:** deja activada la base Postgres (versión 16+ recomendada; Neon suele ofrecer 17–18).
6. **Create project**.

Neon crea por defecto:

- Rama **`production`** (en consola; en API puede llamarse `main`).
- Base de datos **`neondb`**.
- Rol propietario (p. ej. `neondb_owner`).

Documentación: [Create a project](https://neon.com/docs/manage/projects).

### 1.2 (Opcional) Base con nombre `rrhh_andina`

El script local `database/00_create_database.sql` está pensado para pgAdmin. En Neon puedes:

- **Opción A (simple):** usar **`neondb`** y ejecutar solo `01_install.sql` ahí, **o**
- **Opción B:** en **SQL Editor**, ejecutar `CREATE DATABASE rrhh_andina;`, cambiar la base activa a `rrhh_andina` y luego ejecutar `01_install.sql`.

### 1.3 Instalar esquema y datos

1. En el proyecto Neon: menú lateral **Postgres database** → **SQL Editor**.
2. Arriba, selecciona rama **`production`** y base **`neondb`** (o `rrhh_andina`).
3. Abre en tu repo el archivo **`database/01_install.sql`**, copia **todo** el contenido y pégalo en el editor.
4. Pulsa **Run** (puede tardar 1–2 minutos).

Si el editor falla por tamaño o tiempo, divide el script en bloques o usa **`psql`** con la cadena de conexión de Neon (ver **Connection details** en el panel).

5. Comprueba la instalación:

   ```sql
   SELECT COUNT(*) FROM usuario;          -- debe ser 4
   SELECT codigo FROM script_aplicado;    -- debe incluir 06_configuracion_dinamica
   ```

**Contraseña demo** de usuarios tras la instalación: **`Andina2026`** (cámbiala en un entorno real). Usuarios: ver [docs/PRUEBAS.md](PRUEBAS.md).

### 1.4 Guardar credenciales para Render

1. En Neon: **Dashboard** del proyecto → **Connect** o **Connection details**.
2. Copia los datos (host, database, user, password). Neon muestra URI tipo:

   `postgresql://USUARIO:CONTRASEÑA@ep-xxxxx.region.aws.neon.tech/neondb?sslmode=require`

3. Conviértelos a variables para Spring:

| Variable Render | Valor |
|-----------------|--------|
| `DB_USERNAME` | usuario (p. ej. `neondb_owner`) |
| `DB_PASSWORD` | contraseña generada por Neon |
| `DB_URL` | `jdbc:postgresql://ep-xxxxx.region.aws.neon.tech/neondb?sslmode=require` |

**Importante:** la URL JDBC debe incluir **`?sslmode=require`**. No uses `localhost` ni el puerto 5432 de tu PC.

---

## Parte 2 — Backend en Render

El repo incluye **`backend/Dockerfile`** y opcionalmente **`render.yaml`**.

### 2.1 Crear Web Service

1. **[dashboard.render.com](https://dashboard.render.com)** → arriba a la derecha **+ New** → **Web Service**.
2. Conecta el repo **`backned_sis_rrhh`** (GitHub) si aún no aparece.
3. Configura el formulario:

| Campo | Valor |
|--------|--------|
| **Name** | `rrhh-api` (define el subdominio `rrhh-api.onrender.com`) |
| **Region** | Cercana a Neon si puedes (p. ej. Oregon / Ohio) |
| **Branch** | `jesus-pantoja` |
| **Root Directory** | `backend` |
| **Language / Runtime** | **Docker** |
| **Dockerfile Path** | `./Dockerfile` (relativo a `backend/`) |
| **Instance type** | **Free** (para pruebas; ver limitaciones abajo) |

4. **Advanced** → **Health Check Path:** `/api/configuracion/publica`

Documentación: [Docker on Render](https://render.com/docs/docker) · [Web Services](https://render.com/docs/web-services).

### 2.2 Variables de entorno

En la misma creación (o después en **Environment** del servicio), añade:

| Key | Value |
|-----|--------|
| `DB_URL` | JDBC de Neon (paso 1.4) |
| `DB_USERNAME` | usuario Neon |
| `DB_PASSWORD` | contraseña Neon |
| `JWT_SECRET` | cadena aleatoria **≥ 32 caracteres** (no reutilices `.env.example`) |
| `CORS_ORIGINS` | Lo completarás en el paso 4 con la URL de Pages |
| `PUBLIC_API_URL` | `https://<nombre-servicio>.onrender.com` (sin barra final) |

Opcionales (defaults en `application.yml`):

- `RATE_LIMIT_ENABLED`, `RATE_LIMIT_API`, `RATE_LIMIT_AUTH`
- `IDEMPOTENCY_ENABLED`, `IDEMPOTENCY_TTL_HOURS`

Render **no** usa el archivo `backend/.env` del repo; solo variables del panel.

### 2.3 Desplegar y probar

1. **Create Web Service**. Render construye la imagen Docker y despliega (pestaña **Logs** / **Events**).
2. La primera vez en plan **Free** puede tardar varios minutos.
3. Cuando el estado sea **Live**, abre:

   - `https://<tu-servicio>.onrender.com/api/configuracion/publica` → debe responder JSON.
   - `https://<tu-servicio>.onrender.com/swagger-ui.html` → Swagger (opcional).

4. Anota la URL base del API, por ejemplo:

   **`https://rrhh-api.onrender.com`**

**Plan Free:** el servicio **se suspende** tras inactividad; la primera petición puede tardar ~30–60 s en despertar. Para demo en vivo, tenlo en cuenta.

**Login de prueba (API):**

```http
POST https://<tu-servicio>.onrender.com/api/auth/login
Content-Type: application/json

{"nombreUsuario":"juan.espinoza","password":"Andina2026"}
```

---

## Parte 3 — Frontend en Cloudflare Pages

### 3.1 Crear proyecto Pages

1. **[dash.cloudflare.com](https://dash.cloudflare.com)** → menú **Workers & Pages**.
2. **Create** → pestaña **Pages** → **Connect to Git** (o **Import an existing Git repository**).
3. Autoriza **GitHub** si te lo pide.
4. Elige el repo **`backned_sis_rrhh`** → **Begin setup**.

Documentación: [Git integration](https://developers.cloudflare.com/pages/get-started/git-integration/) · [Build configuration](https://developers.cloudflare.com/pages/configuration/build-configuration/).

### 3.2 Build settings

| Campo | Valor |
|--------|--------|
| **Project name** | p. ej. `rrhh-andina` → URL `https://rrhh-andina.pages.dev` |
| **Production branch** | `jesus-pantoja` |
| **Framework preset** | **Vite** (o **None** y rellena a mano) |
| **Build command** | `npm ci && npm run build` |
| **Build output directory** | `dist` |
| **Root directory** (monorepo) | `frontend` |

En **Environment variables** (sección **Build** o **Settings → Environment variables** después del primer deploy):

| Variable | Entorno | Valor |
|----------|---------|--------|
| `VITE_API_BASE_URL` | **Production** | `https://<tu-servicio>.onrender.com` |

Sin barra final. Debe ser la **misma URL HTTPS** del API en Render.

3. **Save and Deploy** (o **Deploy**).

4. Al terminar, abre **`https://<project-name>.pages.dev`**.

El repo ya incluye **`frontend/public/_redirects`** para que React Router no devuelva 404 al refrescar rutas (`/login`, `/bandeja`, etc.).

### 3.3 Volver a Render — CORS

1. **dashboard.render.com** → servicio **`rrhh-api`** → **Environment**.
2. Edita **`CORS_ORIGINS`** con la URL **exacta** de Pages, por ejemplo:

   `https://rrhh-andina.pages.dev`

   Si usas dominio custom en Cloudflare, añádelo separado por coma:

   `https://rrhh-andina.pages.dev,https://app.tudominio.com`

3. Guarda; Render redeploya solo.

---

## Parte 4 — Comprobación end-to-end

| # | Qué probar | Resultado esperado |
|---|------------|-------------------|
| 1 | `GET` configuración pública en Render | JSON 200 |
| 2 | Abrir Pages | Pantalla de login carga (llama al API vía `VITE_API_BASE_URL`) |
| 3 | Login web `juan.espinoza` / `Andina2026` | Entra al menú según rol |
| 4 | Campana / notificaciones | STOMP contra el mismo host del API (Render) |
| 5 | Refrescar una ruta interna | No 404 (gracias a `_redirects`) |

Si el login falla con error de red en el navegador:

- Revisa **`VITE_API_BASE_URL`** en Pages (Production) y vuelve a desplegar.
- Revisa **`CORS_ORIGINS`** en Render (origen = URL del front, sin `/` final).
- Abre DevTools → **Network** y mira si las peticiones van a `onrender.com`.

---

## Parte 5 — App móvil (Flutter)

La app **no** se sube a Neon/Render/Pages. Solo necesita la URL HTTPS del API.

1. Instala la app en dispositivo o emulador.
2. En **login** o **Perfil → Servidor API**, indica:

   `https://<tu-servicio>.onrender.com`

3. O compila con URL fija:

   ```bash
   cd mobile
   flutter build apk --dart-define=API_URL=https://<tu-servicio>.onrender.com
   ```

Usuarios de prueba: ver [docs/PRUEBAS.md](PRUEBAS.md).

---

## Parte 6 — Desarrollo local (sin afectar la nube)

```bash
# PostgreSQL local + backend/.env
cd backend && mvn spring-boot:run

cd frontend && npm install && npm run dev
```

Sin `VITE_API_BASE_URL`, el front en local usa el proxy de Vite (`/api` → `127.0.0.1:8080`). Ver [README.md](../README.md).

---

## Referencia rápida de archivos del repo

| Archivo | Uso |
|---------|-----|
| `backend/Dockerfile` | Imagen Java 21 para Render |
| `render.yaml` | Plantilla Blueprint (opcional) |
| `frontend/.env.example` | Documenta `VITE_API_BASE_URL` |
| `frontend/src/api/baseUrl.js` | Prefijo del API en producción |
| `database/01_install.sql` | Instalación Neon |
| `docs/DEPLOY.md` | Esta guía |

---

## Problemas frecuentes

### Render: deploy falla o health check rojo

- Revisa **Logs**: error de conexión a Neon → `DB_URL` / SSL / usuario.
- Falta **`JWT_SECRET`** → el arranque falla.
- **`DB_PASSWORD`** vacía → obligatoria en `application.yml`.

### Render: 502 o muy lento

- Plan Free **dormido**; espera y reintenta.
- Comprueba health: `/api/configuracion/publica`.

### Pages: login no conecta

- Falta **`VITE_API_BASE_URL`** o deploy viejo sin rebuild.
- **CORS** en Render no incluye tu `*.pages.dev`.

### Neon: error al ejecutar `01_install.sql`

- Archivo muy grande → `psql` o trocear el script.
- Permisos → usa el rol owner de la base seleccionada.

### Swagger en Render

- UI: `https://<servicio>.onrender.com/swagger-ui.html`
- Servidor en Swagger usa **`PUBLIC_API_URL`** si la definiste.

---

## Enlaces oficiales (2026)

- Neon Console: [https://console.neon.tech](https://console.neon.tech)
- Neon SQL Editor: [https://neon.com/docs/get-started/query-with-neon-sql-editor](https://neon.com/docs/get-started/query-with-neon-sql-editor)
- Render Dashboard: [https://dashboard.render.com](https://dashboard.render.com)
- Render — primer deploy: [https://render.com/docs/your-first-deploy](https://render.com/docs/your-first-deploy)
- Cloudflare Dashboard: [https://dash.cloudflare.com](https://dash.cloudflare.com)
- Cloudflare Pages — Git: [https://developers.cloudflare.com/pages/get-started/git-integration/](https://developers.cloudflare.com/pages/get-started/git-integration/)

---

## Checklist final

- [ ] Neon: `01_install.sql` ejecutado
- [ ] Render: servicio **Live**, env completo, health OK
- [ ] Pages: build OK, `VITE_API_BASE_URL` en Production
- [ ] Render: `CORS_ORIGINS` = URL de Pages
- [ ] Web: login OK
- [ ] Móvil: servidor = URL Render HTTPS
