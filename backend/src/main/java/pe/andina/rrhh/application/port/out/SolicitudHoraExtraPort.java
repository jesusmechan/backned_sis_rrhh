package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;

public interface SolicitudHoraExtraPort {
    Optional<SolicitudHoraExtra> findById(Integer id);
    List<SolicitudHoraExtra> findAll();
    SolicitudHoraExtra save(SolicitudHoraExtra entity);
    SolicitudHoraExtra saveAndFlush(SolicitudHoraExtra entity);
    void deleteById(Integer id);
    void delete(SolicitudHoraExtra entity);
    boolean existsById(Integer id);
    long count();

    List<SolicitudHoraExtra> findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(Integer idEmpleado);
}
