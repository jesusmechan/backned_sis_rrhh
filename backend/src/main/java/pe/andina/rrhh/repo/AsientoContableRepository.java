package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.andina.rrhh.domain.AsientoContable;

import java.util.List;
import java.util.Optional;

public interface AsientoContableRepository extends JpaRepository<AsientoContable, Integer> {
    @Query("select distinct a from AsientoContable a left join fetch a.lineas order by a.idAsiento desc")
    List<AsientoContable> findAllConLineas();

    Optional<AsientoContable> findByPlanilla_IdPlanilla(Integer idPlanilla);

    @Query("select a from AsientoContable a left join fetch a.lineas where a.idAsiento = :id")
    Optional<AsientoContable> findConLineas(Integer id);
}
