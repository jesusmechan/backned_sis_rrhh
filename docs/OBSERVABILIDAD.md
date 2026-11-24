# Observabilidad — Prometheus + Grafana

Guía para ver indicadores del API **rrhh-api** (Spring Boot + Micrometer + métricas de negocio).

| Componente | Rol | URL local |
|------------|-----|-----------|
| API | Expone métricas | http://localhost:8080/actuator/prometheus |
| Health | Estado del servicio | http://localhost:8080/actuator/health |
| Prometheus | Recolecta y almacena series | http://localhost:9090 |
| Grafana | Dashboards | http://localhost:3000 (`admin` / `admin`) |

En producción (Render) la API sigue exponiendo `/actuator/health` y `/actuator/prometheus`; Prometheus/Grafana se ejecutan aparte (local para demos del curso, o un host/servicio gestionado).

---

## 1. Arranque (local)

### 1.1 API

```bash
cd backend
mvn spring-boot:run
```

Comprueba:

- http://localhost:8080/actuator/health → `{"status":"UP",...}`
- http://localhost:8080/actuator/prometheus → texto con `http_server_requests_...`, `jvm_...`, `rrhh_...`

### 1.2 Prometheus + Grafana

Desde la raíz del monorepo (Docker Desktop encendido):

```bash
docker compose -f docker-compose.observability.yml up -d
```

Detener:

```bash
docker compose -f docker-compose.observability.yml down
```

---

## 2. Verificar que Prometheus scrapea bien

1. Abre http://localhost:9090/targets  
2. El job **`rrhh-api`** debe estar **UP** (verde).  
3. Si está **DOWN**:
   - la API debe estar en el puerto **8080**
   - en Windows/macOS usa `host.docker.internal` (ya está en `observability/prometheus.yml`)
   - prueba: `curl http://127.0.0.1:8080/actuator/prometheus`

Consulta en Prometheus → **Graph**:

```promql
up{job="rrhh-api"}
```

Debe devolver `1`.

---

## 3. Dashboards en Grafana

1. http://localhost:3000 → **`admin` / `admin`**  
2. **Dashboards** → carpeta **RRHH Andina**

| Dashboard | Contenido |
|-----------|-----------|
| **RRHH API — Overview** | Infra: scrape, RPS, latencia, 5xx, JVM, Tomcat, códigos HTTP |
| **RRHH API — Negocio** | Dominio: logins, solicitudes, aprobaciones, asistencia, planilla, rate limit |

Genera tráfico (login, permisos, marcación, planilla) y espera 15–30 s.

### Paneles Overview

| Panel | Qué indica |
|-------|------------|
| Targets UP | Si Prometheus llega a la API (`1` = OK) |
| RPS HTTP | Peticiones por segundo |
| Latencia p95 | Tiempo de respuesta al percentil 95 |
| Errores 5xx / s | Fallos de servidor |
| Throughput por URI | Endpoints más usados |
| Latencia p50/p95/p99 | Distribución de tiempos |
| Memoria JVM (heap) | Uso vs máximo de heap |
| Hilos JVM / Tomcat | Carga de hilos |
| Requests por código HTTP | 200 / 401 / 403 / 500, etc. |

### Paneles Negocio

| Panel | Qué indica |
|-------|------------|
| Logins OK / fallidos | Autenticación exitosa vs rechazada |
| Solicitudes creadas | Permisos y horas extras nuevas |
| Decisiones de aprobación | APROBAR / RECHAZAR / DEVOLVER / COMPLETAR |
| Marcaciones | Ingreso/salida por origen (WEB, MOVIL, MANUAL) |
| Planilla crear/calcular/cerrar | Ciclo de planilla |
| Duración cálculo planilla | p50 / p95 del timer de cálculo |
| Rate limit 429 | Rechazos por cupo (api / auth) |

---

## 4. Métricas de negocio (código)

Puerto: `MetricsPort` · Adaptador: `MicrometerMetricsAdapter`.

| Métrica Prometheus | Origen | Tags |
|--------------------|--------|------|
| `rrhh_auth_login_total` | `AuthService.login` | `result`, `reason` |
| `rrhh_solicitud_creada_total` | crear permiso / HE | `tipo` |
| `rrhh_aprobacion_decision_total` | `SolicitudService.decidir` | `decision`, `origen` |
| `rrhh_asistencia_marcacion_total` | `AsistenciaService.marcar` | `tipo`, `origen` |
| `rrhh_planilla_accion_total` | crear / calcular / cerrar | `accion` |
| `rrhh_planilla_calculo_seconds_*` | timer de cálculo | histograma |
| `rrhh_rate_limit_rejected_total` | `RateLimitFilter` | `scope` |

Motivos de login (`reason`): `ok`, `bad_credentials`, `inactive`, `unknown_user`.

---

## 5. Consultas PromQL (Explore)

| Indicador | PromQL |
|-----------|--------|
| ¿API viva? | `up{job="rrhh-api"}` |
| RPS total | `sum(rate(http_server_requests_seconds_count{application="rrhh-api"}[1m]))` |
| Latencia p95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="rrhh-api"}[5m])) by (le))` |
| Errores 5xx | `sum(rate(http_server_requests_seconds_count{application="rrhh-api",status=~"5.."}[1m]))` |
| Heap usado | `sum(jvm_memory_used_bytes{application="rrhh-api",area="heap"})` |
| Logins fallidos | `sum(rate(rrhh_auth_login_total{result="failure"}[5m]))` |
| Aprobaciones | `sum by (decision) (rate(rrhh_aprobacion_decision_total[5m]))` |
| Cálculo planilla p95 | `histogram_quantile(0.95, sum(rate(rrhh_planilla_calculo_seconds_bucket[5m])) by (le))` |
| Rate limit | `sum by (scope) (rate(rrhh_rate_limit_rejected_total[5m]))` |

---

## 6. Endpoints Actuator

| Path | Público | Uso |
|------|---------|-----|
| `/actuator/health` | Sí | Health check (Render) |
| `/actuator/prometheus` | Sí | Scrape Prometheus |
| `/actuator/metrics` | JWT | Listado JSON Micrometer |
| `/actuator/info` | JWT | Nombre y descripción de la app |

```http
GET /actuator/metrics/rrhh.auth.login
Authorization: Bearer <accessToken>
```

---

## 7. Producción (Render)

1. Tras el deploy:
   - `https://<servicio>.onrender.com/actuator/health`
   - `https://<servicio>.onrender.com/actuator/prometheus`
2. Para scrapear prod desde tu PC, usa el ejemplo:

```bash
# Revisa y ajusta el host en:
# observability/prometheus-prod.example.yml
cp observability/prometheus-prod.example.yml observability/prometheus.yml
docker compose -f docker-compose.observability.yml up -d --force-recreate prometheus
```

**Notas:** el free tier de Render puede dormir (target DOWN hasta el wake-up). En un entorno real conviene restringir `/actuator/prometheus` (red privada, Basic Auth o secret).

---

## 8. Archivos del repo

```
docker-compose.observability.yml
observability/
  prometheus.yml
  prometheus-prod.example.yml
  grafana/provisioning/
    datasources/prometheus.yml
    dashboards/dashboards.yml
    dashboards/json/rrhh-api-overview.json
    dashboards/json/rrhh-api-negocio.json
backend/
  .../MetricsPort.java
  .../MicrometerMetricsAdapter.java
  AuthService, SolicitudService, PlanillaService, AsistenciaService, RateLimitFilter
  application.yml · SecurityConfig · JwtAuthFilter
render.yaml   # healthCheckPath: /actuator/health
```
