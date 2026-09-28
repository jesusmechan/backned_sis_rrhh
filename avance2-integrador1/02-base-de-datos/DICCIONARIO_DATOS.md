# Diccionario de datos — rrhh_andina

> Documento **generado** desde `database/01_install.sql` con `generar-diccionario.mjs`. No editar a mano: modificar el script SQL y volver a generar.

Motor: PostgreSQL 16+. Tablas: **34**. Tipos enumerados: **22**. Índices explícitos: **34**.

Convenciones: nombres en `snake_case` y singular; PK sustituta `id_<tabla>` con `IDENTITY`; fechas de auditoría en `TIMESTAMPTZ`; montos en `NUMERIC(12,2)`.

## Índice

- **Catálogos organizacionales**: [`area`](#area), [`cargo`](#cargo), [`horario_laboral`](#horario_laboral), [`tipo_permiso`](#tipo_permiso), [`parametro_sistema`](#parametro_sistema)
- **Seguridad y menú**: [`rol`](#rol), [`permiso_funcional`](#permiso_funcional), [`rol_permiso`](#rol_permiso), [`menu_item`](#menu_item), [`menu_rol`](#menu_rol), [`usuario`](#usuario), [`refresh_token`](#refresh_token)
- **Personal y asistencia**: [`empleado`](#empleado), [`contrato`](#contrato), [`marcacion`](#marcacion)
- **Flujos de aprobación y trámites**: [`configuracion_aprobacion`](#configuracion_aprobacion), [`configuracion_aprobacion_detalle`](#configuracion_aprobacion_detalle), [`solicitud_permiso`](#solicitud_permiso), [`solicitud_hora_extra`](#solicitud_hora_extra), [`solicitud_paso_aprobacion`](#solicitud_paso_aprobacion), [`historial_solicitud`](#historial_solicitud)
- **Operación y control**: [`auditoria`](#auditoria), [`notificacion`](#notificacion), [`carga_masiva`](#carga_masiva), [`carga_masiva_detalle`](#carga_masiva_detalle), [`reporte_generado`](#reporte_generado)
- **Planilla y contabilidad**: [`cuenta_contable`](#cuenta_contable), [`planilla`](#planilla), [`planilla_detalle`](#planilla_detalle), [`asiento_contable`](#asiento_contable), [`asiento_linea`](#asiento_linea)
- **Desempeño y reclutamiento**: [`evaluacion_desempeno`](#evaluacion_desempeno), [`convocatoria`](#convocatoria), [`postulacion`](#postulacion)

## Catálogos organizacionales

### area

Áreas de la consultora (Gerencia, Contabilidad, RR. HH., …).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_area` | INTEGER | No |  | PK, IDENTITY |
| `nombre` | VARCHAR(80) | No |  | UK |
| `descripcion` | VARCHAR(250) | Sí |  |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

### cargo

Puestos de trabajo asignables a un colaborador.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_cargo` | INTEGER | No |  | PK, IDENTITY |
| `nombre` | VARCHAR(80) | No |  | UK |
| `descripcion` | VARCHAR(250) | Sí |  |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

### horario_laboral

Jornadas con hora de ingreso, salida y minutos de refrigerio.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_horario` | INTEGER | No |  | PK, IDENTITY |
| `nombre` | VARCHAR(80) | No |  | UK |
| `hora_ingreso` | TIME | No |  |  |
| `hora_salida` | TIME | No |  |  |
| `minutos_refrigerio` | INTEGER | No | 60 |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_horario_rango CHECK (hora_salida > hora_ingreso)`
- `ck_horario_refrigerio CHECK (minutos_refrigerio BETWEEN 0 AND 180)`

### tipo_permiso

Catálogo de permisos (salud, vacaciones, duelo, …).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_tipo_permiso` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(30) | No |  | UK |
| `nombre` | VARCHAR(80) | No |  | UK |
| `requiere_sustento` | BOOLEAN | No | FALSE |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

### parametro_sistema

Diccionario clave/valor: tasas de planilla, topes de horas extras, zona horaria.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `clave` | VARCHAR(80) | No |  | PK |
| `valor` | VARCHAR(200) | No |  |  |
| `descripcion` | VARCHAR(300) | Sí |  |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

## Seguridad y menú

### rol

Perfiles de acceso: ADMIN, GERENCIA, RRHH, JEFE, EMPLEADO.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_rol` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(30) | No |  | UK |
| `nombre` | VARCHAR(80) | No |  |  |
| `descripcion` | VARCHAR(250) | Sí |  |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

### permiso_funcional

Acciones autorizables del sistema agrupadas por módulo.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_permiso` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(60) | No |  | UK |
| `nombre` | VARCHAR(120) | No |  |  |
| `modulo` | VARCHAR(40) | No |  |  |
| `descripcion` | VARCHAR(250) | Sí |  |  |

### rol_permiso

Puente N:M entre rol y permiso funcional.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_rol` | INTEGER | No |  | PK, FK → rol.id_rol |
| `id_permiso` | INTEGER | No |  | PK, FK → permiso_funcional.id_permiso |
| `fecha_asignacion` | TIMESTAMPTZ | No | NOW() |  |

### menu_item

Opciones del menú lateral (ruta, icono, grupo y orden).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_menu` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(40) | No |  | UK |
| `etiqueta` | VARCHAR(80) | No |  |  |
| `ruta` | VARCHAR(120) | No |  |  |
| `icono` | VARCHAR(40) | No |  |  |
| `grupo` | VARCHAR(40) | No |  |  |
| `descripcion` | VARCHAR(200) | Sí |  |  |
| `orden` | INTEGER | No | 0 |  |
| `activo` | BOOLEAN | No | TRUE |  |

### menu_rol

Puente N:M: qué opciones de menú ve cada rol.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_menu` | INTEGER | No |  | PK, FK → menu_item.id_menu |
| `id_rol` | INTEGER | No |  | PK, FK → rol.id_rol |

### usuario

Cuenta de acceso; como máximo una por empleado. Contraseña con hash BCrypt.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_usuario` | INTEGER | No |  | PK, IDENTITY |
| `id_empleado` | INTEGER | Sí |  | FK → empleado.id_empleado, UK |
| `id_rol` | INTEGER | No |  | FK → rol.id_rol |
| `nombre_usuario` | VARCHAR(60) | No |  | UK |
| `correo` | VARCHAR(120) | No |  | UK |
| `password_hash` | TEXT | No |  |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `ultimo_acceso` | TIMESTAMPTZ | Sí |  |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Índices:

- `ix_usuario_rol` (id_rol)

### refresh_token

Tokens de renovación JWT (7 días) con marca de revocación.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_refresh_token` | INTEGER | No |  | PK, IDENTITY |
| `id_usuario` | INTEGER | No |  | FK → usuario.id_usuario |
| `token` | VARCHAR(200) | No |  | UK |
| `fecha_expiracion` | TIMESTAMPTZ | No |  |  |
| `revocado` | BOOLEAN | No | FALSE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Índices:

- `ix_refresh_token_usuario` (id_usuario, revocado)

## Personal y asistencia

### empleado

Ficha del colaborador. id_jefe_inmediato define el organigrama.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_empleado` | INTEGER | No |  | PK, IDENTITY |
| `codigo_empleado` | VARCHAR(20) | No |  | UK |
| `tipo_documento` | tipo_documento | No | 'DNI' | UK (tipo_documento, numero_documento) |
| `numero_documento` | VARCHAR(20) | No |  | UK (tipo_documento, numero_documento) |
| `nombres` | VARCHAR(80) | No |  |  |
| `apellido_paterno` | VARCHAR(80) | No |  |  |
| `apellido_materno` | VARCHAR(80) | No |  |  |
| `fecha_nacimiento` | DATE | Sí |  |  |
| `sexo` | sexo_empleado | Sí |  |  |
| `correo_institucional` | VARCHAR(120) | No |  | UK |
| `correo_personal` | VARCHAR(120) | Sí |  |  |
| `telefono` | VARCHAR(20) | Sí |  |  |
| `direccion` | VARCHAR(200) | Sí |  |  |
| `fecha_ingreso` | DATE | No |  |  |
| `fecha_cese` | DATE | Sí |  |  |
| `id_area` | INTEGER | No |  | FK → area.id_area |
| `id_cargo` | INTEGER | No |  | FK → cargo.id_cargo |
| `id_horario` | INTEGER | No |  | FK → horario_laboral.id_horario |
| `tipo_contrato` | tipo_contrato | No | 'PLANILLA' |  |
| `estado` | estado_empleado | No | 'ACTIVO' |  |
| `id_jefe_inmediato` | INTEGER | Sí |  | FK → empleado.id_empleado |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_empleado_cese CHECK (fecha_cese IS NULL OR fecha_cese >= fecha_ingreso)`
- `ck_empleado_nacimiento CHECK (fecha_nacimiento IS NULL OR fecha_nacimiento < fecha_ingreso)`
- `ck_empleado_dni CHECK ( tipo_documento <> 'DNI' OR numero_documento ~ '^[0-9]{8}$' )`
- `ck_empleado_no_autojefe CHECK ( id_jefe_inmediato IS NULL OR id_jefe_inmediato <> id_empleado )`

Índices:

- `ix_empleado_area` (id_area)
- `ix_empleado_estado` (estado)
- `ix_empleado_jefe` (id_jefe_inmediato)

### contrato

Vínculo laboral; a lo sumo uno VIGENTE por empleado. Base de planilla y vacaciones.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_contrato` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(20) | No |  | UK |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `modalidad` | modalidad_contrato | No |  |  |
| `id_horario` | INTEGER | No |  | FK → horario_laboral.id_horario |
| `fecha_inicio` | DATE | No |  |  |
| `fecha_fin` | DATE | Sí |  |  |
| `remuneracion_basica` | NUMERIC(12, 2) | No | 0 |  |
| `regimen_pensionario` | regimen_pensionario | No | 'ONP' |  |
| `afp_nombre` | VARCHAR(20) | Sí |  |  |
| `tiene_asignacion_familiar` | BOOLEAN | No | FALSE |  |
| `estado` | estado_contrato | No | 'VIGENTE' |  |
| `observaciones` | VARCHAR(300) | Sí |  |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_contrato_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)`
- `ck_contrato_practicas_fin CHECK ( modalidad <> 'PRACTICANTE' OR fecha_fin IS NOT NULL )`
- `ck_contrato_afp CHECK ( regimen_pensionario <> 'AFP' OR afp_nombre IS NOT NULL )`

Índices:

- `uq_contrato_empleado_vigente` (id_empleado) — único — parcial: `WHERE estado = 'VIGENTE'`

### marcacion

Registros de ingreso y salida. fecha se calcula en hora de Lima.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_marcacion` | INTEGER | No |  | PK, IDENTITY |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `tipo` | tipo_marcacion | No |  |  |
| `fecha_hora` | TIMESTAMPTZ | No | NOW() |  |
| `fecha` | DATE (calculada) | Sí |  |  |
| `origen` | VARCHAR(20) | No | 'WEB' |  |
| `observacion` | VARCHAR(250) | Sí |  |  |
| `id_usuario_registro` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_marcacion_origen CHECK (origen IN ('WEB', 'MOVIL', 'MANUAL', 'CORRECCION'))`

Índices:

- `ix_marcacion_empleado_fecha` (id_empleado, fecha)
- `ix_marcacion_fecha` (fecha)

## Flujos de aprobación y trámites

### configuracion_aprobacion

Cabecera del circuito de aprobación por tipo de trámite.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_configuracion` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(40) | No |  | UK |
| `nombre` | VARCHAR(120) | No |  |  |
| `tipo_origen` | tipo_origen_flujo | No |  |  |
| `id_tipo_permiso` | INTEGER | Sí |  | FK → tipo_permiso.id_tipo_permiso |
| `descripcion` | VARCHAR(300) | Sí |  |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_config_tipo_permiso CHECK ( (tipo_origen = 'PERMISO') OR (tipo_origen = 'HORA_EXTRA' AND id_tipo_permiso IS NULL) )`

Índices:

- `uq_config_activa_especifica` (tipo_origen, id_tipo_permiso) — único — parcial: `WHERE activo AND id_tipo_permiso IS NOT NULL`
- `uq_config_activa_generica` (tipo_origen) — único — parcial: `WHERE activo AND id_tipo_permiso IS NULL`

### configuracion_aprobacion_detalle

Pasos configurados del circuito (jefe, rol o usuario).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_detalle` | INTEGER | No |  | PK, IDENTITY |
| `id_configuracion` | INTEGER | No |  | FK → configuracion_aprobacion.id_configuracion, UK (id_configuracion, numero_paso) |
| `numero_paso` | INTEGER | No |  | UK (id_configuracion, numero_paso) |
| `nombre_paso` | VARCHAR(120) | No |  |  |
| `tipo_aprobador` | tipo_aprobador | No |  |  |
| `id_rol` | INTEGER | Sí |  | FK → rol.id_rol |
| `id_usuario` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `es_obligatorio` | BOOLEAN | No | TRUE |  |
| `activo` | BOOLEAN | No | TRUE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_config_detalle_paso CHECK (numero_paso >= 1)`
- `ck_config_detalle_aprobador CHECK ( (tipo_aprobador = 'JEFE_INMEDIATO' AND id_rol IS NULL AND id_usuario IS NULL) OR (tipo_aprobador = 'ROL' AND id_rol IS NOT NULL AND id_usuario IS NULL) OR (tipo_aprobador = 'USUARIO' AND id_usuario IS NOT NULL AND id_rol IS NULL) )`

Índices:

- `ix_config_detalle_config` (id_configuracion, numero_paso)

### solicitud_permiso

Pedido de permiso. No guarda al aprobador: el circuito vive en los pasos.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_solicitud_permiso` | INTEGER | No |  | PK, IDENTITY |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `id_tipo_permiso` | INTEGER | No |  | FK → tipo_permiso.id_tipo_permiso |
| `id_configuracion` | INTEGER | Sí |  | FK → configuracion_aprobacion.id_configuracion |
| `fecha_inicio` | DATE | No |  |  |
| `fecha_fin` | DATE | No |  |  |
| `hora_inicio` | TIME | Sí |  |  |
| `hora_fin` | TIME | Sí |  |  |
| `motivo` | VARCHAR(400) | No |  |  |
| `estado` | estado_solicitud | No | 'PENDIENTE' |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_permiso_rango_fechas CHECK (fecha_fin >= fecha_inicio)`
- `ck_permiso_horas CHECK ( (hora_inicio IS NULL AND hora_fin IS NULL) OR (hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_fin > hora_inicio) )`
- `ck_permiso_motivo CHECK (length(btrim(motivo)) >= 5)`

Índices:

- `ix_permiso_empleado` (id_empleado)
- `ix_permiso_estado` (estado)
- `ix_permiso_fechas` (fecha_inicio, fecha_fin)
- `ix_permiso_config` (id_configuracion)

### solicitud_hora_extra

Pedido de horas extras (máx. 8 h por registro; topes diario y semanal por parámetro).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_solicitud_hora_extra` | INTEGER | No |  | PK, IDENTITY |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `id_configuracion` | INTEGER | Sí |  | FK → configuracion_aprobacion.id_configuracion |
| `fecha` | DATE | No |  |  |
| `hora_inicio` | TIME | No |  |  |
| `hora_fin` | TIME | No |  |  |
| `cantidad_horas` | NUMERIC(5,2) | No |  |  |
| `motivo` | VARCHAR(400) | No |  |  |
| `estado` | estado_solicitud | No | 'PENDIENTE' |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_actualizacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_hora_extra_rango CHECK (hora_fin > hora_inicio)`
- `ck_hora_extra_cantidad CHECK (cantidad_horas > 0 AND cantidad_horas <= 8)`
- `ck_hora_extra_motivo CHECK (length(btrim(motivo)) >= 5)`

Índices:

- `ix_hora_extra_empleado` (id_empleado)
- `ix_hora_extra_estado` (estado)
- `ix_hora_extra_fecha` (fecha)
- `ix_hora_extra_config` (id_configuracion)
- `uq_hora_extra_pendiente_dia` (id_empleado, fecha) — único — parcial: `WHERE estado = 'PENDIENTE'`

### solicitud_paso_aprobacion

Instancia real de cada paso por solicitud (XOR permiso u hora extra).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_paso_solicitud` | INTEGER | No |  | PK, IDENTITY |
| `id_solicitud_permiso` | INTEGER | Sí |  | FK → solicitud_permiso.id_solicitud_permiso |
| `id_solicitud_hora_extra` | INTEGER | Sí |  | FK → solicitud_hora_extra.id_solicitud_hora_extra |
| `id_configuracion` | INTEGER | No |  | FK → configuracion_aprobacion.id_configuracion |
| `id_detalle` | INTEGER | No |  | FK → configuracion_aprobacion_detalle.id_detalle |
| `numero_paso` | INTEGER | No |  |  |
| `nombre_paso` | VARCHAR(120) | No |  |  |
| `tipo_aprobador` | tipo_aprobador | No |  |  |
| `id_rol` | INTEGER | Sí |  | FK → rol.id_rol |
| `id_usuario_asignado` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `estado` | estado_paso_aprobacion | No | 'PENDIENTE' |  |
| `id_usuario_decision` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `fecha_inicio` | TIMESTAMPTZ | Sí |  |  |
| `fecha_decision` | TIMESTAMPTZ | Sí |  |  |
| `comentario` | VARCHAR(400) | Sí |  |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_paso_una_solicitud CHECK ( (id_solicitud_permiso IS NOT NULL AND id_solicitud_hora_extra IS NULL) OR (id_solicitud_permiso IS NULL AND id_solicitud_hora_extra IS NOT NULL) )`
- `ck_paso_numero CHECK (numero_paso >= 1)`
- `ck_paso_decision CHECK ( estado NOT IN ('APROBADO', 'RECHAZADO') OR (id_usuario_decision IS NOT NULL AND fecha_decision IS NOT NULL) )`

Índices:

- `uq_paso_permiso_numero` (id_solicitud_permiso, numero_paso) — único — parcial: `WHERE id_solicitud_permiso IS NOT NULL`
- `uq_paso_hora_extra_numero` (id_solicitud_hora_extra, numero_paso) — único — parcial: `WHERE id_solicitud_hora_extra IS NOT NULL`
- `uq_paso_permiso_en_curso` (id_solicitud_permiso) — único — parcial: `WHERE id_solicitud_permiso IS NOT NULL AND estado = 'EN_CURSO'`
- `uq_paso_hora_extra_en_curso` (id_solicitud_hora_extra) — único — parcial: `WHERE id_solicitud_hora_extra IS NOT NULL AND estado = 'EN_CURSO'`
- `ix_paso_permiso` (id_solicitud_permiso, estado)
- `ix_paso_hora_extra` (id_solicitud_hora_extra, estado)
- `ix_paso_asignado` (id_usuario_asignado, estado)
- `ix_paso_rol` (id_rol, estado)

### historial_solicitud

Trazabilidad de cada acción sobre una solicitud.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_historial` | INTEGER | No |  | PK, IDENTITY |
| `id_solicitud_permiso` | INTEGER | Sí |  | FK → solicitud_permiso.id_solicitud_permiso |
| `id_solicitud_hora_extra` | INTEGER | Sí |  | FK → solicitud_hora_extra.id_solicitud_hora_extra |
| `id_paso_solicitud` | INTEGER | Sí |  | FK → solicitud_paso_aprobacion.id_paso_solicitud |
| `id_usuario` | INTEGER | No |  | FK → usuario.id_usuario |
| `accion` | accion_solicitud | No |  |  |
| `estado_anterior` | estado_solicitud | Sí |  |  |
| `estado_nuevo` | estado_solicitud | No |  |  |
| `comentario` | VARCHAR(400) | Sí |  |  |
| `fecha_hora` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_historial_una_solicitud CHECK ( (id_solicitud_permiso IS NOT NULL AND id_solicitud_hora_extra IS NULL) OR (id_solicitud_permiso IS NULL AND id_solicitud_hora_extra IS NOT NULL) )`

Índices:

- `ix_historial_permiso` (id_solicitud_permiso)
- `ix_historial_hora_extra` (id_solicitud_hora_extra)

## Operación y control

### auditoria

Bitácora general de operaciones (quién, qué, cuándo, IP).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_auditoria` | BIGINT | No |  | PK, IDENTITY |
| `id_usuario` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `accion` | VARCHAR(80) | No |  |  |
| `entidad` | VARCHAR(60) | No |  |  |
| `id_entidad` | INTEGER | Sí |  |  |
| `detalle` | JSONB | Sí |  |  |
| `direccion_ip` | VARCHAR(45) | Sí |  |  |
| `fecha_hora` | TIMESTAMPTZ | No | NOW() |  |

Índices:

- `ix_auditoria_entidad` (entidad, id_entidad)
- `ix_auditoria_usuario_fecha` (id_usuario, fecha_hora DESC)
- `ix_auditoria_fecha` (fecha_hora DESC)

### notificacion

Avisos en campana/STOMP: BANDEJA, APROBADA, RECHAZADA.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_notificacion` | INTEGER | No |  | PK, IDENTITY |
| `id_usuario` | INTEGER | No |  | FK → usuario.id_usuario |
| `tipo` | VARCHAR(40) | No |  |  |
| `titulo` | VARCHAR(160) | No |  |  |
| `mensaje` | VARCHAR(400) | No |  |  |
| `ruta` | VARCHAR(160) | Sí |  |  |
| `tipo_solicitud` | VARCHAR(20) | Sí |  |  |
| `id_solicitud` | INTEGER | Sí |  |  |
| `id_paso` | INTEGER | Sí |  |  |
| `leida` | BOOLEAN | No | FALSE |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_notificacion_tipo CHECK (tipo IN ('BANDEJA', 'APROBADA', 'RECHAZADA'))`

Índices:

- `ix_notificacion_usuario` (id_usuario, leida, fecha_creacion DESC)

### carga_masiva

Cabecera de una importación Excel de empleados.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_carga` | INTEGER | No |  | PK, IDENTITY |
| `id_usuario` | INTEGER | No |  | FK → usuario.id_usuario |
| `nombre_archivo` | VARCHAR(255) | No |  |  |
| `total_filas` | INTEGER | No | 0 |  |
| `filas_exitosas` | INTEGER | No | 0 |  |
| `filas_fallidas` | INTEGER | No | 0 |  |
| `estado` | estado_carga | No | 'PROCESANDO' |  |
| `mensaje` | VARCHAR(400) | Sí |  |  |
| `fecha_inicio` | TIMESTAMPTZ | No | NOW() |  |
| `fecha_fin` | TIMESTAMPTZ | Sí |  |  |

Reglas (CHECK):

- `ck_carga_totales CHECK ( total_filas >= 0 AND filas_exitosas >= 0 AND filas_fallidas >= 0 AND filas_exitosas + filas_fallidas <= total_filas )`

### carga_masiva_detalle

Resultado por fila de la importación.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_detalle` | INTEGER | No |  | PK, IDENTITY |
| `id_carga` | INTEGER | No |  | FK → carga_masiva.id_carga, UK (id_carga, numero_fila) |
| `numero_fila` | INTEGER | No |  | UK (id_carga, numero_fila) |
| `resultado` | resultado_fila_carga | No |  |  |
| `id_empleado` | INTEGER | Sí |  | FK → empleado.id_empleado |
| `mensaje` | VARCHAR(400) | Sí |  |  |
| `datos` | JSONB | Sí |  |  |

### reporte_generado

Registro de cada exportación Excel/PDF con sus filtros.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_reporte` | INTEGER | No |  | PK, IDENTITY |
| `id_usuario` | INTEGER | No |  | FK → usuario.id_usuario |
| `tipo` | tipo_reporte | No |  |  |
| `formato` | formato_reporte | No |  |  |
| `filtros` | JSONB | Sí |  |  |
| `fecha_generacion` | TIMESTAMPTZ | No | NOW() |  |

## Planilla y contabilidad

### cuenta_contable

Plan de cuentas usado por el asiento de planilla.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_cuenta` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(20) | No |  | UK |
| `nombre` | VARCHAR(80) | No |  |  |
| `uso` | VARCHAR(40) | No |  | UK |
| `naturaleza` | VARCHAR(12) | No | 'GASTO' |  |
| `activo` | BOOLEAN | No | TRUE |  |

### planilla

Cabecera mensual de remuneraciones (única por año y mes).

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_planilla` | INTEGER | No |  | PK, IDENTITY |
| `anio` | INTEGER | No |  | UK (anio, mes) |
| `mes` | INTEGER | No |  | UK (anio, mes) |
| `estado` | estado_planilla | No | 'BORRADOR' |  |
| `total_bruto` | NUMERIC(12, 2) | No | 0 |  |
| `total_descuentos` | NUMERIC(12, 2) | No | 0 |  |
| `total_aportes` | NUMERIC(12, 2) | No | 0 |  |
| `total_neto` | NUMERIC(12, 2) | No | 0 |  |
| `observaciones` | VARCHAR(400) | Sí |  |  |
| `fecha_calculo` | TIMESTAMPTZ | Sí |  |  |
| `fecha_cierre` | TIMESTAMPTZ | Sí |  |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_planilla_mes CHECK (mes BETWEEN 1 AND 12)`

### planilla_detalle

Boleta de un colaborador dentro de la planilla.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_detalle` | INTEGER | No |  | PK, IDENTITY |
| `id_planilla` | INTEGER | No |  | FK → planilla.id_planilla |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `id_contrato` | INTEGER | Sí |  | FK → contrato.id_contrato |
| `modalidad` | VARCHAR(20) | Sí |  |  |
| `regimen_pensionario` | VARCHAR(20) | Sí |  |  |
| `remuneracion_basica` | NUMERIC(12, 2) | No | 0 |  |
| `asignacion_familiar` | NUMERIC(12, 2) | No | 0 |  |
| `horas_extras` | NUMERIC(8, 2) | No | 0 |  |
| `monto_horas_extras` | NUMERIC(12, 2) | No | 0 |  |
| `dias_computados` | NUMERIC(6, 2) | No | 30 |  |
| `dias_no_laborados` | NUMERIC(6, 2) | No | 0 |  |
| `descuento_ausencias` | NUMERIC(12, 2) | No | 0 |  |
| `onp` | NUMERIC(12, 2) | No | 0 |  |
| `afp_aporte` | NUMERIC(12, 2) | No | 0 |  |
| `afp_comision` | NUMERIC(12, 2) | No | 0 |  |
| `afp_seguro` | NUMERIC(12, 2) | No | 0 |  |
| `quinta_categoria` | NUMERIC(12, 2) | No | 0 |  |
| `essalud` | NUMERIC(12, 2) | No | 0 |  |
| `bruto` | NUMERIC(12, 2) | No | 0 |  |
| `neto` | NUMERIC(12, 2) | No | 0 |  |

### asiento_contable

Asiento de partida doble generado al cerrar la planilla.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_asiento` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(30) | No |  | UK |
| `id_planilla` | INTEGER | Sí |  | FK → planilla.id_planilla |
| `fecha` | DATE | No |  |  |
| `glosa` | VARCHAR(200) | No |  |  |
| `estado` | estado_asiento | No | 'CONTABILIZADO' |  |
| `total_debe` | NUMERIC(12, 2) | No | 0 |  |
| `total_haber` | NUMERIC(12, 2) | No | 0 |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

### asiento_linea

Líneas debe/haber del asiento.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_linea` | INTEGER | No |  | PK, IDENTITY |
| `id_asiento` | INTEGER | No |  | FK → asiento_contable.id_asiento |
| `cuenta` | VARCHAR(20) | No |  |  |
| `nombre_cuenta` | VARCHAR(80) | No |  |  |
| `debe` | NUMERIC(12, 2) | No | 0 |  |
| `haber` | NUMERIC(12, 2) | No | 0 |  |

## Desempeño y reclutamiento

### evaluacion_desempeno

Evaluación 1–5 en puntualidad, calidad, cooperación e iniciativa.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_evaluacion` | INTEGER | No |  | PK, IDENTITY |
| `id_empleado` | INTEGER | No |  | FK → empleado.id_empleado |
| `id_evaluador` | INTEGER | Sí |  | FK → usuario.id_usuario |
| `periodo` | VARCHAR(20) | No |  |  |
| `fecha` | DATE | No | CURRENT_DATE |  |
| `puntualidad` | INTEGER | No |  |  |
| `calidad` | INTEGER | No |  |  |
| `cooperacion` | INTEGER | No |  |  |
| `iniciativa` | INTEGER | No |  |  |
| `promedio` | NUMERIC(4, 2) | No |  |  |
| `comentario` | VARCHAR(400) | Sí |  |  |
| `estado` | estado_evaluacion | No | 'CERRADA' |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

Reglas (CHECK):

- `ck_eval_puntajes CHECK ( puntualidad BETWEEN 1 AND 5 AND calidad BETWEEN 1 AND 5 AND cooperacion BETWEEN 1 AND 5 AND iniciativa BETWEEN 1 AND 5 )`

### convocatoria

Proceso de reclutamiento para un puesto.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_convocatoria` | INTEGER | No |  | PK, IDENTITY |
| `codigo` | VARCHAR(20) | No |  | UK |
| `puesto` | VARCHAR(120) | No |  |  |
| `id_area` | INTEGER | Sí |  | FK → area.id_area |
| `vacantes` | INTEGER | No | 1 |  |
| `fecha_inicio` | DATE | No |  |  |
| `fecha_fin` | DATE | Sí |  |  |
| `descripcion` | VARCHAR(400) | Sí |  |  |
| `estado` | estado_convocatoria | No | 'ABIERTA' |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

### postulacion

Candidato registrado en una convocatoria y su estado.

| Columna | Tipo | Nulo | Por defecto | Clave / notas |
|---|---|---|---|---|
| `id_postulacion` | INTEGER | No |  | PK, IDENTITY |
| `id_convocatoria` | INTEGER | No |  | FK → convocatoria.id_convocatoria |
| `nombres` | VARCHAR(80) | No |  |  |
| `apellidos` | VARCHAR(80) | No |  |  |
| `documento` | VARCHAR(20) | No |  |  |
| `correo` | VARCHAR(120) | Sí |  |  |
| `telefono` | VARCHAR(20) | Sí |  |  |
| `puntaje` | INTEGER | Sí |  |  |
| `estado` | estado_postulacion | No | 'POSTULADO' |  |
| `observacion` | VARCHAR(400) | Sí |  |  |
| `fecha_creacion` | TIMESTAMPTZ | No | NOW() |  |

## Tipos enumerados

| Tipo | Valores |
|---|---|
| `tipo_documento` | DNI, CE, PASAPORTE |
| `sexo_empleado` | M, F |
| `tipo_contrato` | PLANILLA, RECIBO_HONORARIOS, PRACTICAS |
| `modalidad_contrato` | COLABORADOR, PRACTICANTE |
| `regimen_pensionario` | ONP, AFP, NINGUNO |
| `estado_contrato` | VIGENTE, FINALIZADO, ANULADO |
| `estado_empleado` | ACTIVO, INACTIVO, CESADO |
| `tipo_marcacion` | INGRESO, SALIDA |
| `estado_solicitud` | PENDIENTE, APROBADO, RECHAZADO, CANCELADO |
| `accion_solicitud` | REGISTRAR, ENVIAR_APROBACION, APROBAR, RECHAZAR, CANCELAR, ACTUALIZAR |
| `estado_carga` | PROCESANDO, COMPLETADA, COMPLETADA_CON_ERRORES, FALLIDA |
| `resultado_fila_carga` | INSERTADO, ACTUALIZADO, ERROR |
| `formato_reporte` | EXCEL, PDF |
| `tipo_reporte` | TRABAJADORES, PERMISOS, HORAS_EXTRAS, ASISTENCIA, USUARIOS |
| `tipo_origen_flujo` | PERMISO, HORA_EXTRA |
| `tipo_aprobador` | JEFE_INMEDIATO, ROL, USUARIO |
| `estado_paso_aprobacion` | PENDIENTE, EN_CURSO, APROBADO, RECHAZADO, OMITIDO, CANCELADO |
| `estado_planilla` | BORRADOR, CALCULADA, CERRADA, ANULADA |
| `estado_asiento` | BORRADOR, CONTABILIZADO, ANULADO |
| `estado_evaluacion` | BORRADOR, CERRADA |
| `estado_convocatoria` | ABIERTA, CERRADA, CANCELADA |
| `estado_postulacion` | POSTULADO, ENTREVISTA, SELECCIONADO, CONTRATADO, DESCARTADO |
