package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.PlanillaDetalle;

public interface PlanillaDetallePort {
    Optional<PlanillaDetalle> findById(Integer id);
    List<PlanillaDetalle> findAll();
    PlanillaDetalle save(PlanillaDetalle entity);
    void flush();
    void deleteById(Integer id);
    void delete(PlanillaDetalle entity);
    boolean existsById(Integer id);
    long count();

    List<PlanillaDetalle> findByPlanilla_IdPlanillaOrderByIdDetalleAsc(Integer idPlanilla);
    void deleteByPlanilla_IdPlanilla(Integer idPlanilla);
    List<PlanillaDetalle> findCompletosByPlanilla(Integer idPlanilla);
    Optional<PlanillaDetalle> findCompleto(Integer idPlanilla, Integer idDetalle);
}
