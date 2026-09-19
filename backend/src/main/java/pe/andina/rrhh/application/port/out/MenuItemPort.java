package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.MenuItem;

public interface MenuItemPort {
    Optional<MenuItem> findById(Integer id);
    List<MenuItem> findAll();
    MenuItem save(MenuItem entity);
    void deleteById(Integer id);
    void delete(MenuItem entity);
    boolean existsById(Integer id);
    long count();

    List<MenuItem> findActivosByPerfil(Integer idRol);
    List<MenuItem> findAllWithPerfiles();
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdMenuNot(String codigo, Integer idMenu);
}
