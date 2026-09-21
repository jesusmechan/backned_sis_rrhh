# API REST — rrhh-api

Base: `http://localhost:8080`

Todas las rutas (salvo login y refresh) requieren:

```
Authorization: Bearer <accessToken>
```

CORS permitido por defecto: `http://localhost:4200`, `5173` y `3000`.

## Paginación

Los listados operativos responden con este envoltorio (página 1-based, `size` por defecto 10, máximo 100):

```json
{
  "content": [],
  "page": 1,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

Query comunes: `page`, `size`, `q` (búsqueda de texto).  
`estado` en permisos y horas extras admite uno o varios valores separados por coma (`PENDIENTE`, `APROBADO,RECHAZADO`).

| Método | Ruta | Extra |
|---|---|---|
| GET | `/api/empleados` | `q` |
| GET | `/api/usuarios` | `q` |
| GET | `/api/permisos` | `estado`, `q` |
| GET | `/api/horas-extras` | `estado`, `q` |
| GET | `/api/bandeja` | `tipo`, `q`, `vista=SEGUIMIENTO`, `estado` |
| GET | `/api/asistencias` | `idEmpleado`, `desde`, `hasta`, `tipo`, `q` |
| GET | `/api/flujos` | `q` |
| GET | `/api/auditoria` | `q` |
| GET | `/api/trazabilidad` | `q` |
| GET | `/api/planillas` | `estado`, `q` |
| GET | `/api/asientos` | `estado`, `q` |
| GET | `/api/evaluaciones` | `q` |
| GET | `/api/convocatorias` | `estado`, `q` |
| GET | `/api/maestros/areas` | `q`, `activo` |

No se paginan catálogos, detalle por id, historial de una solicitud, notificaciones ni reportes Excel/PDF/JSON.

Para combos (empleado, jefe) use `page=1&size=100`.

## Auth

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | No | Inicia sesión |
| POST | `/api/auth/refresh` | No | Renueva el access token |
| POST | `/api/auth/logout` | Sí | Revoca el refresh (o todos los del usuario) |

**Login**

```json
{ "nombreUsuario": "jesus.mechan", "password": "Andina2026" }
```

**Respuesta**

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "usuario": {
    "idUsuario": 3,
    "nombreUsuario": "jesus.mechan",
    "correo": "jesus.mechan@andina.pe",
    "rol": "ADMIN",
    "perfil": "Administrador",
    "idRol": 1,
    "idEmpleado": 1,
    "nombreCompleto": "Jesús Mechan Gonzales",
    "permisos": ["PERMISO_APROBAR", "ASISTENCIA_MARCAR"],
    "menu": [
      { "grupo": "Operación", "items": [{ "codigo": "BANDEJA", "etiqueta": "Bandeja", "ruta": "/bandeja", "icono": "Inbox" }] }
    ]
  }
}
```

- Access token: 15 minutos.
- Refresh token: 7 días, persistido en `refresh_token`.

**Refresh**

```json
{ "refreshToken": "..." }
```

**Logout:** enviar el mismo cuerpo con `refreshToken`, o solo el Bearer para revocar todas las sesiones del usuario.

## Roles

Los perfiles están en la tabla `rol` (mantenedor `GET/POST/PUT /api/roles`). El código del rol se publica como `ROLE_{codigo}` y los permisos funcionales de `rol_permiso` viajan como authorities. El semilla incluye **ADMIN**, **GERENCIA**, **RRHH**, **JEFE** y **EMPLEADO**.

El primer paso de cada permiso y de las horas extras es tipo `JEFE_INMEDIATO` (organigrama), no un rol.

Contraseña de todos: **Andina2026**. Recorrido: [PRUEBAS.md](PRUEBAS.md).

| Usuario | Rol | Colaborador |
|---|---|---|
| `jesus.mechan` | ADMIN | Jesús Mechan Gonzales (Gerente General; en la demo cierra los pasos de Gerencia) |
| `carla.reyes` | RRHH | Carla Reyes Huamán |
| `jesus.pantoja` | JEFE | Jesús Pantoja Pantoja |
| `juan.espinoza` | EMPLEADO | Juan Espinoza |

## Catálogos

Auth: cualquier usuario autenticado.

| Método | Ruta |
|---|---|
| GET | `/api/catalogos/areas` |
| GET | `/api/catalogos/cargos` |
| GET | `/api/catalogos/horarios` |
| GET | `/api/catalogos/tipos-permiso` |
| GET | `/api/catalogos/roles` |
| GET | `/api/catalogos/permisos-funcionales` |
| GET | `/api/catalogos/parametros` |

## Maestros

Auth: **ADMIN** o **RRHH**. Alta y edición de catálogos. No hay borrado físico: se desactiva el registro para no romper las fichas que ya lo usan. `VACACIONES` no puede desactivarse ni cambiar de código.

| Método | Ruta | Descripción |
|---|---|---|
| GET/POST | `/api/maestros/areas` | Áreas |
| PUT | `/api/maestros/areas/{id}` | Actualizar área |
| GET/POST | `/api/maestros/cargos` | Cargos |
| PUT | `/api/maestros/cargos/{id}` | Actualizar cargo |
| GET/POST | `/api/maestros/horarios` | Horarios laborales |
| PUT | `/api/maestros/horarios/{id}` | Actualizar horario |
| GET/POST | `/api/maestros/tipos-permiso` | Tipos de permiso |
| PUT | `/api/maestros/tipos-permiso/{id}` | Actualizar tipo |
| GET/POST | `/api/maestros/parametros` | Parámetros (`tasa_onp`, topes de HE, etc.) |
| PUT | `/api/maestros/parametros/{clave}` | Cambiar valor |
| GET/POST | `/api/maestros/cuentas` | Plan de cuentas usado al cerrar planilla |
| PUT | `/api/maestros/cuentas/{id}` | Actualizar cuenta |

Query: `page`, `size`, `q`, `activo` (excepto parámetros).

**Área / cargo**

```json
{ "nombre": "Auditoría", "descripcion": "Control interno", "activo": true }
```

**Horario**

```json
{ "nombre": "Turno tarde", "horaIngreso": "14:00", "horaSalida": "22:00", "minutosRefrigerio": 45, "activo": true }
```

El cierre de planilla toma código y nombre de la cuenta activa según `uso` (`SUELDOS`, `ESSALUD_GASTO`, `ONP_POR_PAGAR`, `ESSALUD_POR_PAGAR`, `REMU_POR_PAGAR`, `DESC_AUSENCIAS`). Si la cuenta está inactiva se usan los códigos PCGE de respaldo.

## Personal

Auth: **ADMIN** o **RRHH**.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/empleados` | Listar paginado (`page`, `size`, `q`) |
| GET | `/api/empleados/{id}` | Detalle |
| POST | `/api/empleados` | Crear |
| PUT | `/api/empleados/{id}` | Actualizar |
| GET | `/api/empleados/plantilla-excel` | Descarga plantilla `.xlsx` |
| POST | `/api/empleados/carga-excel` | Carga masiva (`multipart/form-data`, campo `archivo`) |

**Crear / actualizar**

```json
{
  "codigoEmpleado": "AND-018",
  "tipoDocumento": "DNI",
  "numeroDocumento": "12345678",
  "nombres": "Ana",
  "apellidoPaterno": "Pérez",
  "apellidoMaterno": "Luna",
  "fechaNacimiento": "1995-04-10",
  "sexo": "F",
  "correoInstitucional": "ana.perez@andina.pe",
  "correoPersonal": null,
  "telefono": "999111099",
  "direccion": "Lima",
  "fechaIngreso": "2026-09-01",
  "fechaCese": null,
  "idArea": 3,
  "idCargo": 5,
  "idHorario": 1,
  "tipoContrato": "PLANILLA",
  "estado": "ACTIVO",
  "idJefeInmediato": 3
}
```

Enums: `tipoDocumento` `DNI|CE|PASAPORTE`, `sexo` `M|F`, `tipoContrato` `PLANILLA|RECIBO_HONORARIOS|PRACTICAS`, `estado` `ACTIVO|INACTIVO|CESADO`.

## Sesión y menú

El perfil de la cuenta es el `rol`. El menú no es estático: se lee de `menu_item` + `menu_rol`.

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/api/sesion` | Sí | Perfil, permisos funcionales y menú del usuario |
| GET | `/api/menu` | Sí | Solo los grupos de menú del perfil |
| GET | `/api/menus` | ADMIN | Mantenedor paginado |
| GET | `/api/menus/{id}` | ADMIN | Detalle |
| POST | `/api/menus` | ADMIN | Crear opción y asociarla a perfiles |
| PUT | `/api/menus/{id}` | ADMIN | Actualizar opción y perfiles |
| DELETE | `/api/menus/{id}` | ADMIN | Eliminar opción |
| GET | `/api/roles` | ADMIN | Mantenedor paginado de roles |
| GET | `/api/roles/{id}` | ADMIN | Detalle con menús y permisos |
| POST | `/api/roles` | ADMIN | Crear rol |
| PUT | `/api/roles/{id}` | ADMIN | Actualizar rol, menús y permisos |

## Usuarios

| Método | Ruta | Roles |
|---|---|---|
| GET | `/api/usuarios/me` | Autenticado |
| GET | `/api/usuarios` | ADMIN, RRHH · paginado |
| GET | `/api/usuarios/{id}` | ADMIN, RRHH |
| POST | `/api/usuarios` | ADMIN |
| PUT | `/api/usuarios/{id}` | ADMIN |
| PATCH | `/api/usuarios/{id}/estado?activo=true` | ADMIN |

```json
{
  "idEmpleado": 4,
  "idRol": 4,
  "nombreUsuario": "atorres",
  "correo": "ana.torres@andina.pe",
  "password": "Andina2026",
  "activo": true
}
```

En actualización, `password` es opcional.

## Permisos

Auth: autenticado. Un empleado ve las suyas; ADMIN, RRHH, GERENCIA y JEFE ven el conjunto operativo.

| Método | Ruta | Notas |
|---|---|---|
| GET | `/api/permisos` | Paginado (`page`, `size`, `estado`, `q`) |
| GET | `/api/permisos/{id}` | Detalle |
| POST | `/api/permisos` | Crear |
| POST | `/api/permisos/{id}/cancelar` | Cancelar |
| GET | `/api/permisos/{id}/historial` | Historial |

```json
{
  "idEmpleado": 16,
  "idTipoPermiso": 2,
  "fechaInicio": "2026-09-10",
  "fechaFin": "2026-09-10",
  "horaInicio": "09:00:00",
  "horaFin": "13:00:00",
  "motivo": "Cita médica"
}
```

`idEmpleado` es opcional para EMPLEADO (usa el de la sesión). Al crear, un trigger de PostgreSQL instancia los pasos del flujo.

## Horas extras

Misma convención que permisos.

| Método | Ruta |
|---|---|
| GET | `/api/horas-extras` (`page`, `size`, `estado`, `q`) |
| GET | `/api/horas-extras/{id}` |
| POST | `/api/horas-extras` |
| POST | `/api/horas-extras/{id}/cancelar` |
| GET | `/api/horas-extras/{id}/historial` |

```json
{
  "idEmpleado": 16,
  "fecha": "2026-09-08",
  "horaInicio": "18:00:00",
  "horaFin": "20:00:00",
  "cantidadHoras": 2.0,
  "motivo": "Cierre de planillas"
}
```

## Aprobación

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/bandeja` | Por defecto pasos `EN_CURSO` asignados. `vista=SEGUIMIENTO` lista solicitudes creadas o ya decididas por el usuario (`tipo`, `q`, `estado`) |
| POST | `/api/pasos/{id}/aprobar` | Aprobar el paso |
| POST | `/api/pasos/{id}/rechazar` | Rechazar (cierra la solicitud) |

```json
{ "comentario": "Conforme" }
```

El `id` es `idPasoSolicitud` de la bandeja, no el id de la solicitud. Los triggers avanzan o cierran el flujo.

## Notificaciones

Auth: cualquier usuario autenticado. El listado no se pagina (máximo 40). El push en vivo usa **STOMP sobre SockJS** en `/ws`.

El cliente se suscribe a `/user/queue/notificaciones` con el access token (`Authorization: Bearer` en CONNECT y/o `?access_token=`).

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/notificaciones` | Últimas notificaciones del usuario |
| GET | `/api/notificaciones/no-leidas` | `{ "noLeidas": 3 }` |
| POST | `/api/notificaciones/{id}/leer` | Marca una como leída |
| POST | `/api/notificaciones/leer-todas` | Marca todas |

Evento STOMP:

```json
{
  "notificacion": {
    "idNotificacion": 12,
    "tipo": "BANDEJA",
    "titulo": "Tiene un paso por aprobar",
    "mensaje": "Juan Espinoza registró Permiso por salud. Le corresponde el paso «Jefe inmediato».",
    "ruta": "/bandeja/41",
    "tipoSolicitud": "PERMISO",
    "idSolicitud": 8,
    "idPaso": 41,
    "leida": false,
    "fechaCreacion": "2026-09-21T11:20:00-05:00"
  },
  "noLeidas": 1
}
```

`tipo`: `BANDEJA` (siguiente aprobador), `APROBADA` o `RECHAZADA` (solicitante).

## Asistencia

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/asistencias/marcar` | Registrar ingreso o salida |
| GET | `/api/asistencias` | Listar paginado. Query: `idEmpleado`, `desde`, `hasta`, `tipo`, `q` |
| GET | `/api/asistencias/{id}` | Detalle |
| PUT | `/api/asistencias/{id}` | Corregir marcación |

```json
{
  "idEmpleado": 16,
  "tipo": "INGRESO",
  "fechaHora": "2026-09-05T08:55:00-05:00",
  "origen": "WEB",
  "observacion": null
}
```

`tipo`: `INGRESO` o `SALIDA`. Origen `WEB` no admite sábado ni domingo (zona `America/Lima`).

## Flujos de aprobación

Auth: **ADMIN** o **RRHH**.

| Método | Ruta |
|---|---|
| GET | `/api/flujos` (`page`, `size`, `q`) |
| GET | `/api/flujos/{id}` |
| POST | `/api/flujos` |
| PUT | `/api/flujos/{id}` |

`tipoOrigen`: `PERMISO` o `HORA_EXTRA`.  
`tipoAprobador`: `JEFE_INMEDIATO`, `ROL` o `USUARIO`.

```json
{
  "codigo": "CFG-PERMISO-SALUD",
  "nombre": "Permiso por salud",
  "tipoOrigen": "PERMISO",
  "idTipoPermiso": 2,
  "descripcion": "Jefe inmediato y RR. HH.",
  "activo": true,
  "pasos": [
    {
      "numeroPaso": 1,
      "nombrePaso": "Jefe inmediato",
      "tipoAprobador": "JEFE_INMEDIATO",
      "idRol": null,
      "idUsuario": null,
      "esObligatorio": true
    },
    {
      "numeroPaso": 2,
      "nombrePaso": "Validación de RR. HH.",
      "tipoAprobador": "ROL",
      "idRol": 2,
      "idUsuario": null,
      "esObligatorio": true
    }
  ]
}
```

## Reportes

Auth: **ADMIN** o **RRHH**.

| Método | Ruta | Formato |
|---|---|---|
| GET | `/api/reportes/trabajadores` | JSON |
| GET | `/api/reportes/permisos` | JSON |
| GET | `/api/reportes/horas-extras` | JSON |
| GET | `/api/reportes/asistencia` | JSON |
| GET | `/api/reportes/usuarios` | JSON |
| GET | `/api/reportes/{tipo}/excel` | `.xlsx` |
| GET | `/api/reportes/{tipo}/pdf` | `.pdf` |

`tipo`: `TRABAJADORES`, `PERMISOS`, `HORAS_EXTRAS`, `ASISTENCIA`, `USUARIOS`.

## Auditoría

Auth: **ADMIN** o **RRHH**.

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/auditoria` | Bitácora paginada |
| GET | `/api/trazabilidad` | Historial paginado de solicitudes |

## Errores

Cuerpo típico:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del problema"
}
```

| HTTP | Cuándo |
|---|---|
| 400 | Validación o regla de negocio |
| 401 | Sin token o token inválido |
| 403 | Rol insuficiente |
| 404 | Recurso inexistente |

## Variables de entorno

Archivo `backend/.env` (se carga al arrancar):

| Variable | Uso |
|---|---|
| `DB_URL` | JDBC de PostgreSQL |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña |
| `JWT_SECRET` | Firma de los tokens |
| `CORS_ORIGINS` | Orígenes permitidos, separados por coma |

## Planillas y remuneraciones

El cálculo usa la remuneración básica del contrato vigente, las horas extras **aprobadas** del mes (`valor hora = básico / 240 × 1.25`) y los permisos aprobados que no son vacaciones (descuento a `básico / 30` por día). Los colaboradores tienen ONP (13 %) y EsSalud (9 %); los practicantes solo subvención. Al cerrar se genera el asiento con las cuentas del maestro (`uso` SUELDOS, ESSALUD_GASTO, ONP_POR_PAGAR, ESSALUD_POR_PAGAR, REMU_POR_PAGAR, DESC_AUSENCIAS; respaldo 6211 / 6271 / 4031 / 4032 / 4111 / 4699).

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/api/planillas` | ADMIN, RRHH | Lista periodos |
| POST | `/api/planillas` | ADMIN, RRHH | Abre periodo `anio` + `mes` |
| GET | `/api/planillas/{id}` | ADMIN, RRHH | Boletas del periodo |
| GET | `/api/planillas/{id}/boletas/pdf` | ADMIN, RRHH | PDF con una página por trabajador |
| GET | `/api/planillas/{id}/boletas/{idDetalle}/pdf` | ADMIN, RRHH | PDF de una boleta |
| POST | `/api/planillas/{id}/calcular` | ADMIN, RRHH | Recalcula boletas |
| POST | `/api/planillas/{id}/cerrar` | ADMIN, RRHH | Cierra y contabiliza |
| GET | `/api/asientos` | ADMIN, RRHH | Asientos de planilla |
| GET | `/api/asientos/{id}` | ADMIN, RRHH | Detalle con líneas |
| GET | `/api/evaluaciones` | Autenticado | Lista (el empleado solo ve las suyas) |
| POST | `/api/evaluaciones` | RRHH, ADMIN, JEFE, GERENCIA | Registrar evaluación 1–5 |
| GET | `/api/convocatorias` | ADMIN, RRHH | Convocatorias |
| POST | `/api/convocatorias` | ADMIN, RRHH | Publicar |
| PUT | `/api/convocatorias/{id}` | ADMIN, RRHH | Actualizar |
| GET | `/api/convocatorias/{id}/postulaciones` | ADMIN, RRHH | Postulantes |
| POST | `/api/convocatorias/{id}/postulaciones` | ADMIN, RRHH | Registrar postulante |
| PUT | `/api/convocatorias/postulaciones/{id}` | ADMIN, RRHH | Cambiar estado del postulante |

