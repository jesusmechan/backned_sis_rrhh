package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.SolicitudAdjunto;

public interface SolicitudAdjuntoPort {
    Optional<SolicitudAdjunto> findById(Integer id);
    SolicitudAdjunto save(SolicitudAdjunto entity);
    void delete(SolicitudAdjunto entity);

    List<SolicitudAdjunto> findBySolicitudPermiso_IdSolicitudPermisoOrderByIdAdjuntoAsc(Integer id);
    long countBySolicitudPermiso_IdSolicitudPermiso(Integer id);
}
