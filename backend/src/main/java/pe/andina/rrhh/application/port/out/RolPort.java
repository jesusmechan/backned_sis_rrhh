package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Rol;

public interface RolPort {
    Optional<Rol> findById(Integer id);
    List<Rol> findAll();
    Rol save(Rol entity);
    void deleteById(Integer id);
    void delete(Rol entity);
    boolean existsById(Integer id);
    long count();

    Optional<Rol> findByCodigo(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdRolNot(String codigo, Integer idRol);
    List<Rol> findAllByOrderByNombreAsc();
}
