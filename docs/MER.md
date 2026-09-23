# MER — Sistema de Gestión de RR. HH. Andina

Modelo entidad-relación lógico de `rrhh_andina`, alineado con `database/01_install.sql`.

**Vista interactiva (recomendada para exposición):** [diagrams/rrhh-mer.html](diagrams/rrhh-mer.html) — pestañas por módulo e **Imprimir / PDF**.

Cardinalidades Mermaid: `||--o{` = 1:N, `||--|{` = 1:N obligatorio, `||--o|` = 1:0..1.  
La solicitud **no** guarda al aprobador: el circuito vive en `configuracion_aprobacion` y se instancia en `solicitud_paso_aprobacion`.

---

## 1. Organización y asistencia

```mermaid
erDiagram
    AREA ||--o{ EMPLEADO : agrupa
    CARGO ||--o{ EMPLEADO : define
    HORARIO_LABORAL ||--o{ EMPLEADO : asigna
    EMPLEADO ||--o{ EMPLEADO : supervisa
    EMPLEADO ||--o{ CONTRATO : firma
    HORARIO_LABORAL ||--o{ CONTRATO : jornada
    EMPLEADO ||--o{ MARCACION : registra

    AREA {
        int id_area PK
        string nombre UK
        bool activo
    }
    CARGO {
        int id_cargo PK
        string nombre UK
        bool activo
    }
    HORARIO_LABORAL {
        int id_horario PK
        string nombre UK
        time hora_ingreso
        time hora_salida
    }
    EMPLEADO {
        int id_empleado PK
        string codigo_empleado UK
        string numero_documento
        string nombres
        int id_area FK
        int id_cargo FK
        int id_horario FK
        int id_jefe_inmediato FK
        string estado
    }
    CONTRATO {
        int id_contrato PK
        string codigo UK
        int id_empleado FK
        string modalidad
        int id_horario FK
        date fecha_inicio
        numeric remuneracion_basica
        string estado
    }
    MARCACION {
        int id_marcacion PK
        int id_empleado FK
        string tipo
        datetime fecha_hora
        date fecha
        string origen
    }
```

`empleado.id_jefe_inmediato` es organigrama (no puede ser él mismo). Se usa cuando el paso del flujo es `JEFE_INMEDIATO`.

---

## 2. Seguridad, menú y sesión

```mermaid
erDiagram
    EMPLEADO ||--o| USUARIO : accede
    ROL ||--o{ USUARIO : otorga
    ROL ||--o{ ROL_PERMISO : tiene
    PERMISO_FUNCIONAL ||--o{ ROL_PERMISO : cubre
    MENU_ITEM ||--o{ MENU_ROL : aparece
    ROL ||--o{ MENU_ROL : ve
    USUARIO ||--o{ REFRESH_TOKEN : posee
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO ||--o{ AUDITORIA : genera

    USUARIO {
        int id_usuario PK
        int id_empleado FK_UK
        int id_rol FK
        string nombre_usuario UK
        string correo UK
        bool activo
    }
    ROL {
        int id_rol PK
        string codigo UK
        string nombre
        bool activo
    }
    NOTIFICACION {
        int id_notificacion PK
        int id_usuario FK
        string tipo
        string titulo
        bool leida
        string ruta
    }
    REFRESH_TOKEN {
        int id_refresh_token PK
        int id_usuario FK
        string token UK
        bool revocado
    }
```

Roles semilla: `ADMIN`, `GERENCIA`, `RRHH`, `JEFE`, `EMPLEADO`.  
`notificacion.tipo`: `BANDEJA` · `APROBADA` · `RECHAZADA`.

---

## 3. Flujos de aprobación y trámites (núcleo del curso)

```mermaid
erDiagram
    EMPLEADO ||--o{ SOLICITUD_PERMISO : pide
    EMPLEADO ||--o{ SOLICITUD_HORA_EXTRA : pide
    TIPO_PERMISO ||--o{ SOLICITUD_PERMISO : clasifica
    TIPO_PERMISO ||--o{ CONFIGURACION_APROBACION : especializa
    CONFIGURACION_APROBACION ||--|{ CONFIGURACION_APROBACION_DETALLE : define
    ROL ||--o{ CONFIGURACION_APROBACION_DETALLE : atiende
    USUARIO ||--o{ CONFIGURACION_APROBACION_DETALLE : atiende
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_PERMISO : aplica
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_HORA_EXTRA : aplica
    CONFIGURACION_APROBACION_DETALLE ||--o{ SOLICITUD_PASO_APROBACION : instancia
    SOLICITUD_PERMISO ||--o{ SOLICITUD_PASO_APROBACION : recorre
    SOLICITUD_HORA_EXTRA ||--o{ SOLICITUD_PASO_APROBACION : recorre
    SOLICITUD_PERMISO ||--o{ HISTORIAL_SOLICITUD : deja
    SOLICITUD_HORA_EXTRA ||--o{ HISTORIAL_SOLICITUD : deja
    SOLICITUD_PASO_APROBACION ||--o{ HISTORIAL_SOLICITUD : origina
    USUARIO ||--o{ HISTORIAL_SOLICITUD : actua

    CONFIGURACION_APROBACION {
        int id_configuracion PK
        string codigo UK
        string tipo_origen
        int id_tipo_permiso FK
        bool activo
    }
    CONFIGURACION_APROBACION_DETALLE {
        int id_detalle PK
        int id_configuracion FK
        int numero_paso
        string tipo_aprobador
        int id_rol FK
        int id_usuario FK
    }
    SOLICITUD_PERMISO {
        int id_solicitud_permiso PK
        int id_empleado FK
        int id_tipo_permiso FK
        int id_configuracion FK
        date fecha_inicio
        date fecha_fin
        string estado
    }
    SOLICITUD_HORA_EXTRA {
        int id_solicitud_hora_extra PK
        int id_empleado FK
        int id_configuracion FK
        date fecha
        numeric cantidad_horas
        string estado
    }
    SOLICITUD_PASO_APROBACION {
        int id_paso_solicitud PK
        int id_solicitud_permiso FK
        int id_solicitud_hora_extra FK
        int id_detalle FK
        int numero_paso
        string tipo_aprobador
        int id_usuario_asignado FK
        int id_usuario_decision FK
        string estado
    }
```

### Cómo se arma el circuito

```
EMPLEADO ──solicita──► SOLICITUD_PERMISO / SOLICITUD_HORA_EXTRA
                              │
                              │ usa
                              ▼
                    CONFIGURACION_APROBACION
                              │
                              │ 1:N pasos configurados
                              ▼
                 CONFIGURACION_APROBACION_DETALLE
                              │
                              │ se instancia (trigger)
                              ▼
                   SOLICITUD_PASO_APROBACION
                              │
                    ┌─────────┴─────────┐
                    │ Aprobar           │ Rechazar
                    ▼                   ▼
              siguiente paso      solicitud RECHAZADO
              o APROBADO          pasos restantes OMITIDO
```

Ejemplos de semilla: salud = Jefe → RRHH; vacaciones = Jefe → RRHH → GERENCIA; comisión = Jefe → GERENCIA.

---

## 4. Planilla y contabilidad

```mermaid
erDiagram
    EMPLEADO ||--o{ PLANILLA_DETALLE : liquida
    CONTRATO ||--o{ PLANILLA_DETALLE : referencia
    PLANILLA ||--|{ PLANILLA_DETALLE : contiene
    PLANILLA ||--o{ ASIENTO_CONTABLE : genera
    ASIENTO_CONTABLE ||--|{ ASIENTO_LINEA : detalla

    PLANILLA {
        int id_planilla PK
        int anio
        int mes
        string estado
        numeric total_neto
    }
    PLANILLA_DETALLE {
        int id_detalle PK
        int id_planilla FK
        int id_empleado FK
        int id_contrato FK
        numeric remuneracion_basica
        numeric neto
    }
    ASIENTO_CONTABLE {
        int id_asiento PK
        string codigo UK
        int id_planilla FK
        date fecha
        string estado
    }
    ASIENTO_LINEA {
        int id_linea PK
        int id_asiento FK
        string cuenta
        numeric debe
        numeric haber
    }
```

---

## 5. Mapa completo (solo relaciones)

Útil en diapositivas; sin atributos para que quepa en una hoja.

```mermaid
erDiagram
    AREA ||--o{ EMPLEADO : agrupa
    CARGO ||--o{ EMPLEADO : define
    HORARIO_LABORAL ||--o{ EMPLEADO : asigna
    EMPLEADO ||--o{ EMPLEADO : supervisa
    EMPLEADO ||--o| USUARIO : accede
    ROL ||--o{ USUARIO : otorga
    EMPLEADO ||--o{ CONTRATO : firma
    EMPLEADO ||--o{ MARCACION : registra
    EMPLEADO ||--o{ SOLICITUD_PERMISO : pide
    EMPLEADO ||--o{ SOLICITUD_HORA_EXTRA : pide
    TIPO_PERMISO ||--o{ SOLICITUD_PERMISO : clasifica
    CONFIGURACION_APROBACION ||--|{ CONFIGURACION_APROBACION_DETALLE : define
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_PERMISO : aplica
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_HORA_EXTRA : aplica
    SOLICITUD_PERMISO ||--o{ SOLICITUD_PASO_APROBACION : recorre
    SOLICITUD_HORA_EXTRA ||--o{ SOLICITUD_PASO_APROBACION : recorre
    CONFIGURACION_APROBACION_DETALLE ||--o{ SOLICITUD_PASO_APROBACION : instancia
    USUARIO ||--o{ NOTIFICACION : recibe
    PLANILLA ||--|{ PLANILLA_DETALLE : contiene
    EMPLEADO ||--o{ PLANILLA_DETALLE : liquida
    PLANILLA ||--o{ ASIENTO_CONTABLE : genera
```

---

## 6. Cardinalidades (resumen)

| Relación | Tipo | Notas |
|---|---|---|
| AREA / CARGO / HORARIO → EMPLEADO | 1 : N | Todo colaborador tiene área, cargo y horario |
| EMPLEADO → EMPLEADO | 1 : N | `id_jefe_inmediato` |
| EMPLEADO → USUARIO | 1 : 0..1 | Como máximo una cuenta |
| EMPLEADO → CONTRATO | 1 : N | A lo sumo un `VIGENTE` por empleado |
| ROL → USUARIO | 1 : N | Perfil del mantenedor `rol` |
| ROL ↔ PERMISO_FUNCIONAL | N : M | Puente `rol_permiso` |
| MENU_ITEM ↔ ROL | N : M | Puente `menu_rol` |
| CONFIGURACION → DETALLE | 1 : N | Pasos `JEFE_INMEDIATO` / `ROL` / `USUARIO` |
| SOLICITUD → PASO | 1 : N | XOR permiso **o** hora extra |
| PLANILLA → DETALLE | 1 : N | Una fila por empleado del periodo |
| USUARIO → NOTIFICACION | 1 : N | Campana / STOMP |

`PARAMETRO_SISTEMA` no tiene FK: diccionario de claves (`zona_horaria`, topes de horas extras, etc.).

---

## 7. Tipos enumerados

| Tipo | Valores |
|---|---|
| `tipo_documento` | DNI, CE, PASAPORTE |
| `sexo_empleado` | M, F |
| `tipo_contrato` / `modalidad_contrato` | PLANILLA, RECIBO_HONORARIOS, PRACTICAS / COLABORADOR, PRACTICANTE |
| `estado_empleado` | ACTIVO, INACTIVO, CESADO |
| `tipo_marcacion` | INGRESO, SALIDA |
| `estado_solicitud` | PENDIENTE, APROBADO, RECHAZADO, CANCELADO |
| `tipo_origen_flujo` | PERMISO, HORA_EXTRA |
| `tipo_aprobador` | JEFE_INMEDIATO, ROL, USUARIO |
| `estado_paso_aprobacion` | PENDIENTE, EN_CURSO, APROBADO, RECHAZADO, OMITIDO, CANCELADO |
| `estado_planilla` | BORRADOR, CALCULADA, CERRADA, ANULADA |
| `estado_asiento` | BORRADOR, CONTABILIZADO, ANULADO |
| `estado_evaluacion` | BORRADOR, CERRADA |
| `estado_convocatoria` | ABIERTA, CERRADA, CANCELADA |
| `estado_postulacion` | POSTULADO, ENTREVISTA, SELECCIONADO, CONTRATADO, DESCARTADO |
| `formato_reporte` | EXCEL, PDF |
| `estado_carga` | PROCESANDO, COMPLETADA, COMPLETADA_CON_ERRORES, FALLIDA |

---

## 8. Módulos (tablas)

| Módulo | Tablas |
|---|---|
| Organización | `area`, `cargo`, `horario_laboral`, `empleado`, `contrato` |
| Seguridad | `rol`, `permiso_funcional`, `rol_permiso`, `usuario`, `refresh_token`, `notificacion` |
| Menú | `menu_item`, `menu_rol` |
| Catálogos | `tipo_permiso`, `parametro_sistema`, `cuenta_contable` |
| Flujos | `configuracion_aprobacion`, `configuracion_aprobacion_detalle` |
| Trámites | `solicitud_permiso`, `solicitud_hora_extra`, `solicitud_paso_aprobacion`, `historial_solicitud` |
| Asistencia | `marcacion` |
| Operación | `carga_masiva`, `carga_masiva_detalle`, `reporte_generado`, `auditoria` |
| Planillas | `planilla`, `planilla_detalle` |
| Contabilidad | `asiento_contable`, `asiento_linea` |
| Desempeño | `evaluacion_desempeno` |
| Reclutamiento | `convocatoria`, `postulacion` |
