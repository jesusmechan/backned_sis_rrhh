package pe.andina.rrhh.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.domain.model.enums.AfpNombre;
import pe.andina.rrhh.domain.model.enums.RegimenLaboral;
import pe.andina.rrhh.domain.model.enums.RegimenPensionario;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del cálculo dinámico de planilla con valores oficiales verificados.
 * 
 * Fuentes 2026:
 * - UIT: S/ 5,500 (DS 301-2025-EF)
 * - RMV: S/ 1,130 hasta 30-sep, S/ 1,230 desde 01-oct (DS 015-2026-TR)
 * - ONP: 13% (D. Ley 19990)
 * - AFP: 10% + 1.37% seguro + comisión variable (SBS)
 * - EsSalud: 9% empleador mínimo sobre RMV (Ley 26790)
 * - Asignación Familiar: 10% RMV (D. Leg. 713)
 */
class PlanillaCalculoPeruDinamicoTest {

    private PlanillaCalculoPeruDinamico calculo;
    private Map<String, String> parametros;

    @BeforeEach
    void setUp() {
        parametros = new HashMap<>();
        // Valores oficiales 2026
        parametros.put("rmv", "1130");
        parametros.put("rmv_2026_10_01", "1230");
        parametros.put("uit_2026", "5500");
        parametros.put("tasa_asignacion_familiar", "0.10");
        parametros.put("tasa_onp", "0.13");
        parametros.put("tasa_essalud", "0.09");
        parametros.put("tasa_afp_aporte", "0.10");
        parametros.put("tasa_afp_seguro", "0.0137");
        parametros.put("tasa_afp_comision_habitat", "0.0147");
        parametros.put("tasa_afp_comision_integra", "0.0155");
        parametros.put("tasa_afp_comision_prima", "0.0160");
        parametros.put("tasa_afp_comision_profuturo", "0.0169");
        parametros.put("horas_mensuales_base", "240");
        parametros.put("tasa_hora_extra_tramo1", "1.25");
        parametros.put("tasa_hora_extra_tramo2", "1.35");
        parametros.put("horas_extra_tramo1", "2");
        parametros.put("ram_2026_q1", "12209.11");
        parametros.put("ram_2026_q2", "12672.65");
        parametros.put("ram_2026_q3", "12672.65");
        parametros.put("ram", "12672.65");

        ParametroPort parametroPort = (clave, valorDefault) -> {
            String valor = parametros.get(clave);
            return new BigDecimal(valor != null ? valor : valorDefault);
        };

        calculo = new PlanillaCalculoPeruDinamico(parametroPort);
    }

    @Test
    @DisplayName("RMV debe ser S/ 1,130 hasta 30-sep-2026 (DS 006-2024-TR)")
    void testRmvAntes30Septiembre() {
        LocalDate fecha = LocalDate.of(2026, 9, 30);
        BigDecimal rmv = calculo.rmv(fecha);
        assertEquals(new BigDecimal("1130.00").setScale(2, RoundingMode.HALF_UP), rmv.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("RMV debe ser S/ 1,230 desde 01-oct-2026 (DS 015-2026-TR)")
    void testRmvDesde01Octubre() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);
        BigDecimal rmv = calculo.rmv(fecha);
        assertEquals(new BigDecimal("1230.00").setScale(2, RoundingMode.HALF_UP), rmv.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("UIT 2026 debe ser S/ 5,500 (DS 301-2025-EF)")
    void testUit2026() {
        BigDecimal uit = calculo.uit(2026);
        assertEquals(new BigDecimal("5500.00").setScale(2, RoundingMode.HALF_UP), uit.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("Asignación familiar: 10% de RMV = S/ 113 (D. Leg. 713)")
    void testAsignacionFamiliar() {
        LocalDate fecha = LocalDate.of(2026, 8, 15);
        BigDecimal asignacion = calculo.asignacionFamiliarMensual(true, fecha);
        // 1,130 × 0.10 = 113
        assertEquals(new BigDecimal("113.00").setScale(2, RoundingMode.HALF_UP), asignacion);
    }

    @Test
    @DisplayName("ONP: 13% sobre sueldo bruto (D. Ley 19990)")
    void testCalculoPensionOnp() {
        BigDecimal baseSueldo = new BigDecimal("2500.00");
        YearMonth periodo = YearMonth.of(2026, 8);
        var pension = calculo.calcularPension(RegimenPensionario.ONP, null, baseSueldo, periodo);
        
        // ONP = 2,500 × 0.13 = 325
        assertEquals(new BigDecimal("325.00").setScale(2, RoundingMode.HALF_UP), pension.onp());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), pension.afpAporte());
        assertEquals(new BigDecimal("325.00").setScale(2, RoundingMode.HALF_UP), pension.total());
    }

    @Test
    @DisplayName("AFP Habitat: 10% aporte + 1.47% comisión + 1.37% seguro sobre RAM (SBS 2026)")
    void testCalculoPensionAfpHabitat() {
        BigDecimal baseSueldo = new BigDecimal("3000.00");
        YearMonth periodo = YearMonth.of(2026, 8); // Q3
        var pension = calculo.calcularPension(RegimenPensionario.AFP, AfpNombre.HABITAT, baseSueldo, periodo);
        
        // Aporte: 3,000 × 0.10 = 300
        assertEquals(new BigDecimal("300.00").setScale(2, RoundingMode.HALF_UP), pension.afpAporte());
        
        // Comisión: 3,000 × 0.0147 = 44.10
        assertEquals(new BigDecimal("44.10").setScale(2, RoundingMode.HALF_UP), pension.afpComision());
        
        // Seguro: 3,000 × 0.0137 = 41.10 (base menor a RAM S/ 12,672.65)
        assertEquals(new BigDecimal("41.10").setScale(2, RoundingMode.HALF_UP), pension.afpSeguro());
        
        // Total: 300 + 44.10 + 41.10 = 385.20
        assertEquals(new BigDecimal("385.20").setScale(2, RoundingMode.HALF_UP), pension.total());
    }

    @Test
    @DisplayName("AFP Profuturo: mayor comisión 1.69% (licitación SBS 2025-2027)")
    void testCalculoPensionAfpProfuturo() {
        BigDecimal baseSueldo = new BigDecimal("2000.00");
        YearMonth periodo = YearMonth.of(2026, 5);
        var pension = calculo.calcularPension(RegimenPensionario.AFP, AfpNombre.PROFUTURO, baseSueldo, periodo);
        
        // Aporte: 2,000 × 0.10 = 200
        assertEquals(new BigDecimal("200.00").setScale(2, RoundingMode.HALF_UP), pension.afpAporte());
        
        // Comisión: 2,000 × 0.0169 = 33.80
        assertEquals(new BigDecimal("33.80").setScale(2, RoundingMode.HALF_UP), pension.afpComision());
        
        // Seguro: 2,000 × 0.0137 = 27.40
        assertEquals(new BigDecimal("27.40").setScale(2, RoundingMode.HALF_UP), pension.afpSeguro());
        
        // Total: 200 + 33.80 + 27.40 = 261.20
        assertEquals(new BigDecimal("261.20").setScale(2, RoundingMode.HALF_UP), pension.total());
    }

    @Test
    @DisplayName("AFP seguro con tope RAM: S/ 12,672.65 Q3 2026 (SBS)")
    void testAfpSeguroConTopeRam() {
        // Sueldo mayor a RAM
        BigDecimal baseSueldo = new BigDecimal("15000.00");
        YearMonth periodo = YearMonth.of(2026, 8); // Q3, RAM = 12,672.65
        var pension = calculo.calcularPension(RegimenPensionario.AFP, AfpNombre.HABITAT, baseSueldo, periodo);
        
        // Aporte: 15,000 × 0.10 = 1,500 (sin tope)
        assertEquals(new BigDecimal("1500.00").setScale(2, RoundingMode.HALF_UP), pension.afpAporte());
        
        // Comisión: 15,000 × 0.0147 = 220.50 (sin tope)
        assertEquals(new BigDecimal("220.50").setScale(2, RoundingMode.HALF_UP), pension.afpComision());
        
        // Seguro: 12,672.65 × 0.0137 = 173.62 (aplica tope RAM)
        assertEquals(new BigDecimal("173.62").setScale(2, RoundingMode.HALF_UP), pension.afpSeguro());
    }

    @Test
    @DisplayName("EsSalud: 9% empleador sobre remuneración, mínimo RMV (Ley 26790)")
    void testEssaludEmpleador() {
        LocalDate fecha = LocalDate.of(2026, 8, 15);
        
        // Caso 1: remuneración mayor a RMV
        BigDecimal base1 = new BigDecimal("2500.00");
        BigDecimal essalud1 = calculo.essaludEmpleador(base1, RegimenLaboral.GENERAL, fecha, true);
        // 2,500 × 0.09 = 225
        assertEquals(new BigDecimal("225.00").setScale(2, RoundingMode.HALF_UP), essalud1);
        
        // Caso 2: remuneración menor a RMV (aplica mínimo)
        BigDecimal base2 = new BigDecimal("800.00");
        BigDecimal essalud2 = calculo.essaludEmpleador(base2, RegimenLaboral.GENERAL, fecha, true);
        // 1,130 (RMV) × 0.09 = 101.70
        assertEquals(new BigDecimal("101.70").setScale(2, RoundingMode.HALF_UP), essalud2);
    }

    @Test
    @DisplayName("MYPE microempresa: no paga EsSalud (usa SIS subsidiado, Ley 32353)")
    void testEssaludMypeMicro() {
        LocalDate fecha = LocalDate.of(2026, 8, 15);
        BigDecimal base = new BigDecimal("2000.00");
        BigDecimal essalud = calculo.essaludEmpleador(base, RegimenLaboral.MYPE_MICRO, fecha, true);
        // MYPE micro no paga EsSalud
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), essalud);
    }

    @Test
    @DisplayName("Horas extras: +25% primeras 2h, +35% resto (D. Leg. 854)")
    void testHorasExtras() {
        BigDecimal remuneracion = new BigDecimal("2400.00"); // 2,400 / 240 = 10 por hora
        
        // Caso 1: 2 horas (solo tramo 1)
        BigDecimal he2h = calculo.montoHorasExtras(remuneracion, new BigDecimal("2.0"));
        // 10 × 2 × 1.25 = 25
        assertEquals(new BigDecimal("25.00").setScale(2, RoundingMode.HALF_UP), he2h);
        
        // Caso 2: 5 horas (2h tramo 1 + 3h tramo 2)
        BigDecimal he5h = calculo.montoHorasExtras(remuneracion, new BigDecimal("5.0"));
        // (10 × 2 × 1.25) + (10 × 3 × 1.35) = 25 + 40.50 = 65.50
        assertEquals(new BigDecimal("65.50").setScale(2, RoundingMode.HALF_UP), he5h);
    }

    @Test
    @DisplayName("5ta categoría: escala progresiva 8%-30% sobre renta anual menos 7 UIT")
    void testRetencionQuintaCategoria() {
        // Caso 1: sueldo bajo, no paga impuesto
        BigDecimal sueldo1 = new BigDecimal("2000.00");
        BigDecimal quinta1 = calculo.retencionQuintaMensual(sueldo1, 2026);
        // Renta anual: 2,000 × 14 = 28,000
        // Deducción: 7 UIT = 7 × 5,500 = 38,500
        // Renta neta: 28,000 - 38,500 = 0 (negativo, no paga)
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), quinta1);
        
        // Caso 2: sueldo S/ 3,500 mensual
        BigDecimal sueldo2 = new BigDecimal("3500.00");
        BigDecimal quinta2 = calculo.retencionQuintaMensual(sueldo2, 2026);
        // Renta anual: 3,500 × 14 = 49,000
        // Deducción: 7 UIT = 38,500
        // Renta neta: 49,000 - 38,500 = 10,500
        // Tramo 1 (hasta 5 UIT = 27,500): 10,500 × 0.08 = 840
        // Impuesto anual: 840
        // Mensual: 840 / 12 = 70
        assertEquals(new BigDecimal("70.00").setScale(2, RoundingMode.HALF_UP), quinta2);
    }

    @Test
    @DisplayName("Gratificación régimen general: 1 remuneración completa (Ley 27735)")
    void testGratificacionRegimenGeneral() {
        BigDecimal remuneracion = new BigDecimal("3000.00");
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal gratificacion = calculo.gratificacion(remuneracion, RegimenLaboral.GENERAL, mesesCompletos);
        // Semestre completo: 1 remuneración
        assertEquals(new BigDecimal("3000.00").setScale(2, RoundingMode.HALF_UP), gratificacion);
    }

    @Test
    @DisplayName("Gratificación MYPE pequeña: media remuneración (Ley 32353)")
    void testGratificacionMypePequena() {
        BigDecimal remuneracion = new BigDecimal("2000.00");
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal gratificacion = calculo.gratificacion(remuneracion, RegimenLaboral.MYPE_PEQUENA, mesesCompletos);
        // MYPE pequeña: 0.5 remuneración
        assertEquals(new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP), gratificacion);
    }

    @Test
    @DisplayName("Gratificación MYPE micro: no obligatoria (Ley 32353)")
    void testGratificacionMypeMicro() {
        BigDecimal remuneracion = new BigDecimal("2000.00");
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal gratificacion = calculo.gratificacion(remuneracion, RegimenLaboral.MYPE_MICRO, mesesCompletos);
        // MYPE micro: no obligatoria
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), gratificacion);
    }

    @Test
    @DisplayName("CTS régimen general: (computable + 1/6 grat) / 12 × meses (D. Leg. 650)")
    void testCtsRegimenGeneral() {
        BigDecimal remuneracion = new BigDecimal("3000.00");
        BigDecimal gratificacion = new BigDecimal("3000.00");
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal cts = calculo.ctsSemestral(remuneracion, gratificacion, RegimenLaboral.GENERAL, mesesCompletos);
        // Base computable: 3,000 + (3,000 / 6) = 3,000 + 500 = 3,500
        // CTS semestre: 3,500 / 12 × 6 = 1,750
        assertEquals(new BigDecimal("1750.00").setScale(2, RoundingMode.HALF_UP), cts);
    }

    @Test
    @DisplayName("CTS MYPE pequeña: medio CTS (Ley 32353)")
    void testCtsMypePequena() {
        BigDecimal remuneracion = new BigDecimal("2000.00");
        BigDecimal gratificacion = new BigDecimal("1000.00"); // Media gratificación MYPE pequeña
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal cts = calculo.ctsSemestral(remuneracion, gratificacion, RegimenLaboral.MYPE_PEQUENA, mesesCompletos);
        // Base: 2,000 + (1,000 / 6) = 2,166.67
        // CTS completo: 2,166.67 / 12 × 6 = 1,083.33
        // MYPE pequeña: 1,083.33 × 0.5 = 541.67
        assertEquals(new BigDecimal("541.67").setScale(2, RoundingMode.HALF_UP), cts);
    }

    @Test
    @DisplayName("CTS MYPE micro: no aplica (Ley 32353)")
    void testCtsMypeMicro() {
        BigDecimal remuneracion = new BigDecimal("2000.00");
        BigDecimal gratificacion = BigDecimal.ZERO;
        BigDecimal mesesCompletos = new BigDecimal("6.0");
        
        BigDecimal cts = calculo.ctsSemestral(remuneracion, gratificacion, RegimenLaboral.MYPE_MICRO, mesesCompletos);
        // MYPE micro: no aplica CTS
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), cts);
    }

    @Test
    @DisplayName("Días de vacaciones: 30 general, 15 MYPE (Ley 32353)")
    void testDiasVacaciones() {
        assertEquals(30, calculo.diasVacacionesAnuales(RegimenLaboral.GENERAL));
        assertEquals(15, calculo.diasVacacionesAnuales(RegimenLaboral.MYPE_MICRO));
        assertEquals(15, calculo.diasVacacionesAnuales(RegimenLaboral.MYPE_PEQUENA));
    }

    @Test
    @DisplayName("Prorrateo: monto × días / 30 (método laboral peruano)")
    void testProrrateo() {
        BigDecimal montoMensual = new BigDecimal("3000.00");
        
        // Mes completo
        BigDecimal prorrateo30 = calculo.prorratear(montoMensual, new BigDecimal("30"));
        assertEquals(new BigDecimal("3000.00").setScale(2, RoundingMode.HALF_UP), prorrateo30);
        
        // 15 días
        BigDecimal prorrateo15 = calculo.prorratear(montoMensual, new BigDecimal("15"));
        assertEquals(new BigDecimal("1500.00").setScale(2, RoundingMode.HALF_UP), prorrateo15);
    }
}
