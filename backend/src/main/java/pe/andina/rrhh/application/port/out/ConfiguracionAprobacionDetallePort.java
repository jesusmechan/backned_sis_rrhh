package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacionDetalle;

public interface ConfiguracionAprobacionDetallePort {
    Optional<ConfiguracionAprobacionDetalle> findById(Integer id);
    List<ConfiguracionAprobacionDetalle> findAll();
    ConfiguracionAprobacionDetalle save(ConfiguracionAprobacionDetalle entity);
    void flush();
    void deleteById(Integer id);
    void delete(ConfiguracionAprobacionDetalle entity);
    boolean existsById(Integer id);
    long count();

    List<ConfiguracionAprobacionDetalle> findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(Integer id);
    void deleteByConfiguracion_IdConfiguracion(Integer id);
}
