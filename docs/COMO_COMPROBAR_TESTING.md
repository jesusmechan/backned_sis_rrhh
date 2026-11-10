# Cómo comprobar el testing

## Opción rápida (terminal)

```bash
cd backend
mvn test
```

Debes ver al final:

```text
Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Cobertura JaCoCo

Después de `mvn test`, abre en el navegador:

`backend/target/site/jacoco/index.html`

Para las clases del avance:

- `AuthService` ≈ **69%**
- `PlanillaCalculoPeruDinamico` ≈ **92%**

## Desde el IDE (IntelliJ / Cursor)

1. Carpeta `backend/src/test/java`
2. Clic derecho → **Run Tests** / **Run 'All Tests'**
3. Verifica iconos verdes en:
   - `AuthServiceTest`
   - `LoginRequestValidationTest`
   - `AuthControllerTest`
   - `PlanillaCalculoPeruDinamicoTest`
   - `PoiExcelAdapterTest`

## Qué prueba cada clase

| Clase | Casos |
|---|---|
| `AuthServiceTest` | CP-AUTH-01 / CP-AUTH-02 (login válido e inválido) |
| `LoginRequestValidationTest` | CP-AUTH-03 / CP-AUTH-04 (límites del usuario) |
| `AuthControllerTest` | HTTP 200 / 400 / 401 |
| `PlanillaCalculoPeruDinamicoTest` | Cálculo legal planilla Perú |
| `PoiExcelAdapterTest` | Lectura/escritura Excel |

Matriz formal: [MATRIZ_PRUEBAS_SEM13.md](MATRIZ_PRUEBAS_SEM13.md)
