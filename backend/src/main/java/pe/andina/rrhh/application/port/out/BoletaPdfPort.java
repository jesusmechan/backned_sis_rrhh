package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.domain.model.Planilla;
import pe.andina.rrhh.domain.model.PlanillaDetalle;

import java.util.List;

public interface BoletaPdfPort {
    byte[] exportarPlanilla(Planilla planilla, List<PlanillaDetalle> boletas);
    byte[] exportarBoleta(Planilla planilla, PlanillaDetalle detalle);
}
