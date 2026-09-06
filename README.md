# Sistema de Gestión de RR. HH. — Consultora Contable Andina S.A.C.

Proyecto del *Curso Integrador I — Sistemas Software* (UTP).  
Origen funcional: **Plan de Toma de Requerimientos v1.0**.

La consultora es una MYPE. El sistema cubre personal, permisos, horas extras, asistencia, usuarios, reportes y auditoría. El circuito de aprobación **no guarda al aprobador en la solicitud**: se configura por tipo de trámite y se instancia paso a paso.

## Stack

| Capa | Tecnología |
|---|---|
| Base de datos | PostgreSQL 16+ |
| API | Java 21, Spring Boot 3.4.5, Spring Security, JPA |
| Auth | JWT (access 15 min + refresh 7 días) |
| Exportes | Excel (Apache POI) y PDF (OpenPDF) |
| Frontend | React 19 + Vite (puerto 5173) |

## Estructura

```
Proyecto_RRHH/
├── README.md
├── docs/
│   ├── API.md
│   └── BASE_DE_DATOS.md
├── database/
│   ├── 00_create_database.sql
│   ├── 01_schema.sql
│   ├── 02_seed.sql
│   ├── 03_validar_flujo.sql
│   └── 04_refresh_token.sql
├── backend/                 # API Spring Boot (puerto 8080)
└── frontend/                # React + Vite (puerto 5173)
```

## Requisitos

- PostgreSQL 16 (pgAdmin)
- JDK 21
- Maven 3.9+
- Node.js 20+

## 1. Base de datos (pgAdmin)

1. Query Tool sobre la base **`postgres`**, con **Auto commit** activo.
2. Ejecutar `database/00_create_database.sql` (F5).
3. Refresh → Query Tool sobre **`rrhh_andina`**.
4. Ejecutar, en este orden:
   - `01_schema.sql`
   - `02_seed.sql`
   - `03_validar_flujo.sql` (opcional, pruebas del flujo)
5. Si la base ya existía **antes** de agregar tokens, ejecutar `04_refresh_token.sql`.

`01_schema.sql` recrea el esquema `public`. Se puede repetir: borra solo objetos de `rrhh_andina`.

Detalle: [docs/BASE_DE_DATOS.md](docs/BASE_DE_DATOS.md).

## 2. Backend

```bash
cd backend
copy .env.example .env    # o cp .env.example .env
```

Editar `backend/.env` con la conexión local:

```
DB_URL=jdbc:postgresql://127.0.0.1:5432/rrhh_andina
DB_USERNAME=postgres
DB_PASSWORD=root
```

Arrancar:

```bash
cd backend
mvn spring-boot:run
```

La API queda en `http://localhost:8080`.

Documentación interactiva (Swagger): [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
Especificación OpenAPI: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

`localhost` en Java puede resolver por IPv6 y chocar con un contenedor Docker en el puerto 5432. Por eso la URL usa `127.0.0.1` (PostgreSQL de Windows / pgAdmin).

## 3. Autenticación

`POST /api/auth/login`

```json
{ "nombreUsuario": "cmendoza", "password": "Andina2026" }
```

Respuesta: `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` y datos del usuario.

Las demás rutas llevan:

```
Authorization: Bearer <accessToken>
```

Usuarios de demostración (contraseña **Andina2026**):

| Usuario | Rol | Persona |
|---|---|---|
| `ediaz` | ADMIN | Elena Díaz |
| `mquispe` | RRHH | María Elena Quispe |
| `csilva` | RRHH | Carmen Silva |
| `cmendoza` | APROBADOR | Carlos Mendoza (jefe Contabilidad) |
| `sparedes` | APROBADOR | Silvia Paredes (jefa Tributario) |
| `rsalas` | APROBADOR | Roberto Salas (Gerencia) |
| `lbenavides` | EMPLEADO | Lucía Benavides |

Lista completa y catálogo de endpoints: [docs/API.md](docs/API.md).

## 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Abre http://localhost:5173. Vite proxea `/api` al backend en el puerto 8080. El menú cambia según el rol.

## Módulos

| Módulo | Qué cubre |
|---|---|
| Personal | Empleados, organigrama (`id_jefe_inmediato`), carga Excel |
| Permisos / horas extras | Registro, cancelación, historial |
| Aprobación | Flujos configurables + bandeja + aprobar/rechazar paso |
| Asistencia | Marcación de ingreso/salida y corrección |
| Usuarios | Roles ADMIN, RRHH, APROBADOR, EMPLEADO |
| Reportes | JSON, Excel y PDF |
| Auditoría | Bitácora y trazabilidad de solicitudes |

## Documentación

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- [Referencia escrita de la API](docs/API.md)
- [MER de la base de datos](docs/MER.md)
- [Base de datos](docs/BASE_DE_DATOS.md)
