package pe.andina.rrhh.application.port.in;

import pe.andina.rrhh.application.dto.AppDtos.ContadorNotificaciones;
import pe.andina.rrhh.application.dto.AppDtos.NotificacionResponse;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;

import java.util.List;

public interface NotificacionUseCase {
    List<NotificacionResponse> listar();
    ContadorNotificaciones noLeidas();
    NotificacionResponse marcarLeida(Integer id);
    void marcarTodas();
    void avisarPasoEnCurso(SolicitudPasoAprobacion paso, Integer idUsuarioExcluido);
    void avisarResultado(SolicitudPasoAprobacion pasoDecidido, boolean aprobada, Integer idUsuarioExcluido);
}
