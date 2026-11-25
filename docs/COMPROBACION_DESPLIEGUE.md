# Comprobación rápida del despliegue (Semana 15)

Guía corta para la exposición junto con [`S15_Presentacion_Despliegue_RRHH_Andina.pptx`](S15_Presentacion_Despliegue_RRHH_Andina.pptx).  
Detalle de cómo se desplegó: [`DEPLOY.md`](DEPLOY.md).

| Capa | Servicio | URL |
|------|----------|-----|
| Frontend | Cloudflare Pages | https://backned-sis-rrhh.pages.dev/ |
| Backend | Render | https://backned-sis-rrhh-api.onrender.com |
| Base de datos | Neon | [Proyecto `orange-silence-81140454` · rama `br-still-glitter-bclet3gx` · BD `neondb`](https://console.neon.tech/app/projects/orange-silence-81140454/branches/br-still-glitter-bclet3gx?database=neondb) |

**Nota demo:** en plan Free, Render puede tardar ~30–60 s en despertar. Abre el health **antes** de empezar a hablar.

Contraseña demo de todos los usuarios: **`Andina2026`**.

---

## 1. Base de datos (Neon)

**Abrir:** SQL Editor en la rama/BD de la tabla de arriba.

### 1.1 Tablas del esquema (`01_install.sql`)

```sql
-- Listar tablas instaladas (esperado: 42)
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
ORDER BY table_name;

SELECT COUNT(*) AS total_tablas
FROM information_schema.tables
WHERE table_schema = 'public' AND table_type = 'BASE TABLE';
-- esperado: 42
```

| Dominio | Tablas |
|---------|--------|
| Catálogos / params | `area`, `cargo`, `horario_laboral`, `tipo_permiso`, `script_aplicado`, `parametro_sistema`, `parametro_vigencia`, `catalogo_tipo`, `catalogo_valor`, `afp`, `regimen_laboral`, `tramo_renta_quinta` |
| Seguridad / menú | `rol`, `permiso_funcional`, `rol_permiso`, `menu_item`, `menu_rol`, `usuario`, `refresh_token` |
| Personal | `empleado`, `contrato`, `marcacion` |
| Aprobaciones | `configuracion_aprobacion`, `configuracion_aprobacion_detalle`, `solicitud_permiso`, `solicitud_hora_extra`, `solicitud_paso_aprobacion`, `historial_solicitud`, `solicitud_adjunto` |
| Planilla / contab. | `cuenta_contable`, `planilla`, `planilla_detalle`, `asiento_contable`, `asiento_linea` |
| Otros módulos | `auditoria`, `notificacion`, `carga_masiva`, `carga_masiva_detalle`, `reporte_generado`, `evaluacion_desempeno`, `convocatoria`, `postulacion` |

Comprobar tablas clave en un solo query:

```sql
SELECT t AS tabla,
       EXISTS (
         SELECT 1 FROM information_schema.tables
         WHERE table_schema = 'public' AND table_name = t
       ) AS existe
FROM unnest(ARRAY[
  'usuario','empleado','contrato','solicitud_permiso','solicitud_hora_extra',
  'planilla','menu_item','rol','script_aplicado','solicitud_adjunto'
]) AS t;
-- todas las filas: existe = true
```

### 1.2 Usuarios demo

```sql
SELECT COUNT(*) AS usuarios FROM usuario;           -- esperado: 5
SELECT codigo FROM script_aplicado ORDER BY 1;      -- incluir: 06_configuracion_dinamica
SELECT nombre_usuario, rol FROM usuario ORDER BY 1;
```

| Usuario | Rol |
|---------|-----|
| `jesus.mechan` | ADMIN |
| `maria.vargas` | GERENCIA |
| `jesus.pantoja` | JEFE |
| `juan.espinoza` | EMPLEADO |
| `carla.reyes` | RRHH |

**Éxito en vivo:** **42** tablas · 5 usuarios · script `06_configuracion_dinamica` presente.

---

## 2. Backend (Render)

Abrir en el navegador (o mostrar en diapositiva / terminal):

| # | Qué abrir | Esperado |
|---|-----------|----------|
| 1 | https://backned-sis-rrhh-api.onrender.com/actuator/health | `{"status":"UP",...}` |
| 2 | https://backned-sis-rrhh-api.onrender.com/api/configuracion/publica | JSON 200 (empresa Andina, moneda `PEN`, etc.) |
| 3 | https://backned-sis-rrhh-api.onrender.com/swagger-ui.html | UI Swagger carga (opcional) |

**Login API (opcional en demo):**

```bash
curl -sS -X POST "https://backned-sis-rrhh-api.onrender.com/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"jesus.mechan","password":"Andina2026"}'
```

Esperado: JSON con tokens (`accessToken` / refresh) y datos del usuario **ADMIN**.

---

## 3. Frontend (Cloudflare Pages)

1. Abrir https://backned-sis-rrhh.pages.dev/ → pantalla de **login**.
2. Entrar con `jesus.mechan` / `Andina2026` → menú **ADMIN** (dashboard / mantenedores).
3. (Opcional) Refrescar una ruta interna (p. ej. tras navegar) → **no** debe dar 404 (`_redirects`).
4. (Opcional, DevTools → Network) las llamadas van a `backned-sis-rrhh-api.onrender.com`.

**Si el login falla por red:** `VITE_API_BASE_URL` en Pages o `CORS_ORIGINS` en Render (origen = URL del front, sin `/` final).

---

## 4. Checklist E2E (marcar en la presentación)

- [ ] Neon: **42** tablas en `public` (listado / tablas clave `existe = true`)
- [ ] Neon: `usuario` = 5 y `script_aplicado` con `06_configuracion_dinamica`
- [ ] Render: `/actuator/health` → **UP**
- [ ] Render: `/api/configuracion/publica` → 200
- [ ] Pages: login carga
- [ ] Pages: login ADMIN OK (`jesus.mechan`)
- [ ] Flujo BD → API → Front visible en HTTPS

**Orden sugerido en vivo:** Neon (SQL) → health del API → login en Pages.
