package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.domain.model.Rol;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer>, RolPort {
    Optional<Rol> findByCodigo(String codigo);
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdRolNot(String codigo, Integer idRol);
    List<Rol> findAllByOrderByNombreAsc();
}
