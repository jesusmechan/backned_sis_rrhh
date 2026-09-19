package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionDetallePort;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacionDetalle;
import java.util.List;
public interface ConfiguracionAprobacionDetalleRepository extends JpaRepository<ConfiguracionAprobacionDetalle, Integer>, ConfiguracionAprobacionDetallePort {
    List<ConfiguracionAprobacionDetalle> findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(Integer id);
    void deleteByConfiguracion_IdConfiguracion(Integer id);
}
