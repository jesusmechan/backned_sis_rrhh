package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.AdjuntoSolicitud;

public interface AdjuntoSolicitudPort {

    AdjuntoSolicitud save(AdjuntoSolicitud entity);

    List<AdjuntoSolicitud> findBySolicitudPermisoId(Integer idSolicitud);

    Optional<AdjuntoSolicitud> findByIdAdjuntoAndSolicitudPermiso_IdSolicitudPermiso(Integer idAdjunto, Integer idSolicitud);
}