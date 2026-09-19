package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.CargoPort;
import pe.andina.rrhh.domain.model.Cargo;

import java.util.Optional;

public interface CargoRepository extends JpaRepository<Cargo, Integer>, CargoPort {
    Optional<Cargo> findByNombreIgnoreCase(String nombre);
}
