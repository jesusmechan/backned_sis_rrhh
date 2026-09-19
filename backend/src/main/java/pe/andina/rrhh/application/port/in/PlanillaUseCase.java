package pe.andina.rrhh.application.port.in;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import pe.andina.rrhh.application.dto.AppDtos.AsientoLineaResponse;
import pe.andina.rrhh.application.dto.AppDtos.AsientoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaDetalleResponse;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaRequest;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaResponse;
import pe.andina.rrhh.application.dto.BoletaPdfFile;
import pe.andina.rrhh.domain.model.AsientoContable;
import pe.andina.rrhh.domain.model.AsientoLinea;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.CuentaContable;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Planilla;
import pe.andina.rrhh.domain.model.PlanillaDetalle;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoAsiento;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.EstadoPlanilla;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.model.enums.ModalidadContrato;

public interface PlanillaUseCase {
    List<PlanillaResponse> listar();
    PlanillaResponse obtener(Integer id);
    PlanillaResponse crear(PlanillaRequest request);
    PlanillaResponse calcular(Integer id);
    PlanillaResponse cerrar(Integer id);
    BoletaPdfFile pdfBoletas(Integer id);
    BoletaPdfFile pdfBoleta(Integer id, Integer idDetalle);
    List<AsientoResponse> listarAsientos();
    AsientoResponse obtenerAsiento(Integer id);
}
