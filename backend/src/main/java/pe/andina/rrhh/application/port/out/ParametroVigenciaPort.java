package pe.andina.rrhh.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.ParametroVigencia;

public interface ParametroVigenciaPort {
    Optional<ParametroVigencia> findById(Integer id);
    Optional<ParametroVigencia> findFirstByClaveAndVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(String clave, LocalDate fecha);
    Optional<ParametroVigencia> findByClaveAndVigenteDesde(String clave, LocalDate vigenteDesde);
    List<ParametroVigencia> findAllByOrderByClaveAscVigenteDesdeDesc();
    ParametroVigencia save(ParametroVigencia entity);
    void delete(ParametroVigencia entity);
}
