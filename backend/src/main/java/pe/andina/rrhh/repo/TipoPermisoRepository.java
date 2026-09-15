package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.TipoPermiso;

import java.util.Optional;

public interface TipoPermisoRepository extends JpaRepository<TipoPermiso, Integer> {
    Optional<TipoPermiso> findByCodigoIgnoreCase(String codigo);
    Optional<TipoPermiso> findByNombreIgnoreCase(String nombre);
}
