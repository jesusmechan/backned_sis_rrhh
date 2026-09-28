# Diseño de base de datos — rrhh_andina

Semanas 6 del temario: *mejores prácticas, errores comunes, consideraciones de seguridad y taller de diseño lógico y físico*.

| Entregable | Dónde |
|---|---|
| Diseño lógico (ERD por módulo) | FigJam [Avance 2 - Diseño de BD](https://www.figma.com/board/FsVw4RskUFFxzjJBByH5C9) (4 diagramas) |
| Diseño físico (diagrama con tipos PostgreSQL, PK, FK, UK y CHECK) | FigJam [Avance 2 - Diseño físico BD, notación BPMN y mapa de reportes](https://www.figma.com/board/ALb7EmLv7XOEh82juQEAxb), secciones BD F1 a BD F4 |
| Diseño físico (DDL ejecutable) | [`database/01_install.sql`](../../database/01_install.sql) |
| Diccionario de datos | [DICCIONARIO_DATOS.md](DICCIONARIO_DATOS.md) (generado del DDL) |
| Seguridad de la base | [03_seguridad_bd.sql](03_seguridad_bd.sql) |
| Errores comunes demostrados sobre la base | [04_ejemplos_errores_comunes.sql](04_ejemplos_errores_comunes.sql) (termina en `ROLLBACK`) |
| Índices propuestos para FK | [05_indices_fk_propuestos.sql](05_indices_fk_propuestos.sql) |
| MER en Markdown (Mermaid) | [`docs/MER.md`](../../docs/MER.md) |

---

## 1. Del modelo conceptual al físico

| Nivel | Pregunta que responde | Resultado en el proyecto |
|---|---|---|
| Conceptual | ¿Qué cosas le importan al negocio? | Empleado, contrato, solicitud, aprobación, planilla, asiento, convocatoria |
| Lógico | ¿Qué entidades, atributos, claves y cardinalidades hay? | 34 entidades en 7 módulos, relaciones 1:N, N:M con tablas puente, autorrelación de jefe |
| Físico | ¿Cómo se implementa en PostgreSQL? | Tipos `ENUM`, `IDENTITY`, `NUMERIC(12,2)`, `TIMESTAMPTZ`, índices parciales, triggers, vistas |

### Módulos del diseño lógico

| Módulo | Entidades | Diagrama FigJam |
|---|---|---|
| Organización, personal y seguridad | area, cargo, horario_laboral, empleado, contrato, marcacion, usuario, rol, permiso_funcional, rol_permiso, menu_item, menu_rol, refresh_token | BD 01 |
| Flujos de aprobación y trámites | tipo_permiso, configuracion_aprobacion(_detalle), solicitud_permiso, solicitud_hora_extra, solicitud_paso_aprobacion, historial_solicitud, notificacion | BD 02 |
| Planilla, contabilidad, desempeño y reclutamiento | planilla, planilla_detalle, asiento_contable, asiento_linea, evaluacion_desempeno, convocatoria, postulacion | BD 03 |
| Operación y control | auditoria, carga_masiva(_detalle), reporte_generado, parametro_sistema, cuenta_contable | BD 04 |

### Decisión de diseño central: el aprobador no vive en la solicitud

Un diseño ingenuo pondría `id_aprobador` en `solicitud_permiso`. Eso obliga a cambiar código cada vez que el circuito cambia (vacaciones necesita tres firmas, salud dos). En su lugar:

```
configuracion_aprobacion (1) ──< configuracion_aprobacion_detalle (pasos configurados)
                                         │ se instancia por trigger
solicitud_permiso / solicitud_hora_extra (1) ──< solicitud_paso_aprobacion (pasos reales)
```

Agregar un paso de GERENCIA a un trámite es insertar una fila, no desplegar una nueva versión.

---

## 2. Mejores prácticas aplicadas (con ejemplo del esquema)

| # | Práctica | Cómo se aplicó |
|---|---|---|
| 1 | Nombres consistentes | `snake_case`, singular, PK `id_<tabla>`, restricciones con prefijo `uq_`, `ck_`, `ix_` (p. ej. `uq_empleado_documento`, `ck_contrato_fechas`) |
| 2 | Claves sustitutas + claves naturales únicas | `id_empleado` es la PK; `codigo_empleado`, `(tipo_documento, numero_documento)` y `correo_institucional` son `UNIQUE` |
| 3 | Tipos de datos exactos | Dinero en `NUMERIC(12,2)` (nunca `FLOAT`); horas en `NUMERIC(5,2)`; instantes en `TIMESTAMPTZ` |
| 4 | Dominios cerrados con `ENUM` | `estado_solicitud`, `estado_planilla`, `tipo_aprobador`… el motor rechaza valores inválidos |
| 5 | Reglas de negocio en `CHECK` | `ck_empleado_dni` (8 dígitos), `ck_horario_rango` (salida > ingreso), `ck_eval_puntajes` (1–5), `ck_contrato_afp` (AFP exige nombre) |
| 6 | Integridad referencial con acción explícita | `ON DELETE CASCADE` solo en hijos dependientes (`planilla_detalle`, `asiento_linea`); `ON DELETE SET NULL` en `historial_solicitud.id_paso_solicitud` para no perder historia |
| 7 | Índices parciales para reglas "a lo sumo uno" | `uq_contrato_empleado_vigente` (un contrato `VIGENTE` por empleado); `uq_paso_permiso_en_curso` (un solo paso `EN_CURSO` por solicitud) |
| 8 | Arcos exclusivos (XOR) controlados | `ck_paso_una_solicitud`: el paso pertenece a un permiso **o** a una hora extra, nunca a ambos |
| 9 | Columnas calculadas persistidas | `marcacion.fecha` = `fecha_hora` en hora de Lima (`GENERATED ALWAYS … STORED`), indexable para reportes diarios |
| 10 | Auditoría uniforme | `fecha_creacion` / `fecha_actualizacion` en todas las tablas maestras; trigger `fn_set_fecha_actualizacion` |
| 11 | Parametrizar lo que cambia por ley | Tasas ONP 13 %, EsSalud 9 %, AFP, RMV, UIT y topes de horas extras en `parametro_sistema`, no en código |
| 12 | Vistas para lectura | `v_reporte_*`, `v_bandeja_aprobacion`, `v_trazabilidad_solicitudes` aíslan a reportes de los cambios de tablas |
| 13 | Esquema versionado como código | `ddl-auto: none` en Hibernate; el único origen del esquema es `database/*.sql` en Git |

### Normalización

- **1FN**: sin grupos repetidos. Los pasos de un circuito no son columnas `paso1`, `paso2`, `paso3`, sino filas en `configuracion_aprobacion_detalle`.
- **2FN**: en tablas puente con PK compuesta (`rol_permiso`, `menu_rol`) no hay atributos que dependan solo de parte de la clave.
- **3FN**: el nombre del área no se repite en `empleado`; se referencia `id_area`.
- **Desnormalización deliberada**: `planilla_detalle` copia `remuneracion_basica`, `modalidad` y `regimen_pensionario` del contrato, y `asiento_linea` copia `nombre_cuenta`. Es una **fotografía histórica**: si mañana el contrato sube de sueldo, la boleta de septiembre no debe cambiar.

---

## 3. Errores comunes y cómo se evitaron

| Error común | Consecuencia | En este diseño |
|---|---|---|
| Guardar dinero en `FLOAT`/`REAL` | Redondeos: 0.1 + 0.2 ≠ 0.3 en boletas | `NUMERIC(12,2)` en todos los montos |
| Usar `TIMESTAMP` sin zona | Marcaciones corridas 5 h si el servidor está en UTC | `TIMESTAMPTZ` + `fecha` calculada en `America/Lima` |
| Estados como texto libre | "Aprobado", "APROBADO", "aprobada" conviven | Tipos `ENUM` |
| Validar solo en el frontend | Una llamada directa a la API salta la regla | `CHECK`, `UNIQUE` y triggers en la base + validación en `domain/service` |
| FK sin índice | Joins y borrados lentos al crecer | Parcial: las tablas de trámites tienen `ix_permiso_empleado`, `ix_paso_asignado`, etc., pero la revisión del ejemplo 11 encontró 27 FK sin índice. [05_indices_fk_propuestos.sql](05_indices_fk_propuestos.sql) cubre las 10 de tablas que crecen (planilla, asiento, historial, postulación); las 17 restantes apuntan a catálogos pequeños |
| Aprobador fijo en la solicitud | Cambiar el circuito exige código | Circuito configurable (sección 1) |
| Borrado físico de historia | Se pierde la trazabilidad exigida por auditoría | `historial_solicitud` y `auditoria` de solo inserción (ver seguridad) |
| Contraseñas en texto plano | Filtración total si se roba la base | `usuario.password_hash` con BCrypt |
| Tabla "Dios" con todo | Nulos por todas partes | Separación permiso / hora extra / paso / historial |
| Duplicar periodo de planilla | Doble pago | `uq_planilla_periodo (anio, mes)` |
| Autorreferencia sin control | Un empleado jefe de sí mismo | `ck_empleado_no_autojefe` |

### Demostración ejecutable

[04_ejemplos_errores_comunes.sql](04_ejemplos_errores_comunes.sql) reproduce los errores sobre la base real y termina en `ROLLBACK`. Resultado obtenido en la base de desarrollo:

| # | Error | Resultado |
|---|---|---|
| 1 | Dinero en `REAL` | Sumar mil veces 0.10 da **99.99905** en `REAL` y **100.00** en `NUMERIC` |
| 2 | Fecha sin zona | Salida a las 20:30 de Lima del 01/09 queda como **02/09** en un servidor UTC; con `America/Lima`, 01/09 |
| 3 | Estado en texto libre | "APROBADO", "Aprobado", "aprobada" y "APROBADO " cuentan como 4 estados; el `ENUM` rechaza `aprobada` (22P02) |
| 4 | Validar solo en el frontend | DNI de 4 dígitos rechazado por `ck_empleado_dni` (23514) |
| 5 | Autorreferencia | `ck_empleado_no_autojefe` (23514) |
| 6 | Planilla duplicada | `uq_planilla_periodo` (23505) |
| 7 | Dos contratos vigentes | Índice único parcial `uq_contrato_empleado_vigente` (23505) |
| 8 | Arco exclusivo | Paso sin solicitud rechazado por `ck_paso_una_solicitud` (23514) |
| 9 | Inyección SQL | Con la entrada `x' OR '1'='1`, concatenar devuelve los 5 usuarios; con parámetro, 0 |
| 10 | Contraseñas en texto plano | 0 de 5 usuarios sin hash BCrypt |
| 11 | FK sin índice | 27 FK sin índice (ver la tabla anterior) |

---

## 4. Consideraciones de seguridad

### Implementadas en el proyecto

- **Autenticación**: contraseñas con BCrypt; JWT de acceso de 15 min y `refresh_token` revocable de 7 días.
- **Autorización por rol**: `rol` → `permiso_funcional` (N:M) y `menu_rol` para el menú; Spring Security valida en cada endpoint.
- **Mínima exposición de datos**: `EmpleadoScope` limita sobre quién actúa cada usuario (el empleado solo sobre sí mismo, el jefe sobre su equipo directo; ADMIN y RRHH sobre todos).
- **Inyección SQL**: todo acceso pasa por JPA/consultas parametrizadas; no se concatena SQL con entradas del usuario.
- **Trazabilidad**: `auditoria` (acción, entidad, detalle `JSONB`, IP) e `historial_solicitud`.
- **Validación de decisión en la base**: `fn_validar_decision_paso` impide que alguien apruebe un paso que no le corresponde, aunque llame a la API directamente.
- **Secretos fuera del repositorio**: `backend/.env` está en `.gitignore`; solo se versiona `.env.example`.
- **Protección de la API**: límite de peticiones (rate limit) e idempotencia en operaciones de escritura.

### Propuestas en este avance ([03_seguridad_bd.sql](03_seguridad_bd.sql))

| Control | Detalle |
|---|---|
| Principio de mínimo privilegio | La API deja de usar `postgres`: rol `rrhh_app` solo con DML, sin DDL |
| Separación de funciones | `rrhh_owner` (dueño, sin login), `rrhh_app` (API), `rrhh_reportes` (solo vistas, sesión de solo lectura) |
| Bitácoras inmutables | `REVOKE UPDATE, DELETE` sobre `auditoria`, `historial_solicitud` y `reporte_generado` |
| Datos sensibles | `rrhh_reportes` no ve `usuario.password_hash` ni `refresh_token` |
| Disponibilidad | `statement_timeout`, `idle_in_transaction_session_timeout` y `CONNECTION LIMIT` por rol |
| Red | `pg_hba.conf` con `scram-sha-256` y solo IPs conocidas; SSL si API y BD están separadas |
| Continuidad | `pg_dump -Fc` diario y prueba mensual de restauración |

---

## 5. Diseño físico

### Objetos del esquema

| Tipo | Cantidad | Ejemplos |
|---|---|---|
| Tablas | 34 | ver [diccionario](DICCIONARIO_DATOS.md) |
| Tipos `ENUM` | 25 | `estado_solicitud`, `regimen_pensionario` |
| Índices explícitos | 34 (8 únicos parciales) | `ix_marcacion_empleado_fecha`, `uq_config_activa_especifica` (parcial) |
| Funciones PL/pgSQL | 12 (con sus triggers) | `fn_instanciar_pasos_aprobacion`, `fn_avanzar_flujo_aprobacion`, `fn_validar_hora_extra` |
| Vistas | 9 | `v_bandeja_aprobacion`, `v_reporte_permisos`, `v_trazabilidad_solicitudes` |

### Lógica en triggers (por qué en la base)

| Trigger | Evento | Qué garantiza |
|---|---|---|
| `trg_permiso_configuracion` / `trg_hora_extra_configuracion` | BEFORE INSERT | Asigna el circuito correcto según el tipo de trámite |
| `trg_permiso_instanciar_flujo` | AFTER INSERT | Crea los pasos; el 1 queda `EN_CURSO`, el resto `PENDIENTE` |
| `trg_validar_decision_paso` | BEFORE UPDATE | Solo el aprobador asignado (jefe, rol o usuario) decide |
| `trg_avanzar_flujo_aprobacion` | AFTER UPDATE | Aprobar activa el siguiente paso o cierra `APROBADO`; rechazar omite el resto |
| `trg_validar_hora_extra` | BEFORE INSERT/UPDATE | Topes diario (4 h) y semanal (12 h) leídos de `parametro_sistema` |
| `trg_*_empleado_activo` | BEFORE INSERT | No se marcan asistencias ni trámites de empleados cesados |

Estas reglas se repiten en la base porque son invariantes del negocio: si mañana se conecta otra aplicación (por ejemplo, un app móvil), no podrá saltarlas.

### Estimación de volumen (MYPE, 5 años)

| Tabla | Crecimiento | Volumen estimado |
|---|---|---|
| `marcacion` | 2 por empleado por día hábil | 30 empleados × 2 × 250 × 5 ≈ 75 000 filas |
| `solicitud_permiso` + pasos | ~3 por empleado al mes, 2–3 pasos c/u | ≈ 5 400 solicitudes y 15 000 pasos |
| `planilla_detalle` | 1 por empleado al mes | ≈ 1 800 filas |
| `auditoria` | Cada operación de escritura | ≈ 200 000 filas → índice por fecha y por entidad |

El volumen es pequeño; los índices están pensados para las consultas frecuentes (bandeja, reporte por rango de fechas) y no por tamaño.

---

## 6. Cómo reconstruir la base

1. `database/00_create_database.sql` sobre `postgres` (Auto commit activo).
2. `database/01_install.sql` sobre `rrhh_andina`.
3. (Opcional) `database/02_reset.sql` para dejar datos de demostración limpios.
4. (Opcional, recomendado en producción) `avance2-integrador1/02-base-de-datos/03_seguridad_bd.sql`.
5. (Opcional) `05_indices_fk_propuestos.sql`. El script `04_ejemplos_errores_comunes.sql` se puede correr en cualquier momento: no deja cambios.
6. Regenerar el diccionario si cambió el DDL: `node avance2-integrador1/02-base-de-datos/generar-diccionario.mjs`.
