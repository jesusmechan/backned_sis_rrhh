package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.RegimenLaboral;

public interface RegimenLaboralPort {
    Optional<RegimenLaboral> findById(String codigo);
    Optional<RegimenLaboral> findFirstByPorDefectoTrueAndActivoTrue();
    List<RegimenLaboral> findAllByOrderByOrdenAscNombreAsc();
    RegimenLaboral save(RegimenLaboral entity);
    boolean existsById(String codigo);
}
