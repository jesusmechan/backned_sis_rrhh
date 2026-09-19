package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.TipoPermiso;

public interface TipoPermisoPort {
    Optional<TipoPermiso> findById(Integer id);
    List<TipoPermiso> findAll();
    TipoPermiso save(TipoPermiso entity);
    void deleteById(Integer id);
    void delete(TipoPermiso entity);
    boolean existsById(Integer id);
    long count();

    Optional<TipoPermiso> findByCodigoIgnoreCase(String codigo);
    Optional<TipoPermiso> findByNombreIgnoreCase(String nombre);
}
