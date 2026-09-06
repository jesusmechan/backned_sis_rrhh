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
| GET | `/api/bandeja` | `tipo`, `q` |
| GET | `/api/asistencias` | `idEmpleado`, `desde`, `hasta`, `tipo`, `q` |
| GET | `/api/flujos` | `q` |
| GET | `/api/auditoria` | `q` |
| GET | `/api/trazabilidad` | `q` |

No se paginan catálogos, detalle por id, historial de una solicitud, ni reportes Excel/PDF/JSON.

Para combos (empleado, jefe) use `page=1&size=100`.

## Auth

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | No | Inicia sesión |
| POST | `/api/auth/refresh` | No | Renueva el access token |
| POST | `/api/auth/logout` | Sí | Revoca el refresh (o todos los del usuario) |

**Login**

```json
{ "nombreUsuario": "cmendoza", "password": "Andina2026" }
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
    "nombreUsuario": "cmendoza",
    "correo": "carlos.mendoza@andina.pe",
    "rol": "APROBADOR",
    "perfil": "Aprobador",
    "idRol": 3,
    "idEmpleado": 3,
    "nombreCompleto": "Carlos Alberto Mendoza Paredes",
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

Spring Security usa `ROLE_ADMIN`, `ROLE_RRHH`, `ROLE_APROBADOR` y `ROLE_EMPLEADO`.  
Los permisos funcionales del rol también viajan como authorities.

| Código | Uso |
|---|---|
| ADMIN | Usuarios, configuración, auditoría |
| RRHH | Personal, flujos, reportes, supervisión |
| APROBADOR | Bandeja y decisión de pasos |
| EMPLEADO | Solicitudes y marcaciones propias |

Contraseña de todos los usuarios seed: **Andina2026**.

| Usuario | Rol | Colaborador |
|---|---|---|
| `ediaz` | ADMIN | Elena Díaz Salazar |
| `mquispe` | RRHH | María Elena Quispe Rojas |
| `csilva` | RRHH | Carmen Silva Ortiz |
| `cmendoza` | APROBADOR | Carlos Mendoza (jefe Contabilidad) |
| `sparedes` | APROBADOR | Silvia Paredes (jefa Tributario) |
| `rsalas` | APROBADOR | Roberto Salas (Gerente General) |
| `atorres` | EMPLEADO | Ana Lucía Torres |
| `lvargas` | EMPLEADO | Luis Vargas |
| `pramos` | EMPLEADO | Patricia Ramos |
| `jhuaman` | EMPLEADO | Jorge Huamán |
| `msoto` | EMPLEADO | Miguel Soto |
| `rflores` | EMPLEADO | Rosa Flores |
| `dleon` | EMPLEADO | Diego León |
| `vchavez` | EMPLEADO | Valeria Chávez |
| `baguilar` | EMPLEADO | Bruno Aguilar |
| `lbenavides` | EMPLEADO | Lucía Benavides |
| `hpalomino` | EMPLEADO | Héctor Palomino |

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

Auth: autenticado. Un empleado ve las suyas; ADMIN / RRHH / APROBADOR ven el conjunto operativo.

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
| GET | `/api/bandeja` | Pasos `EN_CURSO` paginados (`tipo`, `q`) |
| POST | `/api/pasos/{id}/aprobar` | Aprobar el paso |
| POST | `/api/pasos/{id}/rechazar` | Rechazar (cierra la solicitud) |

```json
{ "comentario": "Conforme" }
```

El `id` es `idPasoSolicitud` de la bandeja, no el id de la solicitud. Los triggers avanzan o cierran el flujo.

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

`tipo`: `INGRESO` o `SALIDA`.

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
