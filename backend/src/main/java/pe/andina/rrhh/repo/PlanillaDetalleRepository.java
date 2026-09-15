package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.PlanillaDetalle;

import java.util.List;

public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Integer> {
    List<PlanillaDetalle> findByPlanilla_IdPlanillaOrderByIdDetalleAsc(Integer idPlanilla);
    void deleteByPlanilla_IdPlanilla(Integer idPlanilla);
}
