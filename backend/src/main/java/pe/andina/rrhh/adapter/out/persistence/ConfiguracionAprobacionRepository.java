package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionPort;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import java.util.Optional;
public interface ConfiguracionAprobacionRepository extends JpaRepository<ConfiguracionAprobacion, Integer>, ConfiguracionAprobacionPort {
    Optional<ConfiguracionAprobacion> findByCodigo(String codigo);
}
