package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.AsientoContable;

public interface AsientoContablePort {
    Optional<AsientoContable> findById(Integer id);
    List<AsientoContable> findAll();
    AsientoContable save(AsientoContable entity);
    void deleteById(Integer id);
    void delete(AsientoContable entity);
    boolean existsById(Integer id);
    long count();

    List<AsientoContable> findAllConLineas();
    Optional<AsientoContable> findByPlanilla_IdPlanilla(Integer idPlanilla);
    Optional<AsientoContable> findConLineas(Integer id);
}
