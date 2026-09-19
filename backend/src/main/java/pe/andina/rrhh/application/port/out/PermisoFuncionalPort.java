package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.PermisoFuncional;

public interface PermisoFuncionalPort {
    Optional<PermisoFuncional> findById(Integer id);
    List<PermisoFuncional> findAll();
    PermisoFuncional save(PermisoFuncional entity);
    void deleteById(Integer id);
    void delete(PermisoFuncional entity);
    boolean existsById(Integer id);
    long count();
}
