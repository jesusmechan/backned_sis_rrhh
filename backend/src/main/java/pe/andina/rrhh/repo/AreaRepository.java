package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Area;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Integer> {
    Optional<Area> findByNombreIgnoreCase(String nombre);
}
