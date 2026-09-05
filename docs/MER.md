# MER — Sistema de Gestión de RR. HH. Andina

Modelo entidad-relación lógico de `rrhh_andina`, alineado con `database/01_schema.sql`.

Cardinalidades: `1` = uno, `N` = muchos, `0..1` = opcional.

## Diagrama

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
    EMPLEADO ||--o{ MARCACION : registra
    EMPLEADO ||--o{ SOLICITUD_PERMISO : solicita
    EMPLEADO ||--o{ SOLICITUD_HORA_EXTRA : solicita
    TIPO_PERMISO ||--o{ SOLICITUD_PERMISO : clasifica
    TIPO_PERMISO ||--o{ CONFIGURACION_APROBACION : especializa
    CONFIGURACION_APROBACION ||--|{ CONFIGURACION_APROBACION_DETALLE : define
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_PERMISO : aplica
    CONFIGURACION_APROBACION ||--o{ SOLICITUD_HORA_EXTRA : aplica
    CONFIGURACION_APROBACION_DETALLE ||--o{ SOLICITUD_PASO_APROBACION : instancia
    SOLICITUD_PERMISO ||--o{ SOLICITUD_PASO_APROBACION : recorre
    SOLICITUD_HORA_EXTRA ||--o{ SOLICITUD_PASO_APROBACION : recorre
    SOLICITUD_PERMISO ||--o{ HISTORIAL_SOLICITUD : deja
    SOLICITUD_HORA_EXTRA ||--o{ HISTORIAL_SOLICITUD : deja
    USUARIO ||--o{ HISTORIAL_SOLICITUD : actua
    USUARIO ||--o{ AUDITORIA : genera
    USUARIO ||--o{ CARGA_MASIVA : ejecuta
    CARGA_MASIVA ||--|{ CARGA_MASIVA_DETALLE : contiene
    USUARIO ||--o{ REPORTE_GENERADO : emite
    USUARIO ||--o{ REFRESH_TOKEN : posee

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
        int id_area FK
        int id_cargo FK
        int id_horario FK
        int id_jefe_inmediato FK
        string estado
    }
    TIPO_PERMISO {
        int id_tipo_permiso PK
        string codigo UK
        bool requiere_sustento
    }
    ROL {
        int id_rol PK
        string codigo UK
    }
    PERMISO_FUNCIONAL {
        int id_permiso PK
        string codigo UK
    }
    ROL_PERMISO {
        int id_rol PK_FK
        int id_permiso PK_FK
    }
    USUARIO {
        int id_usuario PK
        int id_empleado FK_UK
        int id_rol FK
        string nombre_usuario UK
        bool activo
    }
    MARCACION {
        int id_marcacion PK
        int id_empleado FK
        string tipo
        datetime fecha_hora
    }
    CONFIGURACION_APROBACION {
        int id_configuracion PK
        string codigo UK
        string tipo_origen
        int id_tipo_permiso FK
    }
    CONFIGURACION_APROBACION_DETALLE {
        int id_detalle PK
        int id_configuracion FK
        int numero_paso
        string tipo_aprobador
    }
    SOLICITUD_PERMISO {
        int id_solicitud_permiso PK
        int id_empleado FK
        int id_tipo_permiso FK
        int id_configuracion FK
        string estado
    }
    SOLICITUD_HORA_EXTRA {
        int id_solicitud_hora_extra PK
        int id_empleado FK
        int id_configuracion FK
        string estado
    }
    SOLICITUD_PASO_APROBACION {
        int id_paso_solicitud PK
        int id_solicitud_permiso FK
        int id_solicitud_hora_extra FK
        int id_detalle FK
        string estado
    }
    HISTORIAL_SOLICITUD {
        int id_historial PK
        int id_solicitud_permiso FK
        int id_solicitud_hora_extra FK
        int id_usuario FK
        string accion
    }
    AUDITORIA {
        long id_auditoria PK
        int id_usuario FK
        string accion
        string entidad
    }
    CARGA_MASIVA {
        int id_carga PK
        int id_usuario FK
        string estado
    }
    CARGA_MASIVA_DETALLE {
        int id_detalle PK
        int id_carga FK
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
    }
```

## Cardinalidades principales

| Relación | Tipo | Notas |
|---|---|---|
| AREA / CARGO / HORARIO → EMPLEADO | 1 : N | Todo colaborador pertenece a un área, cargo y horario |
| EMPLEADO → EMPLEADO | 1 : N | `id_jefe_inmediato` (organigrama; un empleado no es su propio jefe) |
| EMPLEADO → USUARIO | 1 : 0..1 | Un colaborador tiene como máximo una cuenta |
| ROL → USUARIO | 1 : N | ADMIN, RRHH, APROBADOR, EMPLEADO |
| ROL ↔ PERMISO_FUNCIONAL | N : M | Tabla puente `rol_permiso` |
| EMPLEADO → MARCACION | 1 : N | Ingreso / salida |
| EMPLEADO → SOLICITUD_PERMISO / HORA_EXTRA | 1 : N | La solicitud no guarda al aprobador |
| TIPO_PERMISO → CONFIGURACION_APROBACION | 1 : 0..1 | Flujo específico o genérico (`tipo_origen`) |
| CONFIGURACION_APROBACION → DETALLE | 1 : N | Pasos: `JEFE_INMEDIATO`, `ROL` o `USUARIO` |
| SOLICITUD → PASO_APROBACION | 1 : N | Instancia del flujo (xor permiso / hora extra) |
| CARGA_MASIVA → DETALLE | 1 : N+ | Filas del Excel |
| USUARIO → REFRESH_TOKEN | 1 : N | Sesiones JWT |

## Núcleo del circuito de aprobación

```
EMPLEADO ──solicita──► SOLICITUD_PERMISO / SOLICITUD_HORA_EXTRA
                              │
                              │ usa
                              ▼
                    CONFIGURACION_APROBACION
                              │
                              │ 1:N pasos
                              ▼
                 CONFIGURACION_APROBACION_DETALLE
                              │
                              │ se instancia
                              ▼
                   SOLICITUD_PASO_APROBACION
```

`parametro_sistema` es un catálogo de claves (tope de horas extras, etc.) y no tiene FK.
