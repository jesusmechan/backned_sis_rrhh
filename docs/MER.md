# MER — Sistema de Gestión de RR. HH. Andina

Modelo entidad-relación lógico de `rrhh_andina`, alineado con `database/01_install.sql`.

Cardinalidades: `1` = uno, `N` = muchos, `0..1` = opcional.  
La solicitud **no** guarda al aprobador: el circuito vive en `configuracion_aprobacion` y se instancia en `solicitud_paso_aprobacion`.

## 1. Vista general

```mermaid
erDiagram
    AREA ||--o{ EMPLEADO : agrupa
    CARGO ||--o{ EMPLEADO : define
    HORARIO_LABORAL ||--o{ EMPLEADO : asigna
    EMPLEADO ||--o{ EMPLEADO : supervisa
    EMPLEADO ||--o| USUARIO : accede
    ROL ||--o{ USUARIO : otorga
    ROL ||--o{ ROL_PERMISO : tiene
    PERMISO_FUNCIONAL ||--o{ ROL_PERMISO : cubre
    MENU_ITEM ||--o{ MENU_ROL : aparece
    ROL ||--o{ MENU_ROL : ve
    EMPLEADO ||--o{ MARCACION : registra
    USUARIO ||--o{ MARCACION : anota
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
    USUARIO ||--o{ AUDITORIA : genera
    USUARIO ||--o{ CARGA_MASIVA : ejecuta
    CARGA_MASIVA ||--|{ CARGA_MASIVA_DETALLE : contiene
    EMPLEADO ||--o{ CARGA_MASIVA_DETALLE : resulta
    USUARIO ||--o{ REPORTE_GENERADO : emite
    USUARIO ||--o{ REFRESH_TOKEN : posee

    AREA {
        int id_area PK
        string nombre UK
        string descripcion
        bool activo
    }
    CARGO {
        int id_cargo PK
        string nombre UK
        string descripcion
        bool activo
    }
    HORARIO_LABORAL {
        int id_horario PK
        string nombre UK
        time hora_ingreso
        time hora_salida
        int minutos_refrigerio
        bool activo
    }
    EMPLEADO {
        int id_empleado PK
        string codigo_empleado UK
        string tipo_documento
        string numero_documento
        string nombres
        string apellido_paterno
        string apellido_materno
        date fecha_ingreso
        int id_area FK
        int id_cargo FK
        int id_horario FK
        int id_jefe_inmediato FK
        string tipo_contrato
        string estado
    }
    TIPO_PERMISO {
        int id_tipo_permiso PK
        string codigo UK
        string nombre UK
        bool requiere_sustento
        bool activo
    }
    PARAMETRO_SISTEMA {
        string clave PK
        string valor
        string descripcion
    }
    ROL {
        int id_rol PK
        string codigo UK
        string nombre
        bool activo
    }
    PERMISO_FUNCIONAL {
        int id_permiso PK
        string codigo UK
        string nombre
        string modulo
    }
    ROL_PERMISO {
        int id_rol PK_FK
        int id_permiso PK_FK
    }
    MENU_ITEM {
        int id_menu PK
        string codigo UK
        string etiqueta
        string ruta
        string icono
        string grupo
        int orden
        bool activo
    }
    MENU_ROL {
        int id_menu PK_FK
        int id_rol PK_FK
    }
    USUARIO {
        int id_usuario PK
        int id_empleado FK_UK
        int id_rol FK
        string nombre_usuario UK
        string correo UK
        string password_hash
        bool activo
    }
    MARCACION {
        int id_marcacion PK
        int id_empleado FK
        int id_usuario_registro FK
        string tipo
        datetime fecha_hora
        date fecha
        string origen
    }
    CONFIGURACION_APROBACION {
        int id_configuracion PK
        string codigo UK
        string nombre
        string tipo_origen
        int id_tipo_permiso FK
        bool activo
    }
    CONFIGURACION_APROBACION_DETALLE {
        int id_detalle PK
        int id_configuracion FK
        int numero_paso
        string nombre_paso
        string tipo_aprobador
        int id_rol FK
        int id_usuario FK
        bool es_obligatorio
    }
    SOLICITUD_PERMISO {
        int id_solicitud_permiso PK
        int id_empleado FK
        int id_tipo_permiso FK
        int id_configuracion FK
        date fecha_inicio
        date fecha_fin
        string motivo
        string estado
    }
    SOLICITUD_HORA_EXTRA {
        int id_solicitud_hora_extra PK
        int id_empleado FK
        int id_configuracion FK
        date fecha
        numeric cantidad_horas
        string motivo
        string estado
    }
    SOLICITUD_PASO_APROBACION {
        int id_paso_solicitud PK
        int id_solicitud_permiso FK
        int id_solicitud_hora_extra FK
        int id_configuracion FK
        int id_detalle FK
        int numero_paso
        string tipo_aprobador
        int id_rol FK
        int id_usuario_asignado FK
        int id_usuario_decision FK
        string estado
    }
    HISTORIAL_SOLICITUD {
        int id_historial PK
        int id_solicitud_permiso FK
        int id_solicitud_hora_extra FK
        int id_paso_solicitud FK
        int id_usuario FK
        string accion
        string estado_anterior
        string estado_nuevo
    }
    AUDITORIA {
        long id_auditoria PK
        int id_usuario FK
        string accion
        string entidad
        int id_entidad
        jsonb detalle
    }
    CARGA_MASIVA {
        int id_carga PK
        int id_usuario FK
        string nombre_archivo
        string estado
    }
    CARGA_MASIVA_DETALLE {
        int id_detalle PK
        int id_carga FK
        int numero_fila
        string resultado
        int id_empleado FK
    }
    REPORTE_GENERADO {
        int id_reporte PK
        int id_usuario FK
        string tipo
        string formato
    }
    REFRESH_TOKEN {
        int id_refresh_token PK
        int id_usuario FK
        string token UK
        datetime fecha_expiracion
        bool revocado
    }
```

`PARAMETRO_SISTEMA` no tiene FK: es un diccionario de claves (`max_horas_extras_diarias`, `zona_horaria`, etc.).

## 2. Cardinalidades

| Relación | Tipo | Notas |
|---|---|---|
| AREA / CARGO / HORARIO_LABORAL → EMPLEADO | 1 : N | Todo colaborador pertenece a un área, un cargo y un horario |
| EMPLEADO → EMPLEADO | 1 : N | `id_jefe_inmediato` (organigrama; no puede ser él mismo) |
| EMPLEADO → USUARIO | 1 : 0..1 | Un colaborador tiene como máximo una cuenta |
| ROL → USUARIO | 1 : N | ADMIN, RRHH, APROBADOR, EMPLEADO |
| ROL ↔ PERMISO_FUNCIONAL | N : M | Puente `rol_permiso` |
| MENU_ITEM ↔ ROL | N : M | Puente `menu_rol` (menú dinámico por perfil) |
| EMPLEADO → MARCACION | 1 : N | INGRESO / SALIDA; `fecha` se deriva en zona `America/Lima` |
| EMPLEADO → SOLICITUD_PERMISO / SOLICITUD_HORA_EXTRA | 1 : N | La solicitud no guarda al aprobador |
| TIPO_PERMISO → CONFIGURACION_APROBACION | 1 : 0..1 | Flujo específico o genérico por `tipo_origen` |
| CONFIGURACION_APROBACION → DETALLE | 1 : N | Pasos: `JEFE_INMEDIATO`, `ROL` o `USUARIO` |
| DETALLE → ROL / USUARIO | 0..1 | Solo si el paso es de ese tipo |
| SOLICITUD → PASO_APROBACION | 1 : N | Instancia (xor permiso **o** hora extra) |
| PASO → USUARIO (asignado / decisión) | 0..1 | Asignado al crear; decisión al aprobar o rechazar |
| SOLICITUD → HISTORIAL_SOLICITUD | 1 : N | Trazabilidad del trámite |
| CARGA_MASIVA → DETALLE | 1 : N | Filas del Excel |
| USUARIO → REFRESH_TOKEN | 1 : N | Sesiones JWT |

## 3. Núcleo del circuito de aprobación

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

`empleado.id_jefe_inmediato` solo resuelve el paso `JEFE_INMEDIATO`. No es el aprobador guardado en la solicitud.

## 4. Tipos enumerados

| Tipo | Valores |
|---|---|
| `tipo_documento` | DNI, CE, PASAPORTE |
| `sexo_empleado` | M, F |
| `tipo_contrato` | PLANILLA, RECIBO_HONORARIOS, PRACTICAS |
| `estado_empleado` | ACTIVO, INACTIVO, CESADO |
| `tipo_marcacion` | INGRESO, SALIDA |
| `estado_solicitud` | PENDIENTE, APROBADO, RECHAZADO, CANCELADO |
| `tipo_origen_flujo` | PERMISO, HORA_EXTRA |
| `tipo_aprobador` | JEFE_INMEDIATO, ROL, USUARIO |
| `estado_paso_aprobacion` | PENDIENTE, EN_CURSO, APROBADO, RECHAZADO, OMITIDO, CANCELADO |
| `formato_reporte` | EXCEL, PDF |
| `estado_carga` | PROCESANDO, COMPLETADA, COMPLETADA_CON_ERRORES, FALLIDA |

## 5. Módulos (tablas)

| Módulo | Tablas |
|---|---|
| Organización | `area`, `cargo`, `horario_laboral`, `empleado` |
| Seguridad | `rol`, `permiso_funcional`, `rol_permiso`, `usuario`, `refresh_token` |
| Menú | `menu_item`, `menu_rol` |
| Catálogos | `tipo_permiso`, `parametro_sistema` |
| Flujos | `configuracion_aprobacion`, `configuracion_aprobacion_detalle` |
| Trámites | `solicitud_permiso`, `solicitud_hora_extra`, `solicitud_paso_aprobacion`, `historial_solicitud` |
| Asistencia | `marcacion` |
| Operación | `carga_masiva`, `carga_masiva_detalle`, `reporte_generado`, `auditoria` |
