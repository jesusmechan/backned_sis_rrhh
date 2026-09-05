package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.ConfiguracionAprobacionDetalle;
import java.util.List;
public interface ConfiguracionAprobacionDetalleRepository extends JpaRepository<ConfiguracionAprobacionDetalle, Integer> {
    List<ConfiguracionAprobacionDetalle> findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(Integer id);
    void deleteByConfiguracion_IdConfiguracion(Integer id);
}
