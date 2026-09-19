package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.HorarioLaboral;

public interface HorarioLaboralPort {
    Optional<HorarioLaboral> findById(Integer id);
    List<HorarioLaboral> findAll();
    HorarioLaboral save(HorarioLaboral entity);
    void deleteById(Integer id);
    void delete(HorarioLaboral entity);
    boolean existsById(Integer id);
    long count();

    Optional<HorarioLaboral> findByNombre(String nombre);
    Optional<HorarioLaboral> findByNombreIgnoreCase(String nombre);
}
