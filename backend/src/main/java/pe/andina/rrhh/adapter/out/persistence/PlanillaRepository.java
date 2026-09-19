package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.PlanillaPort;
import pe.andina.rrhh.domain.model.Planilla;

import java.util.List;
import java.util.Optional;

public interface PlanillaRepository extends JpaRepository<Planilla, Integer>, PlanillaPort {
    List<Planilla> findAllByOrderByAnioDescMesDesc();
    Optional<Planilla> findByAnioAndMes(Integer anio, Integer mes);
}
