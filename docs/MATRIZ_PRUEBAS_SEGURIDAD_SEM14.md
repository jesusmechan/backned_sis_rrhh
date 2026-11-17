# Matriz de pruebas de seguridad — Semana 14 (Sesión 27–28)

**Proyecto:** Sistema de Gestión de RR. HH. — Consultora Contable Andina S.A.C.  
**Marco:** OWASP Top 10:2025 · SAST / DAST / SCA / pentest de API  
**Referencia de clase:** [S14_S27_Pruebas_Seguridad_Software.md](S14_S27_Pruebas_Seguridad_Software.md), [S14_s2_CIS_Taller_Pruebas_Seguridad_Java.md](S14_s2_CIS_Taller_Pruebas_Seguridad_Java.md)  
**Cómo demostrarlo al docente:** [COMO_DEMOSTRAR_SEGURIDAD_SEM14.md](COMO_DEMOSTRAR_SEGURIDAD_SEM14.md)

---

## 1. Funcionalidades críticas evaluadas

| # | Funcionalidad | Activo protegido | Riesgo residual si falla |
|---|---|---|---|
| 1 | Login / JWT / refresh | Credenciales y sesión | Suplantación, fuerza bruta |
| 2 | Consulta de ficha de personal | DNI, correo, datos laborales | IDOR / fuga de PII (Ley 29733) |
| 3 | Planilla y boletas | Remuneraciones | Escalamiento de privilegios |
| 4 | Auditoría / trazabilidad | Evidencia forense | Ocultamiento de incidentes |
| 5 | Dependencias Maven | Cadena de suministro | CVE conocidos (Equifax / Log4Shell) |

---

## 2. Matriz (evidencia de aprendizaje — 5+ pruebas)

| ID | Funcionalidad | Riesgo OWASP Top 10:2025 | Tipo de prueba | Enfoque | Herramienta | Criterio de aceptación (pasa / falla) | Automatización |
|---|---|---|---|---|---|---|---|
| **SEC-01** | Login | **A07** Fallas de autenticación | Fuerza bruta / rate limit | Caja blanca | RateLimitFilter + Postman | Bloquea con **HTTP 429** tras superar `auth-per-minute` | `RateLimitFilterTest#sextoLoginResponde429` |
| **SEC-02** | Login | **A05** Inyección | Inyección SQL en usuario | Caja negra | MockMvc / Burp Repeater | Payload `' OR '1'='1` → **400**, sin token ni SQLException | `SecurityInjectionIT` |
| **SEC-03** | Ficha empleado | **A01** Pérdida de control de acceso | IDOR (cambiar ID) | Caja gris | MockMvc / Burp | EMPLEADO con token propio → ficha ajena **403** | `SecurityAccessControlIT#empleadoNoVeFichaAjena` |
| **SEC-04** | Planilla | **A01** Control de acceso por rol | Autorización | Caja gris | MockMvc / Postman | EMPLEADO → `GET /api/planillas` **403** | `SecurityAccessControlIT#empleadoNoAccedePlanillas` |
| **SEC-05** | API en ejecución | **A02** Configuración incorrecta | Cabeceras HTTP | DAST | OWASP ZAP / IT | `X-Frame-Options: DENY`, `nosniff`, CSP `frame-ancestors 'none'` | `SecurityHeadersIT` |
| **SEC-06** | JWT | **A04** / **A07** | Expiración y tipado de token | Caja blanca | IT | `expiresIn ≤ 900`; refresh no autentica `/api/sesion` | `SecurityAuthHardeningIT` |
| **SEC-07** | `pom.xml` | **A03** Cadena de suministro | SCA | Caja blanca | OWASP Dependency-Check | 0 CVE con CVSS ≥ 7 o plan de mitigación documentado | `mvn org.owasp:dependency-check-maven:check` |
| **SEC-08** | Código Java | **A05** / **A06** | SAST | Caja blanca | SonarQube Community | Quality Gate sin vulnerabilidades *Blocker/Critical* abiertas | `mvn sonar:sonar` (opcional local) |

---

## 3. Controles ya aplicados en el proyecto

| Control | Implementación | OWASP |
|---|---|---|
| Password hashing | BCrypt strength **12** (`SecurityConfig`) | A04 / A07 |
| JWT access 15 min + refresh rotativo | `JwtService` + `AuthService` | A07 |
| Rate limit login/refresh | `RateLimitFilter` (`auth-per-minute`) | A07 |
| Autorización por autoridad | `@PreAuthorize` + `EmpleadoScope` | A01 |
| Prepared / JPA (sin SQL concatenado) | Repositorios Spring Data | A05 |
| Errores sin stack al cliente | `server.error.include-*` + `GlobalExceptionHandler` | A02 / A10 |
| Cabeceras de seguridad | `SecurityFilterChain.headers(...)` | A02 |
| Swagger desactivable | `SWAGGER_ENABLED` / `app.swagger.enabled` | A02 |
| SCA en build | `dependency-check-maven` fail on CVSS ≥ 7 | A03 |
| Auditoría de login | `AuditoriaService` + logs | A09 |

---

## 4. Checklist autenticación (taller clase)

| # | Control | Resultado en RRHH Andina |
|---|---|---|
| 1 | ¿Rechaza contraseñas débiles en **registro/cambio**? | N/A en login público (solo usuarios seed); validación de usuario estricta |
| 2 | ¿Bloquea / retrasa tras varios intentos? | **PASA** — rate limit → 429 |
| 3 | ¿Credenciales solo por canal seguro en prod? | **PASA** (HTTPS en despliegue; HSTS header listo) |
| 4 | ¿2FA para admin? | **PENDIENTE** (mejora futura) |
| 5 | ¿JWT expira (15–30 min)? | **PASA** — 15 min |
| 6 | ¿Passwords con bcrypt? | **PASA** — BCrypt 12 |

---

## 5. Plan DAST (OWASP ZAP) — ambiente local autorizado

```bash
# App levantada en http://localhost:8080 (solo laboratorio propio)
docker run --rm -t zaproxy/zap-stable zap-baseline.py -t http://host.docker.internal:8080/api/configuracion/publica
```

**Hallazgos esperados / aceptados:**

| Severidad | Hallazgo | Tratamiento |
|---|---|---|
| Info/Low | CSRF disabled | Aceptado: API JWT Bearer (sin cookies de sesión) |
| Medium | Swagger expuesto | Mitigado con `SWAGGER_ENABLED=false` en prod |
| — | SQLi en `/api/auth/login` | Debe **no** aparecer (validación + JPA) |

---

## 6. Ejecución

```bash
cd backend
mvn test          # unitarias
mvn verify        # + IT de seguridad (*Security*IT)
mvn -DskipTests org.owasp:dependency-check-maven:check   # SCA
```

Informes:

- Failsafe: `backend/target/failsafe-reports/`
- Dependency-Check: `backend/target/dependency-check-report.html`
- JaCoCo: `backend/target/site/jacoco/index.html`

---

## 7. Criterio de éxito del avance (sesión 28)

- Matriz con ≥ 5 filas trazables a tests o herramientas.
- 0 hallazgos **críticos/altos** abiertos en autenticación e IDOR de personal.
- SCA sin CVSS ≥ 7 sin justificación.
- Informe / presentación con evidencia (esta matriz + PPT S14).
