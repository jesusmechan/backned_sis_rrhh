# Avance 2 – Curso Integrador I: Sistemas Software

**Unidad de aprendizaje 2: Diseño** · Sistema RRHH – Consultora Contable Andina S.A.C.

Relación entre cada tema del temario y su entregable.

| Sem. | Tema | Entregable en Figma | Documento |
|---|---|---|---|
| 5 | BPM: qué es, notación y uso | — | [01-bpm/BPM.md](01-bpm/BPM.md) §1–2 |
| 5 | Identificación de conceptos con ejemplos | [FigJam guía de notación BPMN](https://www.figma.com/board/ALb7EmLv7XOEh82juQEAxb) (cada símbolo con un caso del sistema) | [01-bpm/BPM.md](01-bpm/BPM.md) §3 |
| 5 | Taller: diagramas BPM del proyecto | [FigJam BPM](https://www.figma.com/board/PyidatOHCjPMOXfprTeVi9) (AS-IS y TO-BE de permisos, horas extras, planilla, reclutamiento) | [01-bpm/BPM.md](01-bpm/BPM.md) §4–6 |
| 6 | BD: mejores prácticas y errores comunes | — | [02-base-de-datos/DISENO_BD.md](02-base-de-datos/DISENO_BD.md) · [04_ejemplos_errores_comunes.sql](02-base-de-datos/04_ejemplos_errores_comunes.sql) · [05_indices_fk_propuestos.sql](02-base-de-datos/05_indices_fk_propuestos.sql) |
| 6 | BD: consideraciones de seguridad | — | [DISENO_BD.md](02-base-de-datos/DISENO_BD.md) · [03_seguridad_bd.sql](02-base-de-datos/03_seguridad_bd.sql) |
| 6 | Taller: diseño lógico | [FigJam ERD por módulos](https://www.figma.com/board/FsVw4RskUFFxzjJBByH5C9) | [DISENO_BD.md](02-base-de-datos/DISENO_BD.md) |
| 6 | Taller: diseño físico | [FigJam diseño físico](https://www.figma.com/board/ALb7EmLv7XOEh82juQEAxb) (BD F1 a F4: tipos PostgreSQL, PK, FK, UK, CHECK) | [`database/01_install.sql`](../database/01_install.sql) · [DICCIONARIO_DATOS.md](02-base-de-datos/DICCIONARIO_DATOS.md) |
| 7 | Identificación y mapeo de reportes clave | [FigJam mapa de reportes](https://www.figma.com/board/ALb7EmLv7XOEh82juQEAxb) (rol, reporte, vista, formato) · [pantalla 30 Reportes](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=26-2) | [03-ux-ui/UX_UI_Y_REPORTES.md](03-ux-ui/UX_UI_Y_REPORTES.md) |
| 7 | Prototipado UX/UI | [Prototipo navegable](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm) (31 pantallas del frontend real, por módulo y rol) | [UX_UI_Y_REPORTES.md](03-ux-ui/UX_UI_Y_REPORTES.md) |
| 7 | Documentación para desarrolladores (Javadoc, Markdown) | — | [04-documentacion/GUIA_DESARROLLADORES.md](04-documentacion/GUIA_DESARROLLADORES.md) · [javadoc/index.html](04-documentacion/javadoc/index.html) |
| 7 | Documentación para usuarios | — | [04-documentacion/MANUAL_USUARIO.md](04-documentacion/MANUAL_USUARIO.md) |
| 8 | Planificación del proyecto | [FigJam Gantt](https://www.figma.com/board/paAWBFnj1JSM0haMYUkahg) | [05-planificacion/PLANIFICACION.md](05-planificacion/PLANIFICACION.md) |

## Estructura

```
avance2-integrador1/
├── 01-bpm/BPM.md
├── 02-base-de-datos/
│   ├── DISENO_BD.md
│   ├── DICCIONARIO_DATOS.md          (generado)
│   ├── generar-diccionario.mjs       node generar-diccionario.mjs
│   ├── 03_seguridad_bd.sql           (propuesta, no ejecutada)
│   ├── 04_ejemplos_errores_comunes.sql (demostración, termina en ROLLBACK)
│   └── 05_indices_fk_propuestos.sql  (propuesta, no ejecutada)
├── 03-ux-ui/UX_UI_Y_REPORTES.md
├── 04-documentacion/
│   ├── GUIA_DESARROLLADORES.md
│   ├── MANUAL_USUARIO.md
│   └── javadoc/                      abrir index.html
└── 05-planificacion/PLANIFICACION.md
```

## Regenerar artefactos

```powershell
# Diccionario de datos (desde avance2-integrador1/02-base-de-datos)
node generar-diccionario.mjs

# Javadoc (desde backend/)
mvn -q -B org.apache.maven.plugins:maven-javadoc-plugin:3.11.2:javadoc "-Ddoclint=none" "-Dshow=protected" "-Dencoding=UTF-8" "-Ddocencoding=UTF-8" "-Dcharset=UTF-8"
# Resultado en backend/target/reports/apidocs; copiar a 04-documentacion/javadoc
```
