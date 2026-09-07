# Cuentas y recorrido de prueba

Contraseña de **todos**: `Andina2026`

La base queda sin solicitudes, marcaciones ni historial. Se conservan menú, roles, horarios, tipos de permiso y la configuración de flujos.

Si la base ya tenía otras cuentas, ejecuta `database/02_reset.sql` sobre `rrhh_andina` (pgAdmin → F5).

## Cuentas

| Usuario | Rol | Persona | Código | Para qué |
|---|---|---|---|---|
| `jesus.mechan` | ADMIN | Jesús Mechan Gonzales | AND-001 | Acceso a todo el sistema. Cierra validación (paso de RR. HH.) y el 3.er paso de vacaciones. |
| `jesus.pantoja` | APROBADOR | Jesús Pantoja Pantoja | AND-002 | Jefe inmediato de Juan. Primer paso de casi todos los circuitos. |
| `juan.espinoza` | EMPLEADO | Juan Espinoza | AND-003 | Inicia permisos, horas extras y marcación. |

No hay cuenta con perfil RR. HH.: esos pasos quedan asignados al perfil ADMIN para poder cerrar el flujo con estas tres personas.

## Organigrama

```
Jesús Mechan (jesus.mechan)
└── Jesús Pantoja (jesus.pantoja) ── Juan Espinoza (juan.espinoza)
```

Juan tiene jefe inmediato = Jesús Pantoja. Si Pantoja pide un permiso, el primer paso llega a Mechan.

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
2. `jesus.pantoja` aprueba el paso 1 en **Bandeja**. La solicitud le queda en **En seguimiento** mientras Mechan atiende el paso 2.
3. La solicitud sigue **PENDIENTE**. El paso 2 queda **EN CURSO**.
4. Entra con `jesus.mechan`. En **Bandeja** aparece el paso de validación. Aprobar.
5. El flujo termina en **APROBADO**.

### Camino de 3 pasos — vacaciones

1. `juan.espinoza` registra **Vacaciones**.
2. `jesus.pantoja` aprueba el paso 1.
3. `jesus.mechan` aprueba el paso 2 (validación).
4. `jesus.mechan` aprueba el paso 3 (Autorización de Gerencia).
5. Estado final **APROBADO**.

### Comisión de servicios

1. `juan.espinoza` registra **Comisión de servicios**.
2. `jesus.pantoja` aprueba el paso 1.
3. El paso 2 es perfil **APROBADOR**: lo cierra `jesus.pantoja`.

## Marcación

- Entra con `juan.espinoza` o `jesus.pantoja` en **Marcar**.
- Sábado y domingo la marcación está deshabilitada (zona `America/Lima`).
- Un **INGRESO** y una **SALIDA** por día, origen WEB.
- `jesus.mechan` ve todo en **Asistencia** y **Personal**.

## Qué se conservó

- Áreas, cargos, horarios laborales, tipos de permiso, parámetros
- Roles, permisos funcionales y menú por perfil
- Circuitos en **Flujos** (`CFG-PERMISO-*` y `CFG-HEXTRA`)

No hay mantenedor de horarios en pantalla: se eligen al editar un empleado en **Personal**. Los dos horarios del catálogo siguen disponibles.
