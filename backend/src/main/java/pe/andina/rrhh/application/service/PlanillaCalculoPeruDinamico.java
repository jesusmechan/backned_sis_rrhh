package pe.andina.rrhh.application.service;

import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.ReglasPlanillaPort;
import pe.andina.rrhh.domain.model.RegimenLaboral;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;
import pe.andina.rrhh.domain.model.enums.RegimenPensionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Cálculo de planilla peruana. Tasas, topes, tramos de 5ta, comisiones AFP y factores por régimen
 * laboral se leen de Maestros (parámetros, vigencias y tablas de reglas).
 */
final class PlanillaCalculoPeruDinamico {

    private static final RoundingMode RM = RoundingMode.HALF_UP;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RM);

    private final ParametroPort parametros;
    private final ReglasPlanillaPort reglas;

    PlanillaCalculoPeruDinamico(ParametroPort parametros, ReglasPlanillaPort reglas) {
        this.parametros = parametros;
        this.reglas = reglas;
    }

    BigDecimal rmv(LocalDate fecha) {
        return parametros.decimal("rmv", fecha);
    }

    BigDecimal uit(int anio) {
        return parametros.decimal("uit", LocalDate.of(anio, 1, 1));
    }

    BigDecimal ram(YearMonth periodo) {
        return parametros.decimal("ram", periodo == null ? null : periodo.atDay(1));
    }

    BigDecimal diasMes() {
        return parametros.decimal("dias_mes_computable");
    }

    BigDecimal mesesSemestre() {
        return parametros.decimal("meses_semestre");
    }

    BigDecimal horasMensuales() {
        return parametros.decimal("horas_mensuales_base");
    }

    BigDecimal asignacionFamiliarMensual(boolean aplica, LocalDate fecha) {
        if (!aplica) {
            return CERO;
        }
        return rmv(fecha).multiply(parametros.decimal("tasa_asignacion_familiar")).setScale(2, RM);
    }

    /** Prorrateo laboral: monto × días / días del mes computable. */
    BigDecimal prorratear(BigDecimal montoMensual, BigDecimal diasComputados) {
        if (montoMensual == null || montoMensual.signum() == 0) {
            return CERO;
        }
        BigDecimal diasMes = diasMes();
        BigDecimal dias = diasComputados == null || diasComputados.signum() <= 0 ? diasMes : diasComputados;
        if (dias.compareTo(diasMes) >= 0) {
            return montoMensual.setScale(2, RM);
        }
        return montoMensual.multiply(dias).divide(diasMes, 2, RM);
    }

    /** Horas extras: primer tramo y resto con su sobretasa; valor hora = remuneración ordinaria / horas base. */
    BigDecimal montoHorasExtras(BigDecimal remuneracionOrdinaria, BigDecimal horas) {
        if (horas == null || horas.signum() <= 0 || remuneracionOrdinaria == null || remuneracionOrdinaria.signum() <= 0) {
            return CERO;
        }
        BigDecimal baseHoras = horasMensuales();
        if (baseHoras.signum() == 0) {
            return CERO;
        }
        BigDecimal valorHora = remuneracionOrdinaria.divide(baseHoras, 6, RM);
        BigDecimal h1 = horas.min(parametros.decimal("horas_extra_tramo1"));
        BigDecimal h2 = horas.subtract(h1).max(BigDecimal.ZERO);
        return h1.multiply(valorHora).multiply(parametros.decimal("tasa_hora_extra_tramo1"))
                .add(h2.multiply(valorHora).multiply(parametros.decimal("tasa_hora_extra_tramo2")))
                .setScale(2, RM);
    }

    record Pension(BigDecimal onp, BigDecimal afpAporte, BigDecimal afpComision, BigDecimal afpSeguro) {
        BigDecimal total() {
            return onp.add(afpAporte).add(afpComision).add(afpSeguro);
        }
    }

    /** ONP: tasa única. AFP: aporte + prima de seguro (base topada en la RAM) + comisión de la AFP. */
    Pension calcularPension(RegimenPensionario regimen, String afp, BigDecimal baseAporte, YearMonth periodo) {
        if (regimen == null || regimen == RegimenPensionario.NINGUNO || baseAporte == null || baseAporte.signum() <= 0) {
            return new Pension(CERO, CERO, CERO, CERO);
        }
        if (regimen == RegimenPensionario.ONP) {
            return new Pension(baseAporte.multiply(parametros.decimal("tasa_onp")).setScale(2, RM), CERO, CERO, CERO);
        }
        BigDecimal aporte = baseAporte.multiply(parametros.decimal("tasa_afp_aporte")).setScale(2, RM);
        BigDecimal seguro = baseAporte.min(ram(periodo)).multiply(parametros.decimal("tasa_afp_seguro")).setScale(2, RM);
        BigDecimal comision = baseAporte.multiply(reglas.afp(afp).getTasaComision()).setScale(2, RM);
        return new Pension(CERO, aporte, comision, seguro);
    }

    /**
     * Retención mensual de 5ta: proyecta {@code meses_proyeccion_quinta} remuneraciones, deduce
     * {@code uit_deduccion_quinta} UIT, aplica los tramos y reparte entre {@code meses_anio}.
     */
    BigDecimal retencionQuintaMensual(BigDecimal brutoMensual, int anio) {
        if (brutoMensual == null || brutoMensual.signum() <= 0) {
            return CERO;
        }
        BigDecimal uit = uit(anio);
        BigDecimal rentaAnual = brutoMensual.multiply(parametros.decimal("meses_proyeccion_quinta"));
        BigDecimal deduccion = uit.multiply(parametros.decimal("uit_deduccion_quinta"));
        BigDecimal rentaNeta = rentaAnual.subtract(deduccion).max(BigDecimal.ZERO);
        if (rentaNeta.signum() == 0) {
            return CERO;
        }
        return impuestoProgresivo(rentaNeta, uit).divide(parametros.decimal("meses_anio"), 2, RM);
    }

    private BigDecimal impuestoProgresivo(BigDecimal rentaNeta, BigDecimal uit) {
        BigDecimal impuesto = BigDecimal.ZERO;
        BigDecimal limiteAnterior = BigDecimal.ZERO;
        for (TramoRentaQuinta tramo : reglas.tramosQuinta()) {
            if (rentaNeta.compareTo(limiteAnterior) <= 0) {
                break;
            }
            BigDecimal limite = tramo.getHastaUit() == null ? null : uit.multiply(tramo.getHastaUit());
            BigDecimal tope = limite == null ? rentaNeta : rentaNeta.min(limite);
            BigDecimal gravado = tope.subtract(limiteAnterior).max(BigDecimal.ZERO);
            impuesto = impuesto.add(gravado.multiply(tramo.getTasa()));
            if (limite == null) {
                break;
            }
            limiteAnterior = limite;
        }
        return impuesto.setScale(2, RM);
    }

    /** EsSalud del empleador sobre la base, con mínimo la RMV; solo si el régimen lo exige. */
    BigDecimal essaludEmpleador(BigDecimal base, RegimenLaboral regimen, LocalDate fecha, boolean colaborador) {
        if (!colaborador || !Boolean.TRUE.equals(regimen.getAplicaEssalud()) || base == null || base.signum() <= 0) {
            return CERO;
        }
        return base.max(rmv(fecha)).multiply(parametros.decimal("tasa_essalud")).setScale(2, RM);
    }

    /** Gratificación semestral proporcional a los meses, multiplicada por el factor del régimen. */
    BigDecimal gratificacion(BigDecimal remuneracionComputable, RegimenLaboral regimen, BigDecimal mesesTrabajados) {
        if (remuneracionComputable == null || remuneracionComputable.signum() <= 0) {
            return CERO;
        }
        BigDecimal semestre = mesesSemestre();
        BigDecimal meses = mesesTrabajados == null || mesesTrabajados.signum() <= 0 ? semestre : mesesTrabajados;
        BigDecimal factorSemestre = meses.divide(semestre, 6, RM).min(BigDecimal.ONE);
        return remuneracionComputable.multiply(regimen.getFactorGratificacion()).multiply(factorSemestre).setScale(2, RM);
    }

    /** CTS: (remuneración + 1/semestre de la gratificación) / meses del año × meses, por el factor del régimen. */
    BigDecimal ctsSemestral(BigDecimal remuneracionComputable, BigDecimal gratificacionSemestre,
                           RegimenLaboral regimen, BigDecimal mesesTrabajados) {
        if (remuneracionComputable == null || remuneracionComputable.signum() <= 0
                || regimen.getFactorCts().signum() == 0) {
            return CERO;
        }
        BigDecimal semestre = mesesSemestre();
        BigDecimal sextoGratificacion = gratificacionSemestre != null && gratificacionSemestre.signum() > 0
                ? gratificacionSemestre.divide(semestre, 6, RM)
                : BigDecimal.ZERO;
        BigDecimal meses = mesesTrabajados == null || mesesTrabajados.signum() <= 0 ? semestre : mesesTrabajados;
        return remuneracionComputable.add(sextoGratificacion)
                .divide(parametros.decimal("meses_anio"), 6, RM)
                .multiply(meses)
                .multiply(regimen.getFactorCts())
                .setScale(2, RM);
    }
}
