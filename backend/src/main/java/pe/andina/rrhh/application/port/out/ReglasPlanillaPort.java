package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.domain.model.Afp;
import pe.andina.rrhh.domain.model.RegimenLaboral;
import pe.andina.rrhh.domain.model.TramoRentaQuinta;

import java.util.List;

/** Tablas configurables que intervienen en el cálculo de planilla. */
public interface ReglasPlanillaPort {
    Afp afp(String codigo);

    RegimenLaboral regimen(String codigo);

    /** Tramos activos ordenados; el último puede no tener tope ({@code hastaUit} nulo). */
    List<TramoRentaQuinta> tramosQuinta();
}
