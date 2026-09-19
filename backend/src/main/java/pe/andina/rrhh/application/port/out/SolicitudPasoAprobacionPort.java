package pe.andina.rrhh.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.enums.EstadoPasoAprobacion;

public interface SolicitudPasoAprobacionPort {
    Optional<SolicitudPasoAprobacion> findById(Integer id);
    List<SolicitudPasoAprobacion> findAll();
    SolicitudPasoAprobacion save(SolicitudPasoAprobacion entity);
    SolicitudPasoAprobacion saveAndFlush(SolicitudPasoAprobacion entity);
    void deleteById(Integer id);
    void delete(SolicitudPasoAprobacion entity);
    boolean existsById(Integer id);
    long count();

    List<SolicitudPasoAprobacion> findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(Integer id);
    List<SolicitudPasoAprobacion> findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(Integer id);
    List<SolicitudPasoAprobacion> findByEstado(EstadoPasoAprobacion estado);
    List<SolicitudPasoAprobacion> findByUsuarioDecision_IdUsuario(Integer idUsuario);
    List<SolicitudPasoAprobacion> findByUsuarioAsignado_IdUsuarioAndEstadoIn(Integer idUsuario, Collection<EstadoPasoAprobacion> estados);
}
