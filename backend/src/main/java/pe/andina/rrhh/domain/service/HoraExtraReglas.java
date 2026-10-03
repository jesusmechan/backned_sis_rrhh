package pe.andina.rrhh.domain.service;

import org.springframework.stereotype.Component;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.port.out.SolicitudHoraExtraPort;
import pe.andina.rrhh.application.port.out.ParametroPort;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;

/**
 * Reglas de negocio que se validan antes de registrar horas extras.
 */
@Component
public class HoraExtraReglas {

    private final SolicitudHoraExtraPort horaExtraRepository;
    private final ParametroPort parametros;

    public HoraExtraReglas(SolicitudHoraExtraPort horaExtraRepository, ParametroPort parametros) {
        this.horaExtraRepository = horaExtraRepository;
        this.parametros = parametros;
    }

    /**
     * Valida la cantidad pedida contra {@code hora_extra_min_solicitud} y {@code hora_extra_max_solicitud},
     * y que la suma de horas {@code PENDIENTE} y {@code APROBADO} del empleado no supere
     * {@code max_horas_extras_diarias} ni {@code max_horas_extras_semanales} (semana de lunes a domingo).
     *
     * @param empleado solicitante
     * @param request  fecha, cantidad de horas y motivo
     * @throws DomainException 400 si se incumple alguna regla
     */
    public void validarAlCrear(Empleado empleado, HoraExtraRequest request) {
        validarCantidad(request.cantidadHoras());
        validarMotivo(request.motivo());
        validarTope(empleado.getIdEmpleado(), request.fecha(), request.cantidadHoras());
    }

    private void validarCantidad(BigDecimal cantidad) {
        BigDecimal min = parametros.decimal("hora_extra_min_solicitud");
        BigDecimal max = parametros.decimal("hora_extra_max_solicitud");
        if (cantidad.compareTo(min) < 0) {
            throw DomainException.badRequest("La cantidad mínima es " + min.stripTrailingZeros().toPlainString() + " horas");
        }
        if (cantidad.compareTo(max) > 0) {
            throw DomainException.badRequest("La cantidad máxima es " + max.stripTrailingZeros().toPlainString() + " horas");
        }
    }

    private void validarMotivo(String motivo) {
        int min = parametros.entero("motivo_min_caracteres");
        if (motivo == null || motivo.trim().length() < min) {
            throw DomainException.badRequest("El motivo debe tener al menos " + min + " caracteres");
        }
    }

    private void validarTope(Integer idEmpleado, LocalDate fecha, BigDecimal cantidad) {
        BigDecimal maxDia = parametros.decimal("max_horas_extras_diarias");
        BigDecimal maxSemana = parametros.decimal("max_horas_extras_semanales");
        LocalDate inicioSemana = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate finSemana = inicioSemana.plusDays(6);
        BigDecimal dia = BigDecimal.ZERO;
        BigDecimal semana = BigDecimal.ZERO;
        var vigentes = EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADO);
        for (SolicitudHoraExtra existente : horaExtraRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(idEmpleado)) {
            if (!vigentes.contains(existente.getEstado()) || existente.getCantidadHoras() == null) {
                continue;
            }
            if (fecha.equals(existente.getFecha())) {
                dia = dia.add(existente.getCantidadHoras());
            }
            if (!existente.getFecha().isBefore(inicioSemana) && !existente.getFecha().isAfter(finSemana)) {
                semana = semana.add(existente.getCantidadHoras());
            }
        }
        if (dia.add(cantidad).compareTo(maxDia) > 0) {
            throw DomainException.badRequest("La cantidad de horas extras del día supera el máximo permitido (" + maxDia + ").");
        }
        if (semana.add(cantidad).compareTo(maxSemana) > 0) {
            throw DomainException.badRequest("La cantidad de horas extras de la semana supera el máximo permitido (" + maxSemana + ").");
        }
    }
}
