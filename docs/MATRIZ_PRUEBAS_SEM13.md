# Matriz de casos de prueba — Semana 13 (Testing)

**Proyecto:** Sistema de Gestión de RR. HH. — Consultora Contable Andina S.A.C.  
**Requisito:** RF-AUTH-01 — El usuario inicia sesión con `nombreUsuario` y `password` y recibe tokens JWT si está activo y las credenciales son correctas.  
**Referencia técnica:** `AuthService`, `AuthController`, `LoginRequest` (`backend/`).

## Casos requeridos (sesión 25)

| ID | Nivel | Tipo | Técnica | Datos | Resultado esperado | Automatización |
|---|---|---|---|---|---|---|
| **CP-AUTH-01** | Unitario (servicio) + aceptación (API) | Funcional | Caso válido | `juan.espinoza` / `Andina2026` | Tokens emitidos, auditoría LOGIN | `AuthServiceTest#loginExitoso`, `AuthControllerTest#loginOk` |
| **CP-AUTH-02** | Unitario | Funcional | Partición inválida | Password incorrecta | `401` / `DomainException` UNAUTHORIZED | `AuthServiceTest#loginConPasswordIncorrecta`, `AuthControllerTest#loginUnauthorized` |
| **CP-AUTH-03** | Unitario (DTO) | Validación | **Valor límite** (bajo) | `nombreUsuario` = `ab` (2 chars) | Error de validación HTTP 400 | `LoginRequestValidationTest#usuarioCortoInvalido`, `AuthControllerTest#loginValidacion400` |
| **CP-AUTH-04** | Unitario (DTO) | Validación | **Valor límite** (borde) | Usuario de **60** caracteres válidos | Sin violaciones Bean Validation | `LoginRequestValidationTest#usuarioEnLimiteSuperiorValido` |

## Justificación (principios del testing)

1. **Detección temprana:** Las reglas de login y el patrón del usuario se prueban sin levantar PostgreSQL (puertos mockeados, arquitectura hexagonal).
2. **Independencia:** Cada test prepara sus propios datos (`@BeforeEach`, Mockito).
3. **Trazabilidad:** Cada `CP-*` aparece en `@DisplayName` de las clases `*Test`.
4. **Riesgo del proyecto:** Un fallo en autenticación expone datos de nómina, permisos y asistencia.

## Pruebas complementarias ya existentes

| Área | Clase | Nivel |
|---|---|---|
| Cálculo de planilla Perú 2026 | `PlanillaCalculoPeruDinamicoTest` | Unitario (negocio crítico) |
| Carga / export Excel | `PoiExcelAdapterTest` | Unitario (adaptador salida) |

## Ejecución

```bash
cd backend && mvn test
```

Informe de cobertura (JaCoCo): `backend/target/site/jacoco/index.html`

## Pruebas manuales de sistema

Recorridos con cuentas demo: ver [PRUEBAS.md](PRUEBAS.md) (flujos de permisos, vacaciones, marcación).
