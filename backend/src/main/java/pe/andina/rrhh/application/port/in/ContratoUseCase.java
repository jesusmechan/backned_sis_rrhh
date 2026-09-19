package pe.andina.rrhh.application.port.in;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import pe.andina.rrhh.application.dto.AppDtos.ContratoRequest;
import pe.andina.rrhh.application.dto.AppDtos.ContratoResponse;
import pe.andina.rrhh.application.dto.AppDtos.VacacionSaldoResponse;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.HorarioLaboral;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.model.enums.ModalidadContrato;
import pe.andina.rrhh.domain.model.enums.TipoContrato;

public interface ContratoUseCase {
    List<ContratoResponse> listar();
    ContratoResponse obtener(Integer id);
    ContratoResponse crear(ContratoRequest request);
    ContratoResponse actualizar(Integer id, ContratoRequest request);
    VacacionSaldoResponse saldoVacaciones(Integer idEmpleado);
    void validarVacaciones(Empleado empleado, LocalDate desde, LocalDate hasta);
}
