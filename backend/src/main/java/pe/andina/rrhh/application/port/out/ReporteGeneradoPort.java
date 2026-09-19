package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.ReporteGenerado;

public interface ReporteGeneradoPort {
    Optional<ReporteGenerado> findById(Integer id);
    List<ReporteGenerado> findAll();
    ReporteGenerado save(ReporteGenerado entity);
    void deleteById(Integer id);
    void delete(ReporteGenerado entity);
    boolean existsById(Integer id);
    long count();
}
