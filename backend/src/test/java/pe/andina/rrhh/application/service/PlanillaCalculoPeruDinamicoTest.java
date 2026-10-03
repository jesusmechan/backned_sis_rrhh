package pe.andina.rrhh.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.ReglasPlanillaPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Afp;
import pe.andina.rrhh.domain.model.RegimenLaboral;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;
import pe.andina.rrhh.domain.model.enums.RegimenPensionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cálculo de planilla con la configuración semilla de Maestros (valores oficiales 2026):
 * UIT S/ 5,500; RMV S/ 1,130 hasta 30-sep y S/ 1,230 desde 01-oct; ONP 13%; AFP 10% + 1.37% + comisión;
 * EsSalud 9% con mínimo RMV; asignación familiar 10% RMV.
 */
class PlanillaCalculoPeruDinamicoTest {

    private PlanillaCalculoPeruDinamico calculo;
    private final Map<String, String> parametros = new HashMap<>();
    private final Map<String, TreeMap<LocalDate, String>> vigencias = new HashMap<>();
    private final Map<String, Afp> afps = new HashMap<>();
    private final Map<String, RegimenLaboral> regimenes = new HashMap<>();

    @BeforeEach
    void setUp() {
        parametros.put("tasa_asignacion_familiar", "0.10");
        parametros.put("tasa_onp", "0.13");
        parametros.put("tasa_essalud", "0.09");
        parametros.put("tasa_afp_aporte", "0.10");
        parametros.put("tasa_afp_seguro", "0.0137");
        parametros.put("horas_mensuales_base", "240");
        parametros.put("tasa_hora_extra_tramo1", "1.25");
        parametros.put("tasa_hora_extra_tramo2", "1.35");
        parametros.put("horas_extra_tramo1", "2");
        parametros.put("dias_mes_computable", "30");
        parametros.put("meses_semestre", "6");
        parametros.put("meses_anio", "12");
        parametros.put("meses_proyeccion_quinta", "14");
        parametros.put("uit_deduccion_quinta", "7");
        parametros.put("rmv", "1130");
        parametros.put("uit", "5500");
        parametros.put("ram", "12672.65");

        vigencia("rmv", LocalDate.of(2025, 1, 1), "1130");
        vigencia("rmv", LocalDate.of(2026, 10, 1), "1230");
        vigencia("uit", LocalDate.of(2026, 1, 1), "5500");
        vigencia("ram", LocalDate.of(2026, 1, 1), "12209.11");
        vigencia("ram", LocalDate.of(2026, 4, 1), "12672.65");

        afp("HABITAT", "0.0147");
        afp("PROFUTURO", "0.0169");
        regimen("GENERAL", "1", "1", true);
        regimen("MYPE_PEQUENA", "0.5", "0.5", true);
        regimen("MYPE_MICRO", "0", "0", false);

        List<TramoRentaQuinta> tramos = List.of(
                tramo(10, "5", "0.08"), tramo(20, "20", "0.14"), tramo(30, "35", "0.17"),
                tramo(40, "45", "0.20"), tramo(50, null, "0.30"));

        ParametroPort parametroPort = new ParametroPort() {
            @Override
            public BigDecimal decimal(String clave) {
                return new BigDecimal(texto(clave));
            }

            @Override
            public BigDecimal decimal(String clave, LocalDate fecha) {
                TreeMap<LocalDate, String> porFecha = vigencias.get(clave);
                Map.Entry<LocalDate, String> vigente = porFecha == null ? null : porFecha.floorEntry(fecha);
                return vigente != null ? new BigDecimal(vigente.getValue()) : decimal(clave);
            }

            @Override
            public int entero(String clave) {
                return decimal(clave).intValueExact();
            }

            @Override
            public String texto(String clave) {
                String valor = parametros.get(clave);
                if (valor == null) {
                    throw DomainException.badRequest("Falta " + clave);
                }
                return valor;
            }

            @Override
            public ZoneId zona() {
                return ZoneId.of("America/Lima");
            }

            @Override
            public Set<DayOfWeek> diasLaborables() {
                return Set.of();
            }

            @Override
            public void invalidar() {
            }
        };

        ReglasPlanillaPort reglas = new ReglasPlanillaPort() {
            @Override
            public Afp afp(String codigo) {
                return afps.get(codigo);
            }

            @Override
            public RegimenLaboral regimen(String codigo) {
                return regimenes.get(codigo);
            }

            @Override
            public List<TramoRentaQuinta> tramosQuinta() {
                return tramos;
            }
        };

        calculo = new PlanillaCalculoPeruDinamico(parametroPort, reglas);
    }

    @Test
    @DisplayName("RMV: S/ 1,130 hasta 30-sep-2026 según la vigencia")
    void rmvAntesDeOctubre() {
        assertEquals(dos("1130"), calculo.rmv(LocalDate.of(2026, 9, 30)).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("RMV: S/ 1,230 desde 01-oct-2026 según la vigencia")
    void rmvDesdeOctubre() {
        assertEquals(dos("1230"), calculo.rmv(LocalDate.of(2026, 10, 1)).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("UIT 2026: S/ 5,500")
    void uit2026() {
        assertEquals(dos("5500"), calculo.uit(2026).setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("RAM: usa la vigencia del trimestre")
    void ramPorVigencia() {
        assertEquals(new BigDecimal("12209.11"), calculo.ram(YearMonth.of(2026, 2)));
        assertEquals(new BigDecimal("12672.65"), calculo.ram(YearMonth.of(2026, 8)));
    }

    @Test
    @DisplayName("Asignación familiar: 10% de RMV = S/ 113")
    void asignacionFamiliar() {
        assertEquals(dos("113"), calculo.asignacionFamiliarMensual(true, LocalDate.of(2026, 8, 15)));
    }

    @Test
    @DisplayName("ONP: 13% sobre la base")
    void pensionOnp() {
        var pension = calculo.calcularPension(RegimenPensionario.ONP, null, dos("2500"), YearMonth.of(2026, 8));
        assertEquals(dos("325"), pension.onp());
        assertEquals(dos("0"), pension.afpAporte());
        assertEquals(dos("325"), pension.total());
    }

    @Test
    @DisplayName("AFP Habitat: 10% aporte + 1.47% comisión + 1.37% seguro")
    void pensionAfpHabitat() {
        var pension = calculo.calcularPension(RegimenPensionario.AFP, "HABITAT", dos("3000"), YearMonth.of(2026, 8));
        assertEquals(dos("300"), pension.afpAporte());
        assertEquals(dos("44.10"), pension.afpComision());
        assertEquals(dos("41.10"), pension.afpSeguro());
        assertEquals(dos("385.20"), pension.total());
    }

    @Test
    @DisplayName("AFP Profuturo: comisión 1.69% desde la tabla de AFP")
    void pensionAfpProfuturo() {
        var pension = calculo.calcularPension(RegimenPensionario.AFP, "PROFUTURO", dos("2000"), YearMonth.of(2026, 5));
        assertEquals(dos("200"), pension.afpAporte());
        assertEquals(dos("33.80"), pension.afpComision());
        assertEquals(dos("27.40"), pension.afpSeguro());
        assertEquals(dos("261.20"), pension.total());
    }

    @Test
    @DisplayName("AFP: el seguro se calcula con tope RAM")
    void afpSeguroConTopeRam() {
        var pension = calculo.calcularPension(RegimenPensionario.AFP, "HABITAT", dos("15000"), YearMonth.of(2026, 8));
        assertEquals(dos("1500"), pension.afpAporte());
        assertEquals(dos("220.50"), pension.afpComision());
        assertEquals(dos("173.62"), pension.afpSeguro());
    }

    @Test
    @DisplayName("EsSalud: 9% con mínimo RMV si el régimen lo exige")
    void essaludEmpleador() {
        LocalDate fecha = LocalDate.of(2026, 8, 15);
        assertEquals(dos("225"), calculo.essaludEmpleador(dos("2500"), regimenes.get("GENERAL"), fecha, true));
        assertEquals(dos("101.70"), calculo.essaludEmpleador(dos("800"), regimenes.get("GENERAL"), fecha, true));
        assertEquals(dos("0"), calculo.essaludEmpleador(dos("2000"), regimenes.get("MYPE_MICRO"), fecha, true));
    }

    @Test
    @DisplayName("Horas extras: primer tramo 1.25 y resto 1.35")
    void horasExtras() {
        assertEquals(dos("25"), calculo.montoHorasExtras(dos("2400"), new BigDecimal("2.0")));
        assertEquals(dos("65.50"), calculo.montoHorasExtras(dos("2400"), new BigDecimal("5.0")));
    }

    @Test
    @DisplayName("5ta categoría: tramos configurables sobre renta anual menos 7 UIT")
    void retencionQuinta() {
        assertEquals(dos("0"), calculo.retencionQuintaMensual(dos("2000"), 2026));
        // 3,500 × 14 − 7 × 5,500 = 10,500 → 8% = 840 / 12 = 70
        assertEquals(dos("70"), calculo.retencionQuintaMensual(dos("3500"), 2026));
        // 10,000 × 14 − 38,500 = 101,500: 27,500×8% + 74,000×14% = 12,560 / 12 = 1,046.67
        assertEquals(dos("1046.67"), calculo.retencionQuintaMensual(dos("10000"), 2026));
    }

    @Test
    @DisplayName("Gratificación: factor del régimen laboral")
    void gratificacion() {
        BigDecimal seis = new BigDecimal("6");
        assertEquals(dos("3000"), calculo.gratificacion(dos("3000"), regimenes.get("GENERAL"), seis));
        assertEquals(dos("1000"), calculo.gratificacion(dos("2000"), regimenes.get("MYPE_PEQUENA"), seis));
        assertEquals(dos("0"), calculo.gratificacion(dos("2000"), regimenes.get("MYPE_MICRO"), seis));
    }

    @Test
    @DisplayName("CTS: (computable + 1/6 gratificación) / 12 × meses × factor del régimen")
    void cts() {
        BigDecimal seis = new BigDecimal("6");
        assertEquals(dos("1750"), calculo.ctsSemestral(dos("3000"), dos("3000"), regimenes.get("GENERAL"), seis));
        assertEquals(dos("541.67"), calculo.ctsSemestral(dos("2000"), dos("1000"), regimenes.get("MYPE_PEQUENA"), seis));
        assertEquals(dos("0"), calculo.ctsSemestral(dos("2000"), BigDecimal.ZERO, regimenes.get("MYPE_MICRO"), seis));
    }

    @Test
    @DisplayName("Prorrateo: monto × días / días del mes computable")
    void prorrateo() {
        assertEquals(dos("3000"), calculo.prorratear(dos("3000"), new BigDecimal("30")));
        assertEquals(dos("1500"), calculo.prorratear(dos("3000"), new BigDecimal("15")));
    }

    @Test
    @DisplayName("Un parámetro faltante es un error de configuración, no un valor por defecto")
    void parametroFaltante() {
        parametros.remove("tasa_onp");
        assertThrows(DomainException.class,
                () -> calculo.calcularPension(RegimenPensionario.ONP, null, dos("2500"), YearMonth.of(2026, 8)));
    }

    private static BigDecimal dos(String valor) {
        return new BigDecimal(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private void vigencia(String clave, LocalDate desde, String valor) {
        vigencias.computeIfAbsent(clave, k -> new TreeMap<>()).put(desde, valor);
    }

    private void afp(String codigo, String comision) {
        Afp afp = new Afp();
        afp.setCodigo(codigo);
        afp.setNombre(codigo);
        afp.setTasaComision(new BigDecimal(comision));
        afps.put(codigo, afp);
    }

    private void regimen(String codigo, String grati, String cts, boolean essalud) {
        RegimenLaboral r = new RegimenLaboral();
        r.setCodigo(codigo);
        r.setFactorGratificacion(new BigDecimal(grati));
        r.setFactorCts(new BigDecimal(cts));
        r.setAplicaEssalud(essalud);
        regimenes.put(codigo, r);
    }

    private static TramoRentaQuinta tramo(int orden, String hastaUit, String tasa) {
        TramoRentaQuinta t = new TramoRentaQuinta();
        t.setOrden(orden);
        t.setHastaUit(hastaUit == null ? null : new BigDecimal(hastaUit));
        t.setTasa(new BigDecimal(tasa));
        return t;
    }
}
