package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.HistorialSolicitud;

public interface HistorialSolicitudPort {
    Optional<HistorialSolicitud> findById(Integer id);
    List<HistorialSolicitud> findAll();
    HistorialSolicitud save(HistorialSolicitud entity);
    void deleteById(Integer id);
    void delete(HistorialSolicitud entity);
    boolean existsById(Integer id);
    long count();

    List<HistorialSolicitud> findBySolicitudPermiso_IdSolicitudPermisoOrderByIdHistorialAsc(Integer id);
    List<HistorialSolicitud> findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByIdHistorialAsc(Integer id);
}
