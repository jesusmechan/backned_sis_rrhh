package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.RolPermiso;
import pe.andina.rrhh.domain.model.RolPermisoId;

public interface RolPermisoPort {
    Optional<RolPermiso> findById(RolPermisoId id);
    List<RolPermiso> findAll();
    RolPermiso save(RolPermiso entity);
    void flush();
    void deleteById(RolPermisoId id);
    void delete(RolPermiso entity);
    boolean existsById(RolPermisoId id);
    long count();

    List<RolPermiso> findByRolId(Integer idRol);
    void deleteByRol_IdRol(Integer idRol);
}
