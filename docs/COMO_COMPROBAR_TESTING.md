# Cómo validar todas las pruebas (unitarias + integración)

Guía para comprobar que el backend pasa la suite completa.  
Matriz formal de casos: [MATRIZ_PRUEBAS_SEM13.md](MATRIZ_PRUEBAS_SEM13.md).

---

## 1. Qué hay que validar

| Nivel | Clases | ¿Necesita PostgreSQL? | Comando |
|---|---|---|---|
| **Unitarias** (`*Test`) | `AuthServiceTest`, `LoginRequestValidationTest`, `AuthControllerTest`, `PlanillaCalculoPeruDinamicoTest`, `PoiExcelAdapterTest` | No (mocks) | `mvn test` |
| **Integración** (`*IT`) | `AuthIntegrationIT`, `SesionIntegrationIT` | Sí → BD **`rrhh_andina_it`** | `mvn verify` |

**Importante:** las IT **no** usan `rrhh_andina` (tu BD de desarrollo/demo).  
Usan la BD de prueba `rrhh_andina_it`. Al arrancar, el test la crea si falta y carga `database/01_install.sql` (incluye `DROP SCHEMA` solo sobre esa BD).

---

## 2. Requisitos previos

### Unitarias
- JDK 21
- Maven 3.9+
- Estar en la carpeta `backend`

### Integración (además de lo anterior)
1. PostgreSQL escuchando en `127.0.0.1:5432`
2. Credenciales válidas en `backend/.env`:
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `JWT_SECRET` (mín. 32 caracteres)
3. El usuario de Postgres debe poder **crear bases** (la primera vez crea `rrhh_andina_it`)

No hace falta crear `rrhh_andina_it` a mano: lo hace `ItDatabaseInitializer`.

---

## 3. Validación recomendada (todo junto)

Desde la raíz del backend:

```bash
cd backend
mvn verify
```

Eso ejecuta:

1. **Surefire** → pruebas unitarias (`*Test`)
2. **Failsafe** → pruebas de integración (`*IT`) contra `rrhh_andina_it`

### Criterio de éxito

Al final debe aparecer `BUILD SUCCESS` y, en los reportes:

| Suite | Ubicación del resumen | Esperado |
|---|---|---|
| Unitarias | salida de Surefire / `target/surefire-reports` | Failures: **0**, Errors: **0** |
| Integración | `target/failsafe-reports/failsafe-summary.xml` | `<completed>9</completed>`, errors/failures **0** |

Las integración actuales son **9** casos (`CP-AUTH-IT-01` … `CP-AUTH-IT-09`).

---

## 4. Validar por separado

### Solo unitarias (rápido, sin BD)

```bash
cd backend
mvn test
```

Éxito típico:

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(El número exacto puede variar si se agregan más `*Test`; lo importante es **0 fallos**.)

### Solo integración (usa `rrhh_andina_it`)

```bash
cd backend
mvn failsafe:integration-test failsafe:verify
```

Éxito:

```text
BUILD SUCCESS
```

Y en `target/failsafe-reports/failsafe-summary.xml`:

```xml
<completed>9</completed>
<errors>0</errors>
<failures>0</failures>
```

---

## 5. Cobertura JaCoCo (opcional)

Después de `mvn test` o `mvn verify`:

Abrir en el navegador:

`backend/target/site/jacoco/index.html`

Referencia del avance:

- `AuthService` ≈ **69%**
- `PlanillaCalculoPeruDinamico` ≈ **92%**

---

## 6. Desde el IDE (IntelliJ / Cursor)

### Unitarias
1. Abrir `backend/src/test/java`
2. Ejecutar cada clase `*Test` o “Run All Tests” de unitarias
3. Iconos verdes en:
   - `application/service/AuthServiceTest`
   - `application/dto/LoginRequestValidationTest`
   - `adapter/in/web/AuthControllerTest`
   - `application/service/PlanillaCalculoPeruDinamicoTest`
   - `adapter/out/excel/PoiExcelAdapterTest`

### Integración
1. Confirmar que PostgreSQL está arriba y que existe `backend/.env`
2. Ejecutar las clases `*IT`:
   - `adapter/in/web/AuthIntegrationIT`
   - `adapter/in/web/SesionIntegrationIT`
3. La primera ejecución puede tardar más: crea/carga `rrhh_andina_it`

---

## 7. Debuggear cada prueba (útil para el video)

**Sí se pueden debuggear**, una por una, con breakpoints. Es la forma más clara de mostrar en video qué hace cada caso.

### En Cursor / VS Code
1. Instala la extensión **Extension Pack for Java** (o al menos Language Support for Java + Debugger).
2. Abre la clase de test (ej. `AuthServiceTest.java` o `AuthIntegrationIT.java`).
3. Pon un **breakpoint** en la línea del `@Test` que quieras demostrar (o dentro del método de negocio: `AuthService.login`, etc.).
   - Clic en el margen izquierdo del número de línea (punto rojo).
4. Sobre el método de prueba (o el icono ▶ del método):
   - clic derecho → **Debug Test** / **Debug Java Test**
   - no uses solo “Run”: Run ejecuta sin detenerse en breakpoints.
5. Cuando pare en el breakpoint:
   - mira **Variables** (request, tokens, status HTTP)
   - usa **Step Over (F10)** / **Step Into (F11)** para avanzar línea a línea
6. Para el video: un método `@Test` = una escena. Narras el `@DisplayName` (ej. `CP-AUTH-01`) y muestras el resultado verde al final.

### En IntelliJ IDEA
1. Abre la clase `*Test` o `*IT`.
2. Breakpoint en el margen.
3. Clic en el icono verde del método → **Debug 'nombreDelMetodo()'**.
4. Misma idea: Step Over / Step Into y panel Variables.

### Orden sugerido para grabar

| Orden | Clase | Por qué |
|---|---|---|
| 1 | `AuthServiceTest` | Unitaria pura (rápida, sin BD) |
| 2 | `LoginRequestValidationTest` | Límites del DTO |
| 3 | `AuthControllerTest` | HTTP con mocks |
| 4 | `PlanillaCalculoPeruDinamicoTest` / `PoiExcelAdapterTest` | Complementarias |
| 5 | `AuthIntegrationIT` | Misma lógica, pero con Spring + `rrhh_andina_it` |
| 6 | `SesionIntegrationIT` | JWT real y recurso protegido |

### Tips para que el video se entienda
- Ejecuta **un solo método** por toma (no toda la clase de golpe).
- En integración, la **primera** depuración puede tardar ~20–40 s (levanta Spring y prepara `rrhh_andina_it`); las siguientes del mismo proceso son más rápidas.
- Antes de grabar IT: confirma Postgres y `backend/.env`.
- El `@DisplayName` de cada test es el texto que conviene leer en voz alta (trazabilidad con la matriz).

### ¿`mvn test` / `mvn verify` se debuggean?
Sirven para demostrar el **resultado global** (BUILD SUCCESS), pero **no** para ir paso a paso. Para el recorrido línea a línea usa siempre **Debug Test** del IDE.

---

## 8. Checklist de aceptación (marcar al validar)

- [ ] `mvn test` → BUILD SUCCESS (unitarias sin fallos)
- [ ] PostgreSQL en `127.0.0.1:5432` y `.env` OK
- [ ] `mvn verify` → BUILD SUCCESS
- [ ] `failsafe-summary.xml` → 9 completed, 0 errors, 0 failures
- [ ] (Opcional) JaCoCo abierto y clases clave con cobertura razonable
- [ ] Confirmado que la BD de demo `rrhh_andina` **sigue intacta** (las IT no la usan)

---

## 9. Mapa rápido: qué prueba cada clase

| Clase | Nivel | Qué valida |
|---|---|---|
| `AuthServiceTest` | Unitaria | CP-AUTH-01 / CP-AUTH-02 (login ok / password mala) |
| `LoginRequestValidationTest` | Unitaria | CP-AUTH-03 / CP-AUTH-04 (límites del usuario) |
| `AuthControllerTest` | Unitaria | HTTP 200 / 400 / 401 con MockMvc + mocks |
| `PlanillaCalculoPeruDinamicoTest` | Unitaria | Cálculo legal planilla Perú |
| `PoiExcelAdapterTest` | Unitaria | Lectura/escritura Excel |
| `AuthIntegrationIT` | Integración | Login, validación, refresh/rotación, logout **con BD real** |
| `SesionIntegrationIT` | Integración | JWT + `/api/sesion` y `/api/configuracion/publica` |

---

## 10. Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| Error al conectar / crear BD | Postgres apagado o mal puerto | Arrancar PostgreSQL en `5432` |
| `password authentication failed` | Credenciales distintas al `.env` | Revisar `DB_USERNAME` / `DB_PASSWORD` |
| No encuentra `01_install.sql` | Comando ejecutado fuera de `backend` | `cd backend` y volver a correr |
| IT fallan pero unitarias pasan | BD de prueba no lista o seed incompleto | Borrar `rrhh_andina_it` en pgAdmin y reejecutar `mvn verify` (se recrea sola) |
| Dudas sobre demos manuales | Eso no es automatizado | Ver [PRUEBAS.md](PRUEBAS.md) (flujos con cuentas demo en `rrhh_andina`) |
