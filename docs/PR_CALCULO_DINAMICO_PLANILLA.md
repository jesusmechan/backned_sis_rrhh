# PR: Cálculo Dinámico de Planilla según Regímenes Laborales de Perú

## Resumen

Este PR implementa el cálculo dinámico de planilla adaptado a los regímenes laborales vigentes en Perú (Ley 32353 MYPE y régimen general D. Leg. 728), reemplazando el cálculo fijo anterior con parámetros configurables que reflejan las tasas y valores oficiales actualizados para 2026.

## Regímenes Laborales Cubiertos

### 1. Régimen General (D. Leg. 728)
**Aplicable a:** Empresas grandes y trabajadores no acogidos a MYPE.

**Beneficios:**
- **Vacaciones:** 30 días anuales
- **Gratificaciones:** 2 remuneraciones completas (julio y diciembre) - Ley 27735
- **CTS:** Remuneración completa semestral - D. Leg. 650
- **Salud:** EsSalud 9% empleador (Ley 26790)
- **Pensiones:** AFP u ONP estándar

### 2. MYPE Pequeña Empresa (Ley 32353)
**Aplicable a:** Empresas con ventas anuales hasta 1,700 UIT (S/ 9,350,000 en 2026).

**Beneficios:**
- **Vacaciones:** 15 días anuales
- **Gratificaciones:** Media remuneración (50%) en julio y diciembre
- **CTS:** Medio CTS semestral (50%)
- **Salud:** EsSalud 9% empleador
- **Pensiones:** AFP u ONP estándar

### 3. MYPE Microempresa (Ley 32353)
**Aplicable a:** Empresas con ventas anuales hasta 150 UIT (S/ 825,000 en 2026).

**Beneficios:**
- **Vacaciones:** 15 días anuales
- **Gratificaciones:** No obligatorias
- **CTS:** No aplica
- **Salud:** SIS subsidiado (no EsSalud)
- **Pensiones:** Sistema de Pensiones Sociales para menores de 40 años

## Fórmulas y Tasas Oficiales 2026

### Valores de Referencia

| Concepto | Valor 2026 | Vigencia | Fuente Legal |
|----------|-----------|----------|--------------|
| **UIT** | S/ 5,500 | Año 2026 | DS 301-2025-EF |
| **RMV** | S/ 1,130 | Hasta 30-sep-2026 | DS 006-2024-TR |
| **RMV** | S/ 1,230 | Desde 01-oct-2026 | DS 015-2026-TR |
| **RMV** | S/ 1,300 | Previsto Q1 2027 | DS 015-2026-TR |
| **Asignación Familiar** | 10% RMV | Permanente | D. Leg. 713 |

### Aportes y Descuentos del Trabajador

#### ONP (Sistema Nacional de Pensiones)
- **Tasa:** 13% sobre remuneración bruta
- **Base legal:** D. Ley 19990
- **Fórmula:** `ONP = Remuneración_Bruta × 0.13`

#### AFP (Sistema Privado de Pensiones)

| Concepto | Tasa | Base | Fuente |
|----------|------|------|--------|
| **Aporte Obligatorio** | 10% | Remuneración bruta | SBS |
| **Prima de Seguro** | 1.37% | RAM (tope trimestral) | SBS |
| **Comisión AFP Habitat** | 1.47% | Remuneración bruta | SBS 2026 |
| **Comisión AFP Integra** | 1.55% | Remuneración bruta | SBS 2026 |
| **Comisión AFP Prima** | 1.60% | Remuneración bruta | SBS 2026 |
| **Comisión AFP Profuturo** | 1.69% | Remuneración bruta | SBS 2026 |

**RAM (Remuneración Asegurable Máxima) 2026:**
- Q1 (ene-mar): S/ 12,209.11
- Q2 (abr-jun): S/ 12,672.65
- Q3 (jul-sep): S/ 12,672.65

**Fórmulas:**
```
AFP_Aporte = Remuneración_Bruta × 0.10
AFP_Seguro = min(Remuneración_Bruta, RAM) × 0.0137
AFP_Comisión = Remuneración_Bruta × Tasa_Comisión_AFP
AFP_Total = AFP_Aporte + AFP_Seguro + AFP_Comisión
```

#### Impuesto a la Renta 5ta Categoría

**Escala Progresiva Anual (TUO Ley Impuesto a la Renta):**

| Tramo de Renta Anual | Tasa |
|---------------------|------|
| Hasta 5 UIT (S/ 27,500) | 8% |
| De 5 a 20 UIT (S/ 27,500 - S/ 110,000) | 14% |
| De 20 a 35 UIT (S/ 110,000 - S/ 192,500) | 17% |
| De 35 a 45 UIT (S/ 192,500 - S/ 247,500) | 20% |
| Más de 45 UIT (> S/ 247,500) | 30% |

**Deducción:** 7 UIT anuales (S/ 38,500 en 2026)

**Fórmula Mensual:**
```
Renta_Anual_Proyectada = Remuneración_Bruta_Mensual × 14
Renta_Neta = max(0, Renta_Anual_Proyectada - 7_UIT)
Impuesto_Anual = Aplicar_Escala_Progresiva(Renta_Neta)
Retención_Mensual = Impuesto_Anual / 12
```

### Aportes del Empleador

#### EsSalud
- **Tasa:** 9% sobre remuneración, mínimo sobre RMV
- **Base legal:** Ley 26790
- **Aplica a:** Régimen general y MYPE pequeña
- **No aplica a:** MYPE microempresa (usa SIS subsidiado)

**Fórmula:**
```
Base_EsSalud = max(Remuneración_Bruta, RMV)
EsSalud = Base_EsSalud × 0.09
```

### Horas Extras
**Base legal:** D. Leg. 854

**Sobretasas:**
- Primeras 2 horas: +25% (factor 1.25)
- Horas siguientes: +35% (factor 1.35)

**Fórmula:**
```
Valor_Hora = Remuneración_Ordinaria / 240
HE_Tramo1 = min(Horas, 2) × Valor_Hora × 1.25
HE_Tramo2 = max(0, Horas - 2) × Valor_Hora × 1.35
Monto_HE = HE_Tramo1 + HE_Tramo2
```

### Gratificaciones
**Base legal:** Ley 27735

**Fórmula por régimen:**
```
# Régimen General
Gratificación_Semestral = Remuneración_Computable × (Meses_Trabajados / 6)

# MYPE Pequeña
Gratificación_Semestral = Remuneración_Computable × 0.5 × (Meses_Trabajados / 6)

# MYPE Micro
Gratificación = 0 (no obligatoria)
```

**Remuneración Computable:** Básica + Asignación Familiar + promedios de conceptos variables.

### CTS (Compensación por Tiempo de Servicios)
**Base legal:** D. Leg. 650

**Fórmula:**
```
Remuneración_Computable = Remuneración_Básica + Asignación_Familiar + (Gratificación_Semestre / 6)
CTS_Base = (Remuneración_Computable / 12) × Meses_Trabajados

# Régimen General
CTS_Semestral = CTS_Base

# MYPE Pequeña
CTS_Semestral = CTS_Base × 0.5

# MYPE Micro
CTS = 0 (no aplica)
```

**Depósitos:** 15 de mayo (semestre nov-abr) y 15 de noviembre (semestre may-oct).

### Prorrateo
**Método laboral peruano:**
```
Monto_Prorrateado = Monto_Mensual × (Días_Trabajados / 30)
```

## Cambios en el Código

### Archivos Nuevos

1. **`RegimenLaboral.java`** (enum)
   - Define los 3 regímenes laborales: GENERAL, MYPE_MICRO, MYPE_PEQUENA

2. **`PlanillaCalculoPeruDinamico.java`**
   - Clase de cálculo dinámico que reemplaza `PlanillaCalculoPeru`
   - Todos los valores configurables mediante parámetros
   - Soporte para vigencia temporal (RMV con fecha)
   - Métodos documentados con fuentes legales

3. **`PlanillaCalculoPeruDinamicoTest.java`**
   - 20 tests unitarios con casos numéricos concretos
   - Cada test verifica valores oficiales con su fuente
   - Casos de ejemplo:
     * ONP 13% sobre S/ 2,500 = S/ 325
     * AFP Habitat sobre S/ 3,000: aporte S/ 300 + comisión S/ 44.10 + seguro S/ 41.10 = S/ 385.20
     * EsSalud 9% sobre S/ 2,500 = S/ 225
     * 5ta categoría sueldo S/ 3,500 mensual = S/ 70 retención mensual
     * CTS régimen general S/ 3,000: (3,000 + 500) / 12 × 6 = S/ 1,750

### Archivos Modificados

1. **`Contrato.java`**
   - Agrega campo `regimenLaboral` (enum)
   - Default: GENERAL

2. **`PlanillaDetalle.java`**
   - Agrega campos:
     * `regimenLaboral` (snapshot del contrato)
     * `gratificacionProyectada`
     * `ctsProyectado`

3. **`PlanillaService.java`**
   - Usa `PlanillaCalculoPeruDinamico` en lugar de `PlanillaCalculoPeru`
   - Pasa `RegimenLaboral` a los métodos de cálculo
   - Proyecta gratificaciones y CTS en cada boleta
   - Usa RMV vigente según fecha de cálculo
   - Aplica tope RAM trimestral para prima AFP

4. **`AppDtos.java`**
   - `PlanillaDetalleResponse` incluye:
     * `regimenLaboral`
     * `gratificacionProyectada`
     * `ctsProyectado`

### Migración SQL

**`05_upgrade_regimen_laboral_dinamico.sql`** (ejecutar a mano en PostgreSQL):

**Cambios de esquema:**
1. Crea tipo `regimen_laboral` (enum)
2. Agrega columna `contrato.regimen_laboral` (default GENERAL)
3. Agrega columnas a `planilla_detalle`:
   - `gratificacion_proyectada`
   - `cts_proyectado`
   - `regimen_laboral`

**Parámetros nuevos (tabla `parametro_sistema`):**
- `rmv_2026_10_01`: S/ 1,230 (vigencia desde 01-oct-2026)
- `rmv_2027_q1`: S/ 1,300 (previsto Q1 2027)
- `uit_2026`: S/ 5,500
- `uit_2025`: S/ 5,350
- `ram_2026_q1`: S/ 12,209.11 (ene-mar)
- `ram_2026_q2`: S/ 12,672.65 (abr-jun)
- `ram_2026_q3`: S/ 12,672.65 (jul-sep)
- `ram`: S/ 12,672.65 (default)

**Actualizaciones de descripciones:** Todos los parámetros existentes actualizados con fuente legal explícita.

## Uso

### Asignar Régimen Laboral a un Contrato

Al crear o actualizar un contrato, especificar el régimen según el tamaño de la empresa:

```java
contrato.setRegimenLaboral(RegimenLaboral.GENERAL);        // Empresa grande
contrato.setRegimenLaboral(RegimenLaboral.MYPE_PEQUENA);   // Hasta 1,700 UIT
contrato.setRegimenLaboral(RegimenLaboral.MYPE_MICRO);     // Hasta 150 UIT
```

### Calcular Planilla

El cálculo es automático al ejecutar `POST /api/planillas/{id}/calcular`. El sistema:

1. Lee el régimen laboral del contrato vigente
2. Aplica las tasas y beneficios correspondientes
3. Usa RMV vigente a la fecha de cálculo
4. Proyecta gratificaciones y CTS semestrales
5. Aplica tope RAM trimestral para prima AFP

### Actualizar Parámetros

Valores como RMV, UIT, comisiones AFP se configuran en `parametro_sistema`:

```sql
UPDATE parametro_sistema 
SET valor = '1300', descripcion = 'RMV Q1 2027 (DS 015-2026-TR)'
WHERE clave = 'rmv_2027_q1';
```

El sistema consultará automáticamente el valor vigente según la fecha.

## Verificación de Cálculos

Los tests incluidos verifican:

1. **Valores de referencia:** UIT, RMV con vigencia temporal
2. **ONP:** 13% exacto
3. **AFP:** Aporte 10%, seguro 1.37% con tope RAM, comisión variable
4. **EsSalud:** 9% sobre mínimo RMV
5. **5ta categoría:** Escala progresiva correcta
6. **Horas extras:** Tramos 25% y 35%
7. **Gratificaciones:** 100% general, 50% MYPE pequeña, 0% MYPE micro
8. **CTS:** Fórmula con 1/6 gratificación, 100% general, 50% MYPE pequeña, 0% MYPE micro

Ejecutar tests:
```bash
cd backend
mvn test -Dtest=PlanillaCalculoPeruDinamicoTest
```

## Limitaciones y Fuera de Alcance

Este PR **NO cubre:**

1. **Régimen Agrario:** D. Leg. 1192 (requiere tasas y beneficios específicos)
2. **Régimen Construcción Civil:** Convenio colectivo sectorial
3. **Régimen Exportación No Tradicional:** Ley 22342
4. **Trabajadores del Hogar:** Ley 27986
5. **Gratificación trunca al cese:** Liquidación final
6. **CTS trunco al cese:** Liquidación final
7. **Utilidades:** Participación en utilidades (D. Leg. 892)
8. **Bonificación extraordinaria 9%:** Sobre gratificaciones (Ley 29351, no remunerativa)

Estos regímenes especiales pueden añadirse posteriormente siguiendo el mismo patrón.

## SQL a Ejecutar Manualmente

**Archivo:** `database/05_upgrade_regimen_laboral_dinamico.sql`

**Instrucciones:**
1. Abrir pgAdmin
2. Conectar a la base de datos `rrhh_andina`
3. Abrir Query Tool
4. Cargar el archivo SQL
5. Ejecutar (F5)
6. Verificar sin errores

**Impacto:**
- Agrega columnas con valores default (no afecta datos existentes)
- Inserta/actualiza parámetros (idempotente, puede reejecutarse)
- No modifica datos de planillas ya calculadas

## Fuentes Oficiales Consultadas

1. **Decreto Supremo N° 301-2025-EF:** UIT 2026 S/ 5,500
2. **Decreto Supremo N° 006-2024-TR:** RMV S/ 1,130
3. **Decreto Supremo N° 015-2026-TR:** Incremento RMV octubre 2026
4. **Ley N° 32353:** Régimen laboral MYPE (mayo 2025)
5. **Decreto Legislativo N° 728:** Régimen general laboral
6. **Decreto Legislativo N° 650:** CTS
7. **Ley N° 27735:** Gratificaciones
8. **Decreto Legislativo N° 854:** Horas extras
9. **Decreto Ley N° 19990:** ONP
10. **Ley N° 26790:** EsSalud
11. **Decreto Legislativo N° 713:** Asignación familiar
12. **TUO Ley Impuesto a la Renta:** 5ta categoría
13. **SBS:** Comisiones AFP y RAM trimestrales

## Notas Finales

- Todos los valores están documentados con fuente legal
- Las tasas pueden actualizarse mediante parámetros sin cambiar código
- La vigencia temporal permite gestionar cambios de RMV, UIT, RAM
- Los regímenes están correctamente diferenciados según Ley 32353
- El código es retrocompatible: contratos sin régimen asumen GENERAL
