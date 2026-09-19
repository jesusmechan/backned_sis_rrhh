package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.ParametroSistema;

public interface ParametroSistemaPort {
    Optional<ParametroSistema> findById(String id);
    List<ParametroSistema> findAll();
    ParametroSistema save(ParametroSistema entity);
    void deleteById(String id);
    void delete(ParametroSistema entity);
    boolean existsById(String id);
    long count();
}
