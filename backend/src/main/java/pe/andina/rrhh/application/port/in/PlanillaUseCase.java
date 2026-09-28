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

/**
 * Planilla mensual de remuneraciones y su asiento contable.
 *
 * <p>Ciclo de vida: {@code BORRADOR} → {@code CALCULADA} → {@code CERRADA}. Una planilla
 * cerrada ya no se recalcula y tiene exactamente un asiento de partida doble.</p>
 */
public interface PlanillaUseCase {

    /** @return todas las planillas, sin detalle de boletas */
    List<PlanillaResponse> listar();

    /**
     * @param id identificador de la planilla
     * @return la planilla con sus boletas
     */
    PlanillaResponse obtener(Integer id);

    /**
     * Crea la planilla de un periodo en estado {@code BORRADOR}.
     *
     * @param request año, mes y observaciones
     * @return la planilla creada
     * @throws pe.andina.rrhh.domain.exception.DomainException si ya existe una para ese año y mes
     */
    PlanillaResponse crear(PlanillaRequest request);

    /**
     * Recalcula todas las boletas: empleados {@code ACTIVO} con contrato {@code VIGENTE} en el
     * periodo. Incluye básico, asignación familiar, horas extras aprobadas, ausencias, ONP o AFP
     * y EsSalud; las tasas se leen de {@code parametro_sistema}.
     *
     * @param id identificador de la planilla
     * @return la planilla en estado {@code CALCULADA} con sus totales
     * @throws pe.andina.rrhh.domain.exception.DomainException si está cerrada o anulada
     */
    PlanillaResponse calcular(Integer id);

    /**
     * Cierra la planilla y genera su asiento contable.
     *
     * @param id identificador de la planilla
     * @return la planilla en estado {@code CERRADA}
     * @throws pe.andina.rrhh.domain.exception.DomainException si no está calculada o ya tiene asiento
     */
    PlanillaResponse cerrar(Integer id);

    /**
     * @param id planilla calculada o cerrada
     * @return un PDF con todas las boletas del periodo
     */
    BoletaPdfFile pdfBoletas(Integer id);

    /**
     * @param id        planilla calculada o cerrada
     * @param idDetalle boleta del colaborador
     * @return el PDF de una sola boleta
     */
    BoletaPdfFile pdfBoleta(Integer id, Integer idDetalle);

    /** @return asientos contables con sus líneas */
    List<AsientoResponse> listarAsientos();

    /**
     * @param id identificador del asiento
     * @return el asiento con sus líneas de debe y haber
     */
    AsientoResponse obtenerAsiento(Integer id);
}
