package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.SolicitudAdjuntoPort;
import pe.andina.rrhh.domain.model.SolicitudAdjunto;

import java.util.List;

public interface SolicitudAdjuntoRepository extends JpaRepository<SolicitudAdjunto, Integer>, SolicitudAdjuntoPort {
    List<SolicitudAdjunto> findBySolicitudPermiso_IdSolicitudPermisoOrderByIdAdjuntoAsc(Integer id);
    long countBySolicitudPermiso_IdSolicitudPermiso(Integer id);
}
