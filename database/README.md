# Scripts de base de datos — `rrhh_andina`

PostgreSQL **16+**. Ejecutar desde **pgAdmin** (Query Tool).

## Install limpio (borrar y recrear)

1. Detén el backend (`mvn spring-boot:run`) si está corriendo.
2. En pgAdmin: clic derecho en `rrhh_andina` → **Delete/Drop** (si existe).
3. Query Tool sobre la base **`postgres`** (Auto commit activo) → ejecuta:
   - `00_create_database.sql`
4. Query Tool sobre **`rrhh_andina`** → ejecuta:
   - `01_install.sql` (F5)

Listo.

| # | Archivo | Conexión | Qué hace |
|---|---|---|---|
| 0 | `00_create_database.sql` | `postgres` | Crea la base `rrhh_andina` |
| 1 | `01_install.sql` | `rrhh_andina` | Esquema completo + datos demo |
| 2 | `02_reset.sql` | `rrhh_andina` | **Opcional.** Limpia trámites y deja las cuentas demo |

## Cuentas demo

Contraseña: **`Andina2026`**

- `jesus.mechan` — ADMIN (Gerente General)  
- `maria.vargas` — GERENCIA (Subgerente General)  
- `jesus.pantoja` — JEFE (Jefe de Contabilidad)  
- `juan.espinoza` — EMPLEADO (colaborador Contabilidad)  
- `carla.reyes` — RRHH (Jefe de Recursos Humanos)  

## Notas

- `01_install.sql` hace `DROP SCHEMA public CASCADE` y recrea todo.
- Si solo quieres vaciar trámites sin reinstalar: usa `02_reset.sql`.
- Detalle del modelo: [docs/BASE_DE_DATOS.md](../docs/BASE_DE_DATOS.md).
- Organigrama API: [docs/API.md](../docs/API.md#organigrama).
