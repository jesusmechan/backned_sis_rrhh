# Cómo demostrar las pruebas de seguridad (Semana 14)

Guía para exponer ante el docente o el equipo.  
Matriz formal: [MATRIZ_PRUEBAS_SEGURIDAD_SEM14.md](MATRIZ_PRUEBAS_SEGURIDAD_SEM14.md) · PPT: [S14_Presentacion_Seguridad_RRHH_Andina.pptx](S14_Presentacion_Seguridad_RRHH_Andina.pptx)

---

## Qué entregar / proyectar

| Orden | Artefacto | Para qué |
|---|---|---|
| 1 | PPT S14 | Narrativa OWASP + controles del proyecto |
| 2 | Matriz SEC-01…SEC-08 | Evidencia de aprendizaje (criterio de clase) |
| 3 | **Dashboard HTML** | Vista visual PASA/FALLA por caso OWASP |
| 4 | Terminal con `BUILD SUCCESS` | Pruebas automatizadas |
| 5 | Swagger o Postman | Ataques controlados (400 / 403 / 429) |

**Guion por cada fila de la matriz:** riesgo OWASP → cómo atacarías → resultado esperado → test o captura que lo prueba.

---

## Vista visual (recomendado para el profesor)

Esta es la forma más clara de **mostrar** las pruebas de seguridad sin quedarte solo en la terminal.

### 1) Dashboard HTML de seguridad (datos reales, automático)

`docs/seguridad-report.html` **no** es una plantilla con PASA/tiempos fijos.  
Cada generación lee los XML **reales** de Maven (`target/surefire-reports` + `target/failsafe-reports`) y escribe:

- Totales y badges **PASA/FALLA** de esa corrida  
- **Tiempo real** de cada método  
- Fecha/hora en el subtítulo  

**Se regenera solo en estos momentos:**

| Cuándo | Qué pasa |
|---|---|
| `mvn test` | Tras Surefire → HTML actualizado |
| `mvn verify` | Tras unitarias + IT de seguridad → HTML actualizado |
| `mvn spring-boot:run` (levantar API) | Al arrancar regenera el HTML con la **última** corrida disponible |
| Manual (opcional) | `mvn -q exec:java -Dexec.mainClass=pe.andina.rrhh.config.SecurityTestReportGenerator` |

Desactivar al arrancar (p. ej. Render): `SECURITY_REPORT_ON_STARTUP=false` en el entorno.

**Flujo demo (PowerShell):**

```powershell
cd D:\backned_sis_rrhh\backend
mvn verify
start ..\docs\seguridad-report.html
```

**Solo suite de seguridad (más rápido en clase):**

```powershell
cd D:\backned_sis_rrhh\backend
mvn "-Dtest=RateLimitFilterTest,SecurityHeadersIT,SecurityInjectionIT,SecurityAccessControlIT,SecurityAuthHardeningIT" "-Dit.test=SecurityHeadersIT,SecurityInjectionIT,SecurityAccessControlIT,SecurityAuthHardeningIT" verify
start ..\docs\seguridad-report.html
```

| Archivo | Uso |
|---|---|
| `docs/seguridad-report.html` | Proyectar en clase (se sobrescribe en cada corrida) |
| `backend/target/seguridad-report.html` | Misma copia junto a reportes Maven |
| `SecurityTestReportGenerator` | Clase Java que arma el HTML |

Si el HTML dice “Sin resultados XML”, aún no hay corrida de tests en `backend/target` (corre `mvn verify` primero).

### 2) Panel del IDE (verde / rojo)

1. Abre p. ej. `backend/src/test/java/pe/andina/rrhh/adapter/in/web/SecurityAccessControlIT.java`
2. Clic en el icono ▶ junto a la clase o a un `@Test`
3. En el panel **Test Results** aparecen los métodos en verde/rojo (muy visual en clase)

### 3) Swagger en vivo

Con la API levantada: [http://127.0.0.1:8080/swagger-ui.html](http://127.0.0.1:8080/swagger-ui.html)  

Ejecutas login SQLi / GET empleados / planillas y el código HTTP (**400** / **403**) se ve al instante.

### 4) Cobertura JaCoCo (opcional)

Tras `mvn verify`: abre `backend/target/site/jacoco/index.html` en el navegador.

### 5) Resumen: qué vista usar en cada momento

| Momento | Vista |
|---|---|
| Exponer resultados globales | Dashboard HTML (`docs/seguridad-report.html`) |
| Explicar un caso paso a paso | IDE ▶ / Debug sobre `Security*IT` |
| Ataque controlado en vivo | Swagger / Postman (Parte B) |
| Cadena de suministro (CVE) | `dependency-check-report.html` |

---

## Requisitos previos

1. PostgreSQL en `127.0.0.1:5432` (las IT usan `rrhh_andina_it`; la demo viva usa `rrhh_andina`).
2. `backend/.env` con `DB_*`, `JWT_SECRET` (≥ 32 caracteres), `RATE_LIMIT_ENABLED=true`.
3. JDK **21** y Maven 3.9+ (si `mvn` no está en PATH: `.tools/apache-maven-3.9.6/bin/mvn.cmd`).

Cuentas demo (contraseña **`Andina2026`**):

| Usuario | Rol | Uso en la demo |
|---|---|---|
| `juan.espinoza` | EMPLEADO | Atacante de caja gris (IDOR / planilla) |
| `jesus.pantoja` | JEFE | Víctima del IDOR (ficha ajena) |
| `carla.reyes` | RRHH | Contraste con permisos altos (opcional) |

---

## Parte A — Evidencia automatizada (recomendado primero)

Desde `backend`:

```bash
cd backend
mvn verify
```

Éxito: `BUILD SUCCESS` al final.

El propio `mvn verify` **ya regenera** el dashboard. Ábrelo:

```powershell
start ..\docs\seguridad-report.html
```

### Solo suite de seguridad (más corto para la exposición)

```bash
mvn "-Dtest=RateLimitFilterTest,SecurityHeadersIT,SecurityInjectionIT,SecurityAccessControlIT,SecurityAuthHardeningIT" "-Dit.test=SecurityHeadersIT,SecurityInjectionIT,SecurityAccessControlIT,SecurityAuthHardeningIT" verify
```

| Clase | IDs que verás en `@DisplayName` | OWASP |
|---|---|---|
| `RateLimitFilterTest` | SEC-A07-03 | A07 fuerza bruta → 429 |
| `SecurityInjectionIT` | SEC-A05-01 … 05 | A05 inyección |
| `SecurityAccessControlIT` | SEC-A01-01 … 06 | A01 IDOR / roles |
| `SecurityHeadersIT` | SEC-A02-01 … 04 | A02 cabeceras |
| `SecurityAuthHardeningIT` | SEC-A07-01, 02, 04 | A07 JWT |

En el IDE: clic derecho sobre la clase `SecurityAccessControlIT` → **Run** / **Debug** para ir caso por caso.

### SCA (cadena de suministro) — si hay tiempo

```bash
mvn -DskipTests org.owasp:dependency-check-maven:check
```

Abrir: `backend/target/dependency-check-report.html`  
Criterio: sin CVE abiertos con CVSS ≥ 7 (o plan documentado).

---

## Parte B — Demo en vivo (API levantada)

```bash
cd backend
mvn spring-boot:run
```

- API: http://127.0.0.1:8080  
- Swagger: http://127.0.0.1:8080/swagger-ui.html  
- Frontend (opcional): `cd frontend && npm run dev` → http://localhost:5173  

### B1 · A05 Inyección SQL (SEC-02)

`POST /api/auth/login` · Content-Type: `application/json`

```json
{
  "nombreUsuario": "admin' OR '1'='1",
  "password": "cualquiera"
}
```

| Criterio | Resultado |
|---|---|
| PASA | **400**, sin `accessToken`, sin mensaje SQL/stack |
| FALLA | 200 con token o error con detalle de PostgreSQL |

### B2 · A01 IDOR ficha de personal (SEC-03)

1. Login como `juan.espinoza` → guarda `accessToken` y `usuario.idEmpleado` (propio).
2. Login como `jesus.pantoja` → anota su `usuario.idEmpleado` (ajeno).
3. Con el **Bearer de Juan**:

```http
GET /api/empleados/{idAjeno}
Authorization: Bearer <token-juan>
```

| Criterio | Resultado |
|---|---|
| PASA | **403** Forbidden |
| FALLA | **200** con DNI/correo de Jesús |

Contraste: `GET /api/empleados/{idPropioDeJuan}` → **200**.

### B3 · A01 Planilla sin permiso (SEC-04)

```http
GET /api/planillas
Authorization: Bearer <token-juan>
```

| Criterio | Resultado |
|---|---|
| PASA | **403** |
| FALLA | **200** con planillas |

### B4 · A02 Cabeceras HTTP (SEC-05)

```http
GET /api/configuracion/publica
```

En Postman → pestaña **Headers** de la respuesta, verificar:

- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `Content-Security-Policy` contiene `frame-ancestors 'none'`
- `Referrer-Policy: strict-origin-when-cross-origin`

### B5 · A07 JWT (SEC-06)

Login válido `juan.espinoza` / `Andina2026`:

- Cuerpo: `"expiresIn": 900` (15 minutos) → **PASA**.
- Copiar el `refreshToken` y usarlo como Bearer en `GET /api/sesion` → debe dar **401** (solo el access token autentica).

### B6 · A07 Rate limit / fuerza bruta (SEC-01)

Con `RATE_LIMIT_ENABLED=true` (por defecto en `.env.example`):

Repetir ~21 veces (si `RATE_LIMIT_AUTH=20`) un login con password incorrecta:

```json
{"nombreUsuario":"juan.espinoza","password":"clave-incorrecta"}
```

| Criterio | Resultado |
|---|---|
| PASA | **429** + cabecera `Retry-After` |
| FALLA | Sigue respondiendo 401 sin límite |

Evidencia automatizada alternativa (sin depender de MockMvc): ejecutar solo `RateLimitFilterTest` en el IDE.

---

## Parte C — Checklist verbal (2 minutos de cierre)

Usar la tabla de la matriz §4 o decir:

1. Rate limit en login → **PASA**  
2. JWT expira 15 min → **PASA**  
3. Passwords BCrypt 12 → **PASA**  
4. IDOR personal bloqueado → **PASA**  
5. SQLi en login rechazado → **PASA**  
6. 2FA admin → **PENDIENTE** (roadmap honesto)

---

## Script sugerido de exposición (~8–10 min)

| Min | Qué haces |
|---|---|
| 0–2 | PPT: qué es probar seguridad + OWASP 2025 mapeado a Andina |
| 2–4 | Abrir matriz; `mvn verify` o Run de `Security*IT` → BUILD SUCCESS |
| 4–7 | Swagger: B1 (SQLi), B2 (IDOR), B4 (headers) |
| 7–9 | Controles en código (`SecurityConfig`, `EmpleadoScope`) + pendientes (2FA) |
| 9–10 | Preguntas |

---

## Problemas frecuentes

| Síntoma | Qué revisar |
|---|---|
| IT fallan / no conecta BD | PostgreSQL arriba; `.env` con usuario que pueda crear `rrhh_andina_it` |
| `mvn` no se reconoce | Usar `.tools/apache-maven-3.9.6/bin/mvn.cmd` y `JAVA_HOME` = JDK 21 |
| No aparece 429 | `RATE_LIMIT_ENABLED=true`; no superar el cupo desde otra IP/proxy; reiniciar API |
| Swagger 401 en prod | Normal si `SWAGGER_ENABLED=false` (control A02) |
| IDOR da 200 | Estás usando token de RRHH/ADMIN (`alcanceTotal`), no de EMPLEADO |

Detalle de instalación / cuentas: [PRUEBAS.md](PRUEBAS.md) · Testing S13: [COMO_COMPROBAR_TESTING.md](COMO_COMPROBAR_TESTING.md).
