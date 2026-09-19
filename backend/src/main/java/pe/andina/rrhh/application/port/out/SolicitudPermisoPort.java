package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.SolicitudPermiso;

public interface SolicitudPermisoPort {
    Optional<SolicitudPermiso> findById(Integer id);
    List<SolicitudPermiso> findAll();
    SolicitudPermiso save(SolicitudPermiso entity);
    SolicitudPermiso saveAndFlush(SolicitudPermiso entity);
    void deleteById(Integer id);
    void delete(SolicitudPermiso entity);
    boolean existsById(Integer id);
    long count();

    List<SolicitudPermiso> findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(Integer idEmpleado);
}
