# Instrucciones de Migración: Cálculo Dinámico de Planilla

## Resumen de Cambios

Este PR implementa el cálculo dinámico de planilla según los regímenes laborales vigentes en Perú (Ley 32353 MYPE y régimen general D. Leg. 728). Todos los valores están verificados contra fuentes oficiales (SUNAT, SBS, MTPE) para 2026.

## SQL a Ejecutar Manualmente

### Pre-requisito
- PostgreSQL 16+
- Base de datos `rrhh_andina` ya instalada con `01_install.sql` y `03_upgrade_planilla_peru.sql`
- pgAdmin con conexión configurada

### Procedimiento

1. **Abrir pgAdmin**
2. **Conectar a la base de datos `rrhh_andina`**
3. **Query Tool** (botón derecho → Query Tool)
4. **Verificar Auto commit está activo** (menú → Auto commit)
5. **Abrir archivo:** `database/05_upgrade_regimen_laboral_dinamico.sql`
6. **Ejecutar** (F5)
7. **Verificar resultado:** Debe completarse sin errores

### Qué hace el script

**Cambios de esquema (idempotente):**
```sql
-- Crea enum regimen_laboral (GENERAL, MYPE_MICRO, MYPE_PEQUENA)
CREATE TYPE regimen_laboral AS ENUM ('GENERAL', 'MYPE_MICRO', 'MYPE_PEQUENA');

-- Agrega columna a contrato
ALTER TABLE contrato
    ADD COLUMN regimen_laboral regimen_laboral NOT NULL DEFAULT 'GENERAL';

-- Agrega columnas a planilla_detalle
ALTER TABLE planilla_detalle
    ADD COLUMN gratificacion_proyectada NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN cts_proyectado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN regimen_laboral VARCHAR(20);
```

**Parámetros nuevos (valores oficiales 2026):**
- `rmv_2026_10_01`: S/ 1,230 (vigencia desde 01-oct-2026, DS 015-2026-TR)
- `rmv_2027_q1`: S/ 1,300 (previsto Q1 2027)
- `uit_2026`: S/ 5,500 (DS 301-2025-EF)
- `uit_2025`: S/ 5,350
- `ram_2026_q1`: S/ 12,209.11 (ene-mar, SBS)
- `ram_2026_q2`: S/ 12,672.65 (abr-jun, SBS)
- `ram_2026_q3`: S/ 12,672.65 (jul-sep, SBS)
- `ram`: S/ 12,672.65 (default)

**Actualizaciones de descripciones:** Todos los parámetros existentes reciben descripción con fuente legal.

### Verificación Post-Migración

```sql
-- Verificar tipo enum creado
SELECT typname FROM pg_type WHERE typname = 'regimen_laboral';

-- Verificar columna en contrato
SELECT column_name, data_type, column_default 
FROM information_schema.columns 
WHERE table_name = 'contrato' AND column_name = 'regimen_laboral';

-- Verificar parámetros nuevos
SELECT clave, valor, descripcion 
FROM parametro_sistema 
WHERE clave IN ('rmv_2026_10_01', 'uit_2026', 'ram_2026_q3');
```

**Resultado esperado:** 3 consultas devuelven filas.

## Configuración Post-Migración

### 1. Asignar Régimen Laboral a Contratos Existentes

Por defecto todos los contratos asumen `GENERAL`. Si la empresa tiene trabajadores MYPE, actualizar:

```sql
-- Microempresa (hasta 150 UIT = S/ 825,000 anuales)
UPDATE contrato 
SET regimen_laboral = 'MYPE_MICRO' 
WHERE id_contrato IN (1, 2, 3);  -- IDs de contratos MYPE micro

-- Pequeña empresa (hasta 1,700 UIT = S/ 9,350,000 anuales)
UPDATE contrato 
SET regimen_laboral = 'MYPE_PEQUENA' 
WHERE id_contrato IN (4, 5, 6);  -- IDs de contratos MYPE pequeña
```

**Nota:** La clasificación MYPE se determina por las ventas anuales de la **empresa**, no por trabajador individual. Verificar inscripción en REMYPE (registro SUNAT).

### 2. Recalcular Planillas del Mes Actual

Si ya existe una planilla calculada para el mes actual, es recomendable recalcularla para aplicar el nuevo cálculo:

**Opción A: Desde la API**
```bash
# Obtener ID de planilla actual
GET /api/planillas

# Recalcular (si está en estado CALCULADA o BORRADOR)
POST /api/planillas/{id}/calcular
```

**Opción B: Desde la BD** (si la planilla no está cerrada):
```sql
-- Ver planillas actuales
SELECT id_planilla, anio, mes, estado 
FROM planilla 
WHERE anio = 2026 AND mes = 9;

-- Borrar detalles para recalcular
DELETE FROM planilla_detalle WHERE id_planilla = {id};

-- Luego llamar a calcular desde la API
```

**No recalcular planillas cerradas** (estado CERRADA) porque ya tienen asiento contable.

## Valores Oficiales Configurables

Los siguientes valores pueden actualizarse en `parametro_sistema` sin cambiar código:

### RMV (Remuneración Mínima Vital)
```sql
-- Actualizar RMV vigente desde una fecha
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('rmv_2027_04_01', '1300', 'RMV desde 01-abr-2027 (DS pendiente)')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor, descripcion = EXCLUDED.descripcion;
```

### UIT (Unidad Impositiva Tributaria)
```sql
-- Actualizar UIT para año fiscal
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('uit_2027', '5700', 'UIT año fiscal 2027 (DS pendiente MEF)')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor, descripcion = EXCLUDED.descripcion;
```

### RAM (Remuneración Asegurable Máxima AFP)
```sql
-- Actualizar RAM trimestral (SBS publica cada 3 meses)
INSERT INTO parametro_sistema (clave, valor, descripcion) VALUES
    ('ram_2026_q4', '13000.00', 'RAM oct-dic 2026 (SBS publicación trimestral)')
ON CONFLICT (clave) DO UPDATE
SET valor = EXCLUDED.valor, descripcion = EXCLUDED.descripcion;
```

### Comisiones AFP
```sql
-- Actualizar comisión AFP (SBS las ajusta trimestralmente)
UPDATE parametro_sistema 
SET valor = '0.0150', descripcion = 'AFP Habitat comisión por flujo 1.50% (SBS 2027)'
WHERE clave = 'tasa_afp_comision_habitat';
```

## Uso en Producción

### Crear Nuevo Contrato con Régimen

**Frontend / API:**
```json
POST /api/contratos
{
  "idEmpleado": 10,
  "modalidad": "COLABORADOR",
  "regimenLaboral": "MYPE_PEQUENA",
  "regimenPensionario": "AFP",
  "afpNombre": "HABITAT",
  "remuneracionBasica": 2500.00,
  "tieneAsignacionFamiliar": true,
  "fechaInicio": "2026-10-01"
}
```

### Calcular Planilla

```bash
# 1. Crear planilla del mes
POST /api/planillas
{
  "anio": 2026,
  "mes": 10
}

# 2. Calcular (aplica régimen laboral de cada contrato)
POST /api/planillas/{id}/calcular

# 3. Revisar detalle (incluye gratificacionProyectada, ctsProyectado)
GET /api/planillas/{id}

# 4. Exportar boletas PDF
GET /api/planillas/{id}/boletas/pdf

# 5. Cerrar (genera asiento contable)
POST /api/planillas/{id}/cerrar
```

### Beneficios por Régimen

| Trabajador | Régimen | Vacaciones | Gratificación Jul | CTS Nov | EsSalud |
|------------|---------|-----------|------------------|---------|---------|
| Juan Pérez | GENERAL | 30 días | S/ 3,000 | S/ 1,750 | S/ 270 |
| Ana Torres | MYPE_PEQUENA | 15 días | S/ 1,250 | S/ 729 | S/ 225 |
| Luis Gómez | MYPE_MICRO | 15 días | S/ 0 | S/ 0 | S/ 0 (SIS) |

**Ejemplo:** Trabajador régimen general, sueldo S/ 3,000:
- Gratificación julio: S/ 3,000 (1 remuneración)
- CTS noviembre: (S/ 3,000 + S/ 500) / 12 × 6 = S/ 1,750
- EsSalud mensual: S/ 3,000 × 0.09 = S/ 270

## Troubleshooting

### Error: "type regimen_laboral already exists"
**Causa:** El script ya se ejecutó antes.  
**Solución:** Es normal, el script es idempotente. Verificar que las columnas existan.

### Error: "column regimen_laboral already exists"
**Causa:** La migración ya se aplicó.  
**Solución:** Verificar con las queries de verificación post-migración.

### Planilla con valores incorrectos
**Causa:** Contrato tiene régimen laboral incorrecto.  
**Solución:**
1. Verificar régimen del contrato: `SELECT id_contrato, regimen_laboral FROM contrato WHERE id_empleado = {id}`
2. Corregir si es necesario: `UPDATE contrato SET regimen_laboral = 'GENERAL' WHERE id_contrato = {id}`
3. Borrar detalle de planilla: `DELETE FROM planilla_detalle WHERE id_detalle = {id}`
4. Recalcular planilla desde API

### Tests fallan
**Causa:** Dependencias Maven no instaladas.  
**Solución:**
```bash
cd backend
mvn clean install
mvn test -Dtest=PlanillaCalculoPeruDinamicoTest
```

## Contacto y Soporte

Para dudas sobre valores oficiales:
- **SUNAT:** Consulta RUC y parámetros tributarios - sunat.gob.pe
- **SBS:** Comisiones AFP y RAM - sbs.gob.pe
- **MTPE:** Régimen laboral MYPE y REMYPE - trabajo.gob.pe

Para dudas sobre implementación: revisar `docs/PR_CALCULO_DINAMICO_PLANILLA.md`
