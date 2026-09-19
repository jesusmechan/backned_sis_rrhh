package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.PlanillaDetallePort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.andina.rrhh.domain.model.PlanillaDetalle;

import java.util.List;
import java.util.Optional;

public interface PlanillaDetalleRepository extends JpaRepository<PlanillaDetalle, Integer>, PlanillaDetallePort {
    List<PlanillaDetalle> findByPlanilla_IdPlanillaOrderByIdDetalleAsc(Integer idPlanilla);
    void deleteByPlanilla_IdPlanilla(Integer idPlanilla);

    @EntityGraph(attributePaths = {"empleado", "empleado.area", "empleado.cargo", "contrato", "planilla"})
    @Query("select d from PlanillaDetalle d where d.planilla.idPlanilla = :idPlanilla order by d.idDetalle")
    List<PlanillaDetalle> findCompletosByPlanilla(@Param("idPlanilla") Integer idPlanilla);

    @EntityGraph(attributePaths = {"empleado", "empleado.area", "empleado.cargo", "contrato", "planilla"})
    @Query("select d from PlanillaDetalle d where d.planilla.idPlanilla = :idPlanilla and d.idDetalle = :idDetalle")
    Optional<PlanillaDetalle> findCompleto(@Param("idPlanilla") Integer idPlanilla, @Param("idDetalle") Integer idDetalle);
}
