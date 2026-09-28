# Manual de usuario — Sistema RRHH Andina

Dirigido al personal de la Consultora Contable Andina S.A.C. Las pantallas de referencia están en el [prototipo de Figma](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm).

## Contenido

1. [Ingreso al sistema](#1-ingreso-al-sistema)
2. [Colaborador (EMPLEADO)](#2-colaborador-empleado)
3. [Jefe inmediato (JEFE)](#3-jefe-inmediato-jefe)
4. [Recursos Humanos (RRHH)](#4-recursos-humanos-rrhh)
5. [Gerencia (GERENCIA)](#5-gerencia-gerencia)
6. [Administrador (ADMIN)](#6-administrador-admin)
7. [Estados de una solicitud](#7-estados-de-una-solicitud)
8. [Mensajes frecuentes y qué hacer](#8-mensajes-frecuentes-y-qué-hacer)

---

## 1. Ingreso al sistema

1. Abrir el navegador en la dirección que indique TI (en pruebas: `http://localhost:5173`).
2. Escribir el **usuario** corporativo (por ejemplo `juan.espinoza`) y la **contraseña**.
3. Pulsar **Ingresar**.
4. El menú lateral muestra solo las opciones de tu perfil.
5. Para salir, pulsar tu nombre en la parte inferior del menú → **Cerrar sesión**.

> Por seguridad, la sesión se renueva sola mientras la usas y se cierra si no hay actividad. Nunca compartas tu contraseña.

Pantallas: [01 Login](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=28-2) · [02 Inicio del administrador](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=2-2) · [03 Inicio del colaborador](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=31-2) · [04 Acceso restringido](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=32-2) (lo que ves si entras a una opción que tu perfil no tiene).

---

## 2. Colaborador (EMPLEADO)

### 2.1 Marcar ingreso o salida

1. Menú **Marcar** (funciona también desde el celular).
2. Pulsar **Marcar INGRESO** al llegar y **Marcar SALIDA** al retirarte.
3. Se permite un ingreso y una salida por día; sábados y domingos está deshabilitado.
4. Si olvidaste marcar, pide a RRHH una **corrección** (queda registrada con origen CORRECCIÓN).

Pantallas: [13 Marcar](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=12-2) · [14 Marcar en celular](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=33-2)

### 2.2 Pedir un permiso

1. Menú **Permisos** → **Nueva solicitud**.
2. Elegir el **tipo** (particular, salud, vacaciones, capacitación, comisión, duelo).
3. Indicar **fecha de inicio** y **fecha de fin**.
4. Para un permiso por horas, llenar **hora de inicio y hora de fin**; para días completos, dejar ambas vacías.
5. Escribir el **motivo** (mínimo 5 caracteres) y pulsar **Registrar solicitud**.
6. En el detalle verás el circuito: quién aprueba cada paso y cuál está **EN CURSO**.

En **vacaciones** el sistema revisa tu saldo (se acumulan 1.5 días por mes trabajado).

Pantallas: [08 Permisos](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=5-2) · [09 Nuevo permiso](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=6-2) · [10 Detalle de permiso](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=7-2)

### 2.3 Registrar horas extras

1. Menú **Horas extras** → **Nueva solicitud**.
2. Indicar fecha, hora de inicio, hora de fin, cantidad de horas y motivo.
3. Topes: 4 horas por día y 12 por semana (lunes a domingo). Las horas aprobadas se pagan en la planilla del mes.

Pantallas: [11 Horas extras](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=8-2) · [12 Nueva hora extra](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=9-2)

### 2.4 Seguir o cancelar una solicitud

- **Bandeja → En seguimiento** muestra tus solicitudes y en qué paso están.
- Mientras esté **PENDIENTE** puedes **Cancelar** desde el detalle. Una solicitud aprobada o rechazada ya no se cancela.
- La campana de notificaciones avisa cuando tu solicitud se aprueba o se rechaza.

Pantalla: [07 Bandeja](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=4-2)

### 2.5 Consultar asistencia y perfil

- **Asistencia**: historial de marcaciones.
- **Mi perfil**: tus datos personales y de la cuenta.

Pantallas: [15 Asistencia](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=10-2) · [16 Mi perfil](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=11-2)

---

## 3. Jefe inmediato (JEFE)

Además de todo lo del colaborador:

### 3.1 Aprobar o rechazar solicitudes de tu equipo

1. Menú **Bandeja → Por atender**. La campana indica cuántas hay.
2. Pulsar **Revisar** en la solicitud.
3. Leer el detalle y el historial del circuito.
4. Escribir un **comentario** (obligatorio al rechazar, mínimo 3 caracteres).
5. Pulsar **Aprobar** o **Rechazar** (el rechazo pide confirmación).
   - Aprobar: la solicitud pasa al siguiente aprobador (RRHH o Gerencia) o queda **APROBADO** si eras el último.
   - Rechazar: la solicitud termina como **RECHAZADO** y los pasos siguientes se omiten.
6. Solo puedes decidir el paso que está **EN CURSO** y que te corresponde.
7. Para varias solicitudes a la vez, márcalas en la lista y usa **Aprobar** o **Rechazar** de la barra superior (aprobación masiva).

Pantallas: [05 Bandeja del jefe](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=29-2) · [06 Revisar solicitud](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=30-2)

### 3.2 Evaluar desempeño

Menú **Desempeño** → elegir al colaborador → calificar de 1 a 5 **puntualidad, calidad, cooperación e iniciativa** → Guardar. El promedio se calcula solo.

Pantalla: [28 Desempeño](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=24-2)

---

## 4. Recursos Humanos (RRHH)

Además de lo anterior:

| Tarea | Dónde | Pasos clave | Pantalla |
|---|---|---|---|
| Validar trámites | **Bandeja** | Atender el paso de RRHH (salud, duelo, capacitación, vacaciones, horas extras) | [07](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=4-2) |
| Registrar un colaborador | **Personal** → Nuevo | Datos personales, área, cargo, horario y **jefe inmediato** | [17](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=13-2) |
| Carga masiva | **Personal** → Cargar Excel | Subir la plantilla; revisar el resultado por fila (insertado, actualizado, error) | [17](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=13-2) |
| Registrar contrato | **Contratos** → Nuevo | Modalidad, fechas, remuneración básica, régimen ONP/AFP, asignación familiar | [18](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=14-2) |
| Catálogos | **Maestros** | Áreas, cargos, tipos de permiso, parámetros (tasas, topes), plan de cuentas | [19](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=15-2) |
| Circuitos de aprobación | **Flujos** | Pasos por tipo de trámite: jefe inmediato, un rol o un usuario | [23](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=19-2) · [24](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=20-2) |
| Reclutamiento | **Reclutamiento** | Crear convocatoria, registrar postulantes y moverlos: Postulado → Entrevista → Seleccionado → Contratado / Descartado | [29](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=25-2) |
| Reportes | **Reportes** | Elegir el reporte y exportar a Excel o PDF | [30](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=26-2) |
| Auditoría | **Auditoría** | Buscar quién hizo qué y cuándo | [31](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=27-2) |

### 4.1 Planilla mensual

1. **Planillas → Nueva**: elegir año y mes (solo una planilla por periodo).
2. Pulsar **Calcular**. El sistema toma a los colaboradores activos con contrato vigente y calcula: básico, asignación familiar, horas extras aprobadas, descuentos por ausencias, ONP o AFP y EsSalud.
3. Revisar las boletas. Si algo no cuadra, corregir el contrato o las marcaciones y **Recalcular**.
4. **Exportar boletas PDF** (todas o una por colaborador).
5. Pulsar **Cerrar planilla**. Se genera el asiento contable y la planilla ya no se puede recalcular.
6. El asiento se consulta en **Contabilidad**.

Pantallas: [25 Planillas](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=21-2) · [26 Detalle de planilla](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=22-2) · [27 Contabilidad](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=23-2)

---

## 5. Gerencia (GERENCIA)

- Aprueba el último paso de **vacaciones** y el segundo de **comisión de servicios** desde **Bandeja**.
- Consulta **Personal**, **Desempeño** y **Reportes**.

---

## 6. Administrador (ADMIN)

| Tarea | Dónde | Pantalla |
|---|---|---|
| Crear cuentas y asignar perfil | **Usuarios** | [20](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=16-2) |
| Crear o editar perfiles y sus permisos | **Roles** | [21](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=17-2) |
| Definir qué opciones de menú ve cada perfil | **Menú** | [22](https://www.figma.com/design/e8HCwGg9xywFm98J5vyEGm?node-id=18-2) |
| Todo lo de RRHH | Mismo menú | |

Buenas prácticas: una cuenta por persona, desactivar (no borrar) las cuentas de quien cesa, y revisar la **Auditoría** periódicamente.

---

## 7. Estados de una solicitud

| Estado | Significado |
|---|---|
| PENDIENTE | Registrada; su circuito está en marcha |
| APROBADO | Todos los pasos fueron aprobados |
| RECHAZADO | Un aprobador la rechazó |
| CANCELADO | El solicitante (o RRHH) la anuló mientras estaba pendiente |

Cada **paso** del circuito puede estar: PENDIENTE (aún no le toca), EN CURSO (esperando decisión), APROBADO, RECHAZADO, OMITIDO (se saltó por un rechazo anterior) o CANCELADO.

---

## 8. Mensajes frecuentes y qué hacer

| Mensaje | Causa | Qué hacer |
|---|---|---|
| "Indique hora de inicio y de fin, o deje ambas vacías." | Llenaste solo una hora | Completa ambas o bórralas para día completo |
| "La hora de fin debe ser posterior a la de inicio…" | Horario invertido | Corrige las horas |
| "Ya existe un permiso pendiente o aprobado que se cruza con esas fechas." | Traslapo con otro permiso | Cambia las fechas o cancela el permiso anterior |
| "La cantidad de horas extras del día supera el máximo permitido (4)." | Superaste el tope diario | Reduce las horas |
| "La cantidad de horas extras de la semana supera el máximo permitido (12)." | Superaste el tope semanal | Reduce las horas o regístralas otra semana |
| "Indique el motivo del rechazo." | Rechazaste sin comentario | Escribe al menos 3 caracteres |
| "Este paso no le corresponde" | Intentaste decidir un paso de otro aprobador | Revisa en el historial a quién le toca |
| "Ya existe una planilla para …" | El periodo ya fue creado | Abre la planilla existente |
| "Solo se cierra una planilla calculada" | Intentaste cerrar sin calcular | Pulsa **Calcular** primero |
| "Calcule la planilla antes de exportar las boletas" | Planilla en borrador | Calcula y luego exporta |

Soporte: comunicarse con el área de RRHH o con el administrador del sistema.
