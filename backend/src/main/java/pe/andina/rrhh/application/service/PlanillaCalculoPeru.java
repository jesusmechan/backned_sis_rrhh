package pe.andina.rrhh.application.service;

import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.domain.model.enums.AfpNombre;
import pe.andina.rrhh.domain.model.enums.RegimenPensionario;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas de planilla laboral peruana (modelo académico alineado a 2026):
 * RMV, asignación familiar 10% RMV, HE +25%/+35%, ONP 13% o AFP (10%+comisión+seguro),
 * EsSalud 9% empleador, retención 5ta categoría simplificada (proyección ×14 − 7 UIT).
 */
final class PlanillaCalculoPeru {

    private static final BigDecimal TREINTA = BigDecimal.valueOf(30);
    private static final BigDecimal CATORCE = BigDecimal.valueOf(14);
    private static final BigDecimal SIETE = BigDecimal.valueOf(7);
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final ParametroPort parametros;

    PlanillaCalculoPeru(ParametroPort parametros) {
        this.parametros = parametros;
    }

    BigDecimal rmv() {
        return parametros.decimal("rmv", "1130");
    }

    BigDecimal uit() {
        return parametros.decimal("uit", "5500");
    }

    BigDecimal horasMensuales() {
        return parametros.decimal("horas_mensuales_base", "240");
    }

    BigDecimal tasaEssalud() {
        return parametros.decimal("tasa_essalud", "0.09");
    }

    BigDecimal tasaOnp() {
        return parametros.decimal("tasa_onp", "0.13");
    }

    BigDecimal asignacionFamiliarMensual(boolean aplica) {
        if (!aplica) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal tasa = parametros.decimal("tasa_asignacion_familiar", "0.10");
        return rmv().multiply(tasa).setScale(2, RM);
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
     * Horas extras D. Leg. 854: primeras N horas × 1.25, resto × 1.35.
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
        BigDecimal tasa1 = parametros.decimal("tasa_hora_extra_tramo1",
                parametros.decimal("tasa_hora_extra", "1.25").toPlainString());
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

    Pension calcularPension(RegimenPensionario regimen, AfpNombre afp, BigDecimal baseAporte) {
        BigDecimal cero = BigDecimal.ZERO.setScale(2, RM);
        if (regimen == null || regimen == RegimenPensionario.NINGUNO || baseAporte == null || baseAporte.signum() <= 0) {
            return new Pension(cero, cero, cero, cero);
        }
        if (regimen == RegimenPensionario.ONP) {
            return new Pension(baseAporte.multiply(tasaOnp()).setScale(2, RM), cero, cero, cero);
        }
        BigDecimal aporte = baseAporte.multiply(parametros.decimal("tasa_afp_aporte", "0.10")).setScale(2, RM);
        BigDecimal seguro = baseAporte.multiply(parametros.decimal("tasa_afp_seguro", "0.0137")).setScale(2, RM);
        BigDecimal comision = baseAporte.multiply(tasaComisionAfp(afp)).setScale(2, RM);
        return new Pension(cero, aporte, comision, seguro);
    }

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
     * Retención mensual aproximada de 5ta categoría:
     * proyecta 14 remuneraciones (12 + 2 gratificaciones), resta 7 UIT y aplica la escala progresiva / 12.
     */
    BigDecimal retencionQuintaMensual(BigDecimal brutoMensual) {
        if (brutoMensual == null || brutoMensual.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal uit = uit();
        BigDecimal rentaAnual = brutoMensual.multiply(CATORCE);
        BigDecimal deduccion = uit.multiply(SIETE);
        BigDecimal rentaNeta = rentaAnual.subtract(deduccion).max(BigDecimal.ZERO);
        if (rentaNeta.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal impuestoAnual = impuestoProgresivo(rentaNeta, uit);
        return impuestoAnual.divide(BigDecimal.valueOf(12), 2, RM);
    }

    private BigDecimal impuestoProgresivo(BigDecimal rentaNeta, BigDecimal uit) {
        // Tramos en UIT: 5→8%, 15→14%, 15→17%, 10→20%, resto→30%
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

    BigDecimal essaludEmpleador(BigDecimal base, boolean colaborador) {
        if (!colaborador || base == null || base.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RM);
        }
        BigDecimal baseMin = base.max(rmv());
        return baseMin.multiply(tasaEssalud()).setScale(2, RM);
    }
}
