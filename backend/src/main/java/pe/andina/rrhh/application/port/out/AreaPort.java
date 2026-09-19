package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Area;

public interface AreaPort {
    Optional<Area> findById(Integer id);
    List<Area> findAll();
    Area save(Area entity);
    void deleteById(Integer id);
    void delete(Area entity);
    boolean existsById(Integer id);
    long count();

    Optional<Area> findByNombreIgnoreCase(String nombre);
}
