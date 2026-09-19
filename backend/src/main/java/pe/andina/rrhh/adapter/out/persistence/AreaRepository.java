package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.AreaPort;
import pe.andina.rrhh.domain.model.Area;

import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Integer>, AreaPort {
    Optional<Area> findByNombreIgnoreCase(String nombre);
}
