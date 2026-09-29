package pe.andina.rrhh.application.service;

import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.domain.model.enums.AfpNombre;
import pe.andina.rrhh.domain.model.enums.RegimenLaboral;
import pe.andina.rrhh.domain.model.enums.RegimenPensionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cálculo dinámico de planilla laboral peruana según régimen laboral (Ley 32353 MYPE y régimen general).
 * Todos los valores se configuran mediante parámetros con vigencia y fuente documentada.
 * <p>
 * Fuentes oficiales 2026:
 * - UIT 2026: S/ 5,500 (DS 301-2025-EF)
 * - RMV: S/ 1,130 (DS 006-2024-TR) hasta 30-sep-2026; S/ 1,230 desde 01-oct-2026 (DS 015-2026-TR)
 * - Asignación Familiar: 10% RMV (D. Leg. 713)
 * - ONP: 13% (D. Ley 19990)
 * - AFP: 10% aporte + 1.37% seguro + comisión variable (SBS)
 * - EsSalud: 9% empleador (Ley 26790)
 * - Horas extras: +25% primeras 2h, +35% resto (D. Leg. 854)
 * - 5ta categoría: escala progresiva 8%-30% sobre renta anual menos 7 UIT (TUO Ley Impuesto a la Renta)
 * - CTS: remuneración computable + 1/6 gratificación / 12 * meses (D. Leg. 650)
 * - Gratificaciones: 1 remuneración en julio y diciembre (Ley 27735); MYPE pequeña: media remuneración
 * - RAM (Remuneración Asegurable Máxima): S/ 12,672.65 jul-sep 2026 (SBS)
 */
final class PlanillaCalculoPeruDinamico {

    private static final BigDecimal TREINTA = BigDecimal.valueOf(30);
    private static final BigDecimal CATORCE = BigDecimal.valueOf(14);
    private static final BigDecimal SIETE = BigDecimal.valueOf(7);
    private static final BigDecimal SEIS = BigDecimal.valueOf(6);
    private static final BigDecimal DOCE = BigDecimal.valueOf(12);
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final ParametroPort parametros;

    PlanillaCalculoPeruDinamico(ParametroPort parametros) {
        this.parametros = parametros;
    }

    /**
     * RMV vigente a la fecha especificada.
     * DS 006-2024-TR: S/ 1,130 hasta 30-sep-2026.
     * DS 015-2026-TR: S/ 1,230 desde 01-oct-2026.
     */
    BigDecimal rmv(LocalDate fecha) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        // DS 015-2026-TR: S/ 1,230 desde 01-oct-2026
        if (!fecha.isBefore(LocalDate.of(2026, 10, 1))) {
            return parametros.decimal("rmv_2026_10_01", "1230");
        }
        // DS 006-2024-TR: S/ 1,130 vigente desde 2025
        return parametros.decimal("rmv", "1130");
    }

    /**
     * UIT (Unidad Impositiva Tributaria) vigente para el año fiscal.
     * DS 301-2025-EF: S/ 5,500 para el año 2026.
     */
    BigDecimal uit(int anio) {
        String clave = "uit_" + anio;
        String valorDefault = anio == 2026 ? "5500" : "5350";
        return parametros.decimal(clave, valorDefault);
    }

    BigDecimal horasMensuales() {
        return parametros.decimal("horas_mensuales_base", "240");
    }

    /**
     * EsSalud empleador: 9% sobre remuneración, mínimo sobre RMV.
     * Ley 26790. No aplica a MYPE microempresa (usa SIS subsidiado).
     */
    BigDecimal tasaEssalud() {
        return parametros.decimal("tasa_essalud", "0.09");
    }

    /**
     * ONP: 13% del trabajador (D. Ley 19990).
     */
    BigDecimal tasaOnp() {
        return parametros.decimal("tasa_onp", "0.13");
    }

    /**
     * Asignación familiar: 10% de RMV (D. Leg. 713).
     * Aplica solo a trabajadores con hijos menores o hijos con discapacidad.
     */
    BigDecimal asignacionFamiliarMensual(boolean aplica, LocalDate fecha) {
        if (!aplica) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal tasa = parametros.decimal("tasa_asignacion_familiar", "0.10");
        return rmv(fecha).multiply(tasa).setScale(2, RM);
    }

    /** Prorrateo laboral peruano: monto × días / 30. */
    BigDecimal prorratear(BigDecimal montoMensual, BigDecimal diasComputados) {
        if (montoMensual == null || montoMensual.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal dias = diasComputados == null || diasComputados.signum() <= 0 ? TREINTA : diasComputados;
        if (dias.compareTo(TREINTA) >= 0) {
            return montoMensual.setScale(2, RM);
        }
        return montoMensual.multiply(dias).divide(TREINTA, 2, RM);
    }

    /**
     * Horas extras D. Leg. 854: primeras 2 horas × 1.25, resto × 1.35.
     * Valor hora ordinaria = remuneración computable ordinaria / 240.
     */
    BigDecimal montoHorasExtras(BigDecimal remuneracionOrdinaria, BigDecimal horas) {
        if (horas == null || horas.signum() <= 0 || remuneracionOrdinaria == null || remuneracionOrdinaria.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal baseHoras = horasMensuales();
        if (baseHoras.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal valorHora = remuneracionOrdinaria.divide(baseHoras, 6, RM);
        BigDecimal tramo1Horas = parametros.decimal("horas_extra_tramo1", "2");
        BigDecimal tasa1 = parametros.decimal("tasa_hora_extra_tramo1", "1.25");
        BigDecimal tasa2 = parametros.decimal("tasa_hora_extra_tramo2", "1.35");

        BigDecimal h1 = horas.min(tramo1Horas);
        BigDecimal h2 = horas.subtract(h1).max(BigDecimal.ZERO);
        BigDecimal monto = h1.multiply(valorHora).multiply(tasa1)
                .add(h2.multiply(valorHora).multiply(tasa2));
        return monto.setScale(2, RM);
    }

    record Pension(BigDecimal onp, BigDecimal afpAporte, BigDecimal afpComision, BigDecimal afpSeguro) {
        BigDecimal total() {
            return onp.add(afpAporte).add(afpComision).add(afpSeguro);
        }
    }

    /**
     * Cálculo de pensión (ONP o AFP) según régimen pensionario.
     * ONP: 13% del trabajador.
     * AFP: 10% aporte obligatorio + 1.37% prima de seguro (sobre RAM) + comisión variable por AFP.
     * RAM (Remuneración Asegurable Máxima) Q3 2026: S/ 12,672.65 (SBS).
     */
    Pension calcularPension(RegimenPensionario regimen, AfpNombre afp, BigDecimal baseAporte, YearMonth periodo) {
        BigDecimal cero = BigDecimal.ZERO.setScale(2, RM);
        if (regimen == null || regimen == RegimenPensionario.NINGUNO || baseAporte == null || baseAporte.signum() <= 0) {
            return new Pension(cero, cero, cero, cero);
        }
        if (regimen == RegimenPensionario.ONP) {
            return new Pension(baseAporte.multiply(tasaOnp()).setScale(2, RM), cero, cero, cero);
        }
        
        // AFP
        BigDecimal aporte = baseAporte.multiply(parametros.decimal("tasa_afp_aporte", "0.10")).setScale(2, RM);
        
        // Prima de seguro 1.37% sobre RAM (tope trimestral SBS)
        BigDecimal ram = obtenerRam(periodo);
        BigDecimal baseSeguro = baseAporte.min(ram);
        BigDecimal seguro = baseSeguro.multiply(parametros.decimal("tasa_afp_seguro", "0.0137")).setScale(2, RM);
        
        BigDecimal comision = baseAporte.multiply(tasaComisionAfp(afp)).setScale(2, RM);
        return new Pension(cero, aporte, comision, seguro);
    }

    /**
     * RAM (Remuneración Asegurable Máxima) por trimestre según SBS.
     * Q1 2026: S/ 12,209.11; Q2 2026: S/ 12,672.65 (jul-sep).
     * Fuente: SBS (actualización trimestral).
     */
    private BigDecimal obtenerRam(YearMonth periodo) {
        if (periodo == null) {
            periodo = YearMonth.now();
        }
        int mes = periodo.getMonthValue();
        int anio = periodo.getYear();
        
        // Q3 2026 (jul-sep): S/ 12,672.65
        if (anio == 2026 && mes >= 7 && mes <= 9) {
            return parametros.decimal("ram_2026_q3", "12672.65");
        }
        // Q2 2026 (abr-jun): S/ 12,672.65
        if (anio == 2026 && mes >= 4 && mes <= 6) {
            return parametros.decimal("ram_2026_q2", "12672.65");
        }
        // Q1 2026 (ene-mar): S/ 12,209.11
        if (anio == 2026 && mes >= 1 && mes <= 3) {
            return parametros.decimal("ram_2026_q1", "12209.11");
        }
        // Default para otros períodos
        return parametros.decimal("ram", "12672.65");
    }

    /**
     * Comisión AFP por flujo según entidad (actualización trimestral SBS).
     * Vigente 2026: Habitat 1.47%, Integra 1.55%, Prima 1.60%, Profuturo 1.69%.
     */
    private BigDecimal tasaComisionAfp(AfpNombre afp) {
        String key = switch (afp == null ? AfpNombre.HABITAT : afp) {
            case HABITAT -> "tasa_afp_comision_habitat";
            case INTEGRA -> "tasa_afp_comision_integra";
            case PRIMA -> "tasa_afp_comision_prima";
            case PROFUTURO -> "tasa_afp_comision_profuturo";
        };
        String def = switch (afp == null ? AfpNombre.HABITAT : afp) {
            case HABITAT -> "0.0147";
            case INTEGRA -> "0.0155";
            case PRIMA -> "0.0160";
            case PROFUTURO -> "0.0169";
        };
        return parametros.decimal(key, def);
    }

    /**
     * Retención mensual aproximada de 5ta categoría (TUO Ley Impuesto a la Renta).
     * Proyecta 14 remuneraciones (12 + 2 gratificaciones), resta 7 UIT y aplica escala progresiva / 12.
     * Escala 2026: 8% (hasta 5 UIT), 14% (5-20 UIT), 17% (20-35 UIT), 20% (35-45 UIT), 30% (>45 UIT).
     */
    BigDecimal retencionQuintaMensual(BigDecimal brutoMensual, int anio) {
        if (brutoMensual == null || brutoMensual.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal uit = uit(anio);
        BigDecimal rentaAnual = brutoMensual.multiply(CATORCE);
        BigDecimal deduccion = uit.multiply(SIETE);
        BigDecimal rentaNeta = rentaAnual.subtract(deduccion).max(BigDecimal.ZERO);
        if (rentaNeta.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal impuestoAnual = impuestoProgresivo(rentaNeta, uit);
        return impuestoAnual.divide(DOCE, 2, RM);
    }

    /**
     * Escala progresiva 5ta categoría (TUO Ley Impuesto a la Renta).
     * Tramos 2026 en UIT: 5→8%, 15→14%, 15→17%, 10→20%, resto→30%.
     */
    private BigDecimal impuestoProgresivo(BigDecimal rentaNeta, BigDecimal uit) {
        BigDecimal[] anchos = {
                uit.multiply(BigDecimal.valueOf(5)),
                uit.multiply(BigDecimal.valueOf(15)),
                uit.multiply(BigDecimal.valueOf(15)),
                uit.multiply(BigDecimal.valueOf(10))
        };
        BigDecimal[] tasas = {
                new BigDecimal("0.08"),
                new BigDecimal("0.14"),
                new BigDecimal("0.17"),
                new BigDecimal("0.20"),
                new BigDecimal("0.30")
        };
        BigDecimal restante = rentaNeta;
        BigDecimal impuesto = BigDecimal.ZERO;
        for (int i = 0; i < tasas.length; i++) {
            if (restante.signum() <= 0) {
                break;
            }
            BigDecimal tramo = i < anchos.length ? restante.min(anchos[i]) : restante;
            impuesto = impuesto.add(tramo.multiply(tasas[i]));
            restante = restante.subtract(tramo);
        }
        return impuesto.setScale(2, RM);
    }

    /**
     * EsSalud empleador: 9% sobre remuneración, mínimo sobre RMV (Ley 26790).
     * No aplica a MYPE microempresa (usa SIS subsidiado) ni a practicantes.
     */
    BigDecimal essaludEmpleador(BigDecimal base, RegimenLaboral regimen, LocalDate fecha, boolean colaborador) {
        if (!colaborador || regimen == RegimenLaboral.MYPE_MICRO || base == null || base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal baseMin = base.max(rmv(fecha));
        return baseMin.multiply(tasaEssalud()).setScale(2, RM);
    }

    /**
     * Cálculo de gratificación según régimen laboral (Ley 27735).
     * General: 1 remuneración completa (julio y diciembre).
     * MYPE pequeña: 0.5 remuneración (media gratificación).
     * MYPE micro: no obligatoria.
     */
    BigDecimal gratificacion(BigDecimal remuneracionComputable, RegimenLaboral regimen, BigDecimal mesesTrabajados) {
        if (remuneracionComputable == null || remuneracionComputable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal meses = mesesTrabajados == null || mesesTrabajados.signum() <= 0 ? SEIS : mesesTrabajados;
        BigDecimal factorSemestre = meses.divide(SEIS, 6, RM).min(BigDecimal.ONE);
        
        return switch (regimen) {
            case GENERAL -> remuneracionComputable.multiply(factorSemestre).setScale(2, RM);
            case MYPE_PEQUENA -> remuneracionComputable.multiply(new BigDecimal("0.5")).multiply(factorSemestre).setScale(2, RM);
            case MYPE_MICRO -> BigDecimal.ZERO.setScale(2, RM);
        };
    }

    /**
     * Cálculo de CTS semestral (D. Leg. 650).
     * Fórmula: (remuneración computable + 1/6 gratificación) / 12 * meses trabajados.
     * General: CTS completo.
     * MYPE pequeña: 0.5 CTS (medio CTS).
     * MYPE micro: no aplica.
     */
    BigDecimal ctsSemestral(BigDecimal remuneracionComputable, BigDecimal gratificacionSemestre, 
                           RegimenLaboral regimen, BigDecimal mesesTrabajados) {
        if (remuneracionComputable == null || remuneracionComputable.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        if (regimen == RegimenLaboral.MYPE_MICRO) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        
        BigDecimal sextoGratificacion = gratificacionSemestre != null && gratificacionSemestre.signum() > 0
                ? gratificacionSemestre.divide(SEIS, 6, RM)
                : BigDecimal.ZERO;
        BigDecimal baseComputable = remuneracionComputable.add(sextoGratificacion);
        BigDecimal meses = mesesTrabajados == null || mesesTrabajados.signum() <= 0 ? SEIS : mesesTrabajados;
        BigDecimal cts = baseComputable.divide(DOCE, 6, RM).multiply(meses).setScale(2, RM);
        
        if (regimen == RegimenLaboral.MYPE_PEQUENA) {
            return cts.multiply(new BigDecimal("0.5")).setScale(2, RM);
        }
        return cts;
    }

    /**
     * Días de vacaciones anuales según régimen laboral (Ley 32353).
     * General: 30 días.
     * MYPE micro y pequeña: 15 días.
     */
    int diasVacacionesAnuales(RegimenLaboral regimen) {
        return switch (regimen) {
            case GENERAL -> 30;
            case MYPE_MICRO, MYPE_PEQUENA -> 15;
        };
    }
}
