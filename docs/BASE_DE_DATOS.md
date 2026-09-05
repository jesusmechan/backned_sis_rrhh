# Base de datos — rrhh_andina

PostgreSQL 16+. Los scripts viven en `database/` y se ejecutan desde **pgAdmin**, no desde `psql` con `\i` o `\c`.

## Orden de ejecución

| # | Archivo | Dónde | Qué hace |
|---|---|---|---|
| 0 | `00_create_database.sql` | Query Tool sobre **postgres** | Crea `rrhh_andina` |
| 1 | `01_schema.sql` | Query Tool sobre **rrhh_andina** | Tipos, tablas, triggers, vistas |
| 2 | `02_seed.sql` | Misma conexión | Catálogos, 17 colaboradores, flujos, casos demo |
| 3 | `03_validar_flujo.sql` | Misma conexión | Pruebas automáticas del circuito (opcional) |
| 4 | `04_refresh_token.sql` | Misma conexión | Tabla `refresh_token` si la base ya existía |

En el Query Tool del `00`, dejar **Auto commit** activo. `CREATE DATABASE` no puede ir dentro de una transacción.

`01_schema.sql` empieza con `DROP SCHEMA public CASCADE`. Se puede repetir sobre `rrhh_andina` sin tocar otras bases. El `03` también es rerunnable: borra solo solicitudes `[VAL-]`.

Si recreas con el `01` actual, **no** necesitas el `04`: la tabla ya está en el esquema.

## Modelo de aprobación

La solicitud **no** guarda al aprobador. El circuito se arma así:

1. `configuracion_aprobacion` — cabecera por tipo de trámite (salud, vacaciones, horas extras, …).
2. `configuracion_aprobacion_detalle` — pasos configurados.
3. `solicitud_paso_aprobacion` — instancia real de cada solicitud.

`empleado.id_jefe_inmediato` es solo organigrama. El trigger lo usa cuando el paso es `JEFE_INMEDIATO`.

```
Empleado registra permiso / hora extra
        │
        ▼
Trigger crea los pasos (el 1 queda EN_CURSO, el resto PENDIENTE)
        │
        ▼
Aprobador atiende su paso (API: /api/pasos/{id}/aprobar|rechazar)
        │
        ├─ Aprobar → el siguiente paso pasa a EN_CURSO
        │            (si era el último, la solicitud queda APROBADO)
        └─ Rechazar → solicitud RECHAZADO y pasos restantes OMITIDO
```

### Tipo de aprobador

| Tipo | Quién atiende |
|---|---|
| `JEFE_INMEDIATO` | Jefe del solicitante (`empleado.id_jefe_inmediato`) |
| `ROL` | Cualquier usuario activo con ese rol (p. ej. RRHH) |
| `USUARIO` | Un usuario concreto (p. ej. Gerencia = `rsalas`) |

### Flujos seed

| Código | Trámite | Pasos |
|---|---|---|
| `CFG-PERMISO-DEFAULT` | Fallback | Jefe |
| `CFG-PERMISO-PARTICULAR` | Particular | Jefe |
| `CFG-PERMISO-SALUD` | Salud | Jefe → RRHH |
| `CFG-PERMISO-VACACIONES` | Vacaciones | Jefe → RRHH → Gerencia (`rsalas`) |
| `CFG-PERMISO-CAPACITACION` | Capacitación | Jefe → RRHH |
| `CFG-PERMISO-COMISION` | Comisión | Jefe → rol APROBADOR |
| `CFG-PERMISO-DUELO` | Duelo | Jefe → RRHH |
| `CFG-HEXTRA` | Horas extras | Jefe → RRHH |

## Tablas principales

**Organización y personal**

- `area`, `cargo`, `horario_laboral`
- `empleado` (código `AND-xxx`, jefe inmediato, estado)
- `carga_masiva`, `carga_masiva_detalle`

**Seguridad**

- `rol`, `permiso_funcional`, `rol_permiso`
- `usuario`
- `refresh_token`

**Trámites**

- `tipo_permiso`
- `configuracion_aprobacion`, `configuracion_aprobacion_detalle`
- `solicitud_permiso`, `solicitud_hora_extra`
- `solicitud_paso_aprobacion`
- `historial_solicitud`

**Operación**

- `marcacion`
- `reporte_generado`
- `auditoria`
- `parametro_sistema`

Hibernate usa `ddl-auto: none`. El esquema lo mantienen solo los scripts SQL.

## Vistas útiles

| Vista | Uso |
|---|---|
| `v_bandeja_aprobacion` | Pasos pendientes de atender |
| `v_pasos_solicitud` | Historial de pasos por solicitud |
| `v_reporte_*` | Insumo de reportes |

## Usuarios de demostración

Contraseña de todos: **Andina2026** (hash `pgcrypto` + Blowfish, compatible con BCrypt de Spring).

| Código | Usuario | Rol | Área |
|---|---|---|---|
| AND-001 | `rsalas` | APROBADOR | Gerencia |
| AND-002 | `mquispe` | RRHH | Recursos Humanos |
| AND-003 | `cmendoza` | APROBADOR | Contabilidad |
| AND-008 | `ediaz` | ADMIN | Administración |
| AND-012 | `csilva` | RRHH | Recursos Humanos |
| AND-014 | `sparedes` | APROBADOR | Tributario |
| AND-004…017 | ver seed | EMPLEADO | varias |

## Prueba `03_validar_flujo.sql`

Genera casos automáticos (`OK` / `FALLO` en Messages) y solicitudes `[VAL-MANUAL]` para recorrer la bandeja a mano.

Para repetir la prueba: vuelve a ejecutar el `03` sobre `rrhh_andina`. No hace falta rerun del `01` ni del `02`.

## Conexión de la API

La aplicación lee `backend/.env`:

```
DB_URL=jdbc:postgresql://127.0.0.1:5432/rrhh_andina
DB_USERNAME=postgres
DB_PASSWORD=root
```

Usar `127.0.0.1` (IPv4). En esta máquina el puerto 5432 de IPv6 puede estar ocupado por Docker (`rrhh_andina_db`, usuario `rrhh_admin`), distinto de PostgreSQL de Windows / pgAdmin.
