# Planificación del proyecto

Semana 8 del temario: *planificación del proyecto*.

**Cronograma (FigJam):** [Planificación del proyecto RRHH Andina](https://www.figma.com/board/paAWBFnj1JSM0haMYUkahg) (diagrama de Gantt).

> Supuesto de calendario: la semana 1 del ciclo empieza el lunes 31 de agosto de 2026. Si el calendario oficial difiere, basta con mover la fecha de inicio: las demás tareas están encadenadas por dependencias.

---

## 1. Alcance

Sistema web de gestión de RR. HH. para la Consultora Contable Andina S.A.C. (MYPE): personal, contratos, asistencia, permisos, horas extras con aprobación configurable, planilla, contabilidad de planilla, desempeño, reclutamiento, usuarios, reportes y auditoría.

Fuera de alcance: integración con PLAME/SUNAT, pago bancario automático, app móvil nativa (la marcación móvil es web responsiva).

## 2. Estructura de desglose del trabajo (EDT)

```
1. Proyecto RRHH Andina
├── 1.1 Análisis (U1)
│   ├── 1.1.1 Plan de toma de requerimientos
│   ├── 1.1.2 Historias de usuario y alcance
│   └── 1.1.3 Laboratorio UX (empatía, sketches, wireframes, mockups)
├── 1.2 Diseño (U2) ← este avance
│   ├── 1.2.1 BPM AS-IS / TO-BE
│   ├── 1.2.2 Diseño lógico y físico de base de datos
│   ├── 1.2.3 Prototipo UX/UI navegable
│   └── 1.2.4 Reportes y documentación (Javadoc, Markdown, manual de usuario)
├── 1.3 Construcción (U3)
│   ├── 1.3.1 Backend API, seguridad JWT, flujos de aprobación
│   ├── 1.3.2 Frontend React por módulo
│   └── 1.3.3 Planilla peruana y asiento contable
└── 1.4 Pruebas y cierre (U4)
    ├── 1.4.1 Pruebas funcionales por caso de uso
    ├── 1.4.2 Pruebas con usuarios (RRHH, jefe, colaborador)
    ├── 1.4.3 Despliegue y manuales finales
    └── 1.4.4 Sustentación
```

## 3. Cronograma

| Semana | Fechas | Unidad | Actividad | Entregable |
|---|---|---|---|---|
| 1–2 | 31 ago – 13 sep | U1 | Toma de requerimientos | Plan de Toma de Requerimientos v1.0 |
| 3 | 14 – 20 sep | U1 | Historias y alcance | Backlog priorizado |
| 4 | 21 – 27 sep | U1 | Laboratorio UX | `docs/laboratorio-ux/` · **Avance 1** |
| 5 | 28 sep – 4 oct | U2 | BPM AS-IS y TO-BE | [BPM.md](../01-bpm/BPM.md) + FigJam |
| 6 | 5 – 11 oct | U2 | Diseño lógico y físico de BD | [DISENO_BD.md](../02-base-de-datos/DISENO_BD.md), diccionario, seguridad |
| 7 | 12 – 18 oct | U2 | Prototipo UX/UI | Figma navegable, [UX_UI_Y_REPORTES.md](../03-ux-ui/UX_UI_Y_REPORTES.md) |
| 8 | 19 – 25 oct | U2 | Reportes, documentación y planificación | Javadoc, guía, manual, este documento · **Avance 2** |
| 9–10 | 26 oct – 8 nov | U3 | Backend API y seguridad | Endpoints documentados en Swagger |
| 9–12 | 26 oct – 22 nov | U3 | Frontend React | Pantallas por módulo |
| 11–12 | 9 – 22 nov | U3 | Planilla y contabilidad | Boletas PDF y asiento · **Avance 3** |
| 13 | 23 – 29 nov | U4 | Pruebas funcionales | Matriz de casos de prueba |
| 14 | 30 nov – 6 dic | U4 | Pruebas de usuario | Acta de conformidad y ajustes |
| 15 | 7 – 13 dic | U4 | Despliegue y manuales | Versión final |
| 16 | 14 – 20 dic | U4 | Sustentación | **Entrega final** |

Ruta crítica: diseño → backend → planilla y contabilidad → pruebas → sustentación.

## 4. Estado del proyecto al Avance 2

Buena parte de la construcción se adelantó durante el análisis; este avance formaliza el diseño que ya está implementado y deja visibles los ajustes pendientes.

| Módulo | Estado | Pendiente para U3 |
|---|---|---|
| Autenticación, roles y menú dinámico | Implementado | — |
| Personal, contratos, maestros | Implementado | — |
| Permisos y horas extras con circuito configurable | Implementado | — |
| Bandeja y aprobación masiva | Implementado | — |
| Asistencia y marcación | Implementado | Solicitud de corrección desde el colaborador |
| Planilla (ONP/AFP, EsSalud, asignación familiar) y boletas PDF | Implementado, en ajuste | Validar quinta categoría con casos reales |
| Contabilidad (asiento al cerrar) | Implementado | — |
| Desempeño y reclutamiento | Implementado | Pasar de postulante contratado a empleado automáticamente |
| Reportes Excel/PDF | Implementado | Pendiente: filtros por fecha, área y estado (hoy solo Asistencia filtra por colaborador y fechas) |
| Seguridad de BD con roles de mínimo privilegio | Propuesto ([03_seguridad_bd.sql](../02-base-de-datos/03_seguridad_bd.sql)) | Aplicar y cambiar `backend/.env` a `rrhh_app` |

## 5. Roles y responsabilidades

Completar con los integrantes del grupo.

| Rol | Responsabilidades | Integrante |
|---|---|---|
| Jefe de proyecto | Cronograma, coordinación con el docente, control de entregables | |
| Analista de procesos | BPM, requerimientos, manual de usuario | |
| Diseñador UX/UI | Prototipo Figma, guía de estilos, pruebas de usabilidad | |
| Desarrollador backend y BD | API, base de datos, seguridad, Javadoc | |
| Desarrollador frontend y QA | Pantallas React, pruebas funcionales | |

### Matriz RACI (R responsable, A aprueba, C consultado, I informado)

| Entregable | Jefe proyecto | Analista | UX/UI | Backend/BD | Frontend/QA |
|---|---|---|---|---|---|
| BPM | A | R | C | C | I |
| Diseño de BD | A | C | I | R | C |
| Prototipo UX/UI | A | C | R | I | C |
| Documentación técnica | A | I | I | R | C |
| Manual de usuario | A | R | C | I | C |
| Planificación | R | C | C | C | C |

## 6. Riesgos

| # | Riesgo | Prob. | Impacto | Respuesta |
|---|---|---|---|---|
| R1 | Cambios en tasas legales (RMV, UIT, AFP) | Media | Alto | Tasas en `parametro_sistema`, sin recompilar |
| R2 | Cálculo de planilla incorrecto en casos especiales | Media | Alto | Casos de prueba con boletas reales y validación de RRHH antes de cerrar |
| R3 | Integrantes con poca disponibilidad al cierre del ciclo | Alta | Medio | Tareas pequeñas en el backlog y revisión semanal |
| R4 | Pérdida de datos de la base local | Baja | Alto | Scripts SQL versionados en Git y `pg_dump` periódico |
| R5 | Conflicto de puerto 5432 con Docker | Media | Bajo | Conexión por `127.0.0.1` documentada en el README |
| R6 | Alcance creciente (nuevos módulos) | Media | Medio | Todo cambio pasa por el backlog y se prioriza con el docente |

## 7. Herramientas

| Uso | Herramienta |
|---|---|
| Diagramas BPM, ERD y Gantt | FigJam |
| Prototipo UX/UI | Figma |
| Control de versiones | Git |
| Base de datos | PostgreSQL 16 + pgAdmin |
| Backend | Java 21, Spring Boot 3.4, Maven |
| Frontend | React 19 + Vite |
| Documentación | Markdown, Mermaid, Javadoc, Swagger/OpenAPI |

## 8. Seguimiento

- Reunión semanal de 30 minutos: qué se hizo, qué sigue, qué bloquea.
- El Gantt se actualiza marcando tareas como terminadas.
- Criterio de terminado de una tarea: código en Git, documentado y probado con las cuentas de [`docs/PRUEBAS.md`](../../docs/PRUEBAS.md).
