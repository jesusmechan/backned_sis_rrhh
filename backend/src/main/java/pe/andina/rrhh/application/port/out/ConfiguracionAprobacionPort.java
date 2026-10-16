package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.model.enums.TipoOrigenFlujo;

public interface ConfiguracionAprobacionPort {
    Optional<ConfiguracionAprobacion> findById(Integer id);
    List<ConfiguracionAprobacion> findAll();
    ConfiguracionAprobacion save(ConfiguracionAprobacion entity);
    void deleteById(Integer id);
    void delete(ConfiguracionAprobacion entity);
    boolean existsById(Integer id);
    long count();

    Optional<ConfiguracionAprobacion> findByCodigo(String codigo);

    Optional<ConfiguracionAprobacion> findActivaPorTipoPermiso(TipoOrigenFlujo tipo, Integer idTipoPermiso);

    Optional<ConfiguracionAprobacion> findActivaGenerica(TipoOrigenFlujo tipo);
}
