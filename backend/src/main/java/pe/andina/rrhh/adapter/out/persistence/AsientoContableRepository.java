package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.AsientoContablePort;
import org.springframework.data.jpa.repository.Query;
import pe.andina.rrhh.domain.model.AsientoContable;

import java.util.List;
import java.util.Optional;

public interface AsientoContableRepository extends JpaRepository<AsientoContable, Integer>, AsientoContablePort {
    @Query("select distinct a from AsientoContable a left join fetch a.lineas order by a.idAsiento desc")
    List<AsientoContable> findAllConLineas();

    Optional<AsientoContable> findByPlanilla_IdPlanilla(Integer idPlanilla);

    @Query("select a from AsientoContable a left join fetch a.lineas where a.idAsiento = :id")
    Optional<AsientoContable> findConLineas(Integer id);
}
