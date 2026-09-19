package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;
import pe.andina.rrhh.domain.model.TipoPermiso;

import java.util.Optional;

public interface TipoPermisoRepository extends JpaRepository<TipoPermiso, Integer>, TipoPermisoPort {
    Optional<TipoPermiso> findByCodigoIgnoreCase(String codigo);
    Optional<TipoPermiso> findByNombreIgnoreCase(String nombre);
}
