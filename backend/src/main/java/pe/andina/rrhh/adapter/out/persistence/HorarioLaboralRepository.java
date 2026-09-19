package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;
import pe.andina.rrhh.domain.model.HorarioLaboral;

import java.util.Optional;

public interface HorarioLaboralRepository extends JpaRepository<HorarioLaboral, Integer>, HorarioLaboralPort {
    Optional<HorarioLaboral> findByNombre(String nombre);
    Optional<HorarioLaboral> findByNombreIgnoreCase(String nombre);
}
