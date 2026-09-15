package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Planilla;

import java.util.List;
import java.util.Optional;

public interface PlanillaRepository extends JpaRepository<Planilla, Integer> {
    List<Planilla> findAllByOrderByAnioDescMesDesc();
    Optional<Planilla> findByAnioAndMes(Integer anio, Integer mes);
}
