# Cuentas y recorrido de prueba

Contraseña de **todos**: `Andina2026`

La base queda sin solicitudes ni historial. Se conservan menú, roles, horarios, tipos de permiso y la configuración de flujos. Cada cuenta vuelve a tener marcaciones de lunes a viernes desde el **2026-08-03** hasta hoy (ingreso y salida según su jornada; sin sábados ni domingo).

Si la base ya tenía otras cuentas, ejecuta `database/02_reset.sql` sobre `rrhh_andina` (pgAdmin → F5). Al levantar la API, `schema.sql` también crea los roles `JEFE` / `GERENCIA` y la cuenta `carla.reyes` si faltan.

## Cuentas

| Usuario | Rol | Persona | Código | Para qué |
|---|---|---|---|---|
| `juan.espinoza` | EMPLEADO | Juan Espinoza | AND-003 | Inicia permisos, horas extras y marcación. |
| `jesus.pantoja` | JEFE | Jesús Pantoja Pantoja | AND-002 | Jefe inmediato de Juan. Primer paso de casi todos los circuitos. |
| `carla.reyes` | RRHH | Carla Reyes Huamán | AND-004 | Segundo paso de salud, duelo, capacitación, vacaciones y horas extras. |
| `jesus.mechan` | ADMIN | Jesús Mechan Gonzales | AND-001 | Sistema. En la demo también cierra los pasos de **Gerencia** (es el Gerente General). |

El primer paso **no** usa un rol “Jefe inmediato”: se resuelve con `empleado.id_jefe_inmediato`.

## Organigrama

```
Jesús Mechan (jesus.mechan)
├── Jesús Pantoja (jesus.pantoja) ── Juan Espinoza (juan.espinoza)
└── Carla Reyes (carla.reyes)
```

Si Pantoja pide un permiso, el primer paso llega a Mechan. Si Carla pide un permiso, también.

Horarios:

- Mechan: jornada gerencial 09:00–18:30
- Los demás: jornada administrativa 09:00–18:00

## Cómo recorrer un flujo completo

Frontend: http://localhost:5173  
Cierra sesión al cambiar de usuario (icono de salir).

### Camino corto (1 paso) — permiso particular

1. Entra con `juan.espinoza`.
2. **Permisos → Nueva solicitud**. Tipo **Permiso particular**, fechas de un día hábil, motivo de al menos 5 caracteres. Registrar.
3. En el detalle debe verse el circuito con el paso 1 **EN CURSO** asignado al jefe.
4. Cierra sesión. Entra con `jesus.pantoja`.
5. **Bandeja → Revisar** esa solicitud. Aprobar (o rechazar; el rechazo cierra el flujo).
6. Tras aprobar, el ítem sale de **Por atender** y queda en **En seguimiento** (solo lectura). Juan también la ve ahí desde **Bandeja**.
7. Vuelve a entrar con `juan.espinoza` y abre el detalle: estado **APROBADO** (o **RECHAZADO**).

### Camino de 2 pasos — permiso por salud u horas extras

1. `juan.espinoza` registra **Permiso por salud** o **Horas extras**.
2. `jesus.pantoja` aprueba el paso 1 en **Bandeja**.
3. La solicitud sigue **PENDIENTE**. El paso 2 queda **EN CURSO** para el perfil **RRHH**.
4. Entra con `carla.reyes`. En **Bandeja** aparece el paso de validación. Aprobar.
5. El flujo termina en **APROBADO**.

### Camino de 3 pasos — vacaciones

1. `juan.espinoza` registra **Vacaciones**.
2. `jesus.pantoja` aprueba el paso 1.
3. `carla.reyes` aprueba el paso 2 (RR. HH.).
4. `jesus.mechan` aprueba el paso 3 (Gerencia).
5. Estado final **APROBADO**.

### Comisión de servicios

1. `juan.espinoza` registra **Comisión de servicios**.
2. `jesus.pantoja` aprueba el paso 1.
3. El paso 2 es perfil **GERENCIA**: lo cierra `jesus.mechan`.

## Marcación

- El seed deja historial hábil desde el **3 de agosto de 2026** hasta la fecha en que se ejecutó el script (hora Lima).
- Entra con `juan.espinoza` o `jesus.pantoja` en **Marcar** para el día de hoy si aún no hay salida.
- Sábado y domingo la marcación está deshabilitada (zona `America/Lima`).
- Un **INGRESO** y una **SALIDA** por día, origen WEB.
- `jesus.mechan` y `carla.reyes` ven el conjunto en **Asistencia** y **Personal**.

## Qué se conservó

- Áreas, cargos, horarios laborales, tipos de permiso, parámetros
- Roles, permisos funcionales y menú por perfil
- Circuitos en **Flujos** (`CFG-PERMISO-*` y `CFG-HEXTRA`)

No hay mantenedor de horarios en pantalla: se eligen al editar un empleado en **Personal**. Los dos horarios del catálogo siguen disponibles.
